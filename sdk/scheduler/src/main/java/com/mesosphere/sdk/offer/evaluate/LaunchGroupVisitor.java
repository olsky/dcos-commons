package com.mesosphere.sdk.offer.evaluate;

import com.google.common.annotations.VisibleForTesting;
import com.mesosphere.sdk.api.ArtifactResource;
import com.mesosphere.sdk.api.EndpointUtils;
import com.mesosphere.sdk.dcos.DcosConstants;
import com.mesosphere.sdk.offer.CommonIdUtils;
import com.mesosphere.sdk.offer.Constants;
import com.mesosphere.sdk.offer.InvalidRequirementException;
import com.mesosphere.sdk.offer.taskdata.AuxLabelAccess;
import com.mesosphere.sdk.offer.taskdata.EnvConstants;
import com.mesosphere.sdk.offer.taskdata.EnvUtils;
import com.mesosphere.sdk.offer.taskdata.TaskLabelWriter;
import com.mesosphere.sdk.scheduler.SchedulerFlags;
import com.mesosphere.sdk.scheduler.plan.PodInstanceRequirement;
import com.mesosphere.sdk.specification.CommandSpec;
import com.mesosphere.sdk.specification.ConfigFileSpec;
import com.mesosphere.sdk.specification.DiscoverySpec;
import com.mesosphere.sdk.specification.HealthCheckSpec;
import com.mesosphere.sdk.specification.NetworkSpec;
import com.mesosphere.sdk.specification.PodInstance;
import com.mesosphere.sdk.specification.PodSpec;
import com.mesosphere.sdk.specification.PortSpec;
import com.mesosphere.sdk.specification.ReadinessCheckSpec;
import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.SecretSpec;
import com.mesosphere.sdk.specification.TaskSpec;
import com.mesosphere.sdk.specification.VolumeSpec;
import com.mesosphere.sdk.specification.util.RLimit;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class LaunchGroupVisitor implements SpecVisitor<List<Protos.Offer.Operation>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchGroupVisitor.class);

    private static final String CONFIG_TEMPLATE_KEY_FORMAT = "CONFIG_TEMPLATE_%s";
    private static final String CONFIG_TEMPLATE_DOWNLOAD_PATH = "config-templates/";

    private final SpecVisitor delegate;
    private final VisitorResultCollector<List<Protos.Offer.Operation>> collector;
    private final Collection<Protos.TaskInfo> taskInfos;
    private final String serviceName;
    private final Protos.FrameworkID frameworkID;
    private final UUID targetConfigurationId;
    private final SchedulerFlags schedulerFlags;

    private Protos.ExecutorInfo executorInfo;
    private Protos.Offer.Operation.LaunchGroup.Builder launchGroup;
    private PodInstanceRequirement podInstanceRequirement;
    private List<Protos.Offer.Operation> operations;
    private boolean isTaskActive;

    // TODO(mrb): tasks to launch, transients, etc
    public LaunchGroupVisitor(
            Collection<Protos.TaskInfo> taskInfos,
            String serviceName,
            Protos.FrameworkID frameworkID,
            UUID targetConfigurationId,
            SchedulerFlags schedulerFlags,
            SpecVisitor delegate) {
        this.taskInfos = taskInfos;
        this.serviceName = serviceName;
        this.frameworkID = frameworkID;
        this.targetConfigurationId = targetConfigurationId;
        this.schedulerFlags = schedulerFlags;
        this.delegate = delegate;
        this.collector = createVisitorResultCollector();

        this.operations = new ArrayList<>();
        this.isTaskActive = false;
    }

    @Override
    public PodInstanceRequirement visitImplementation(PodInstanceRequirement podInstanceRequirement) {
        this.podInstanceRequirement = podInstanceRequirement;

        return podInstanceRequirement;
    }

    @Override
    public PodSpec visitImplementation(PodSpec podSpec) {
        executorInfo = getExecutorInfo(podInstanceRequirement.getPodInstance().getPod());

        return podSpec;
    }

    @Override
    public TaskSpec visitImplementation(TaskSpec taskSpec) throws InvalidRequirementException {
        launchGroup = Protos.Offer.Operation.LaunchGroup.newBuilder();
        isTaskActive = true;
        Protos.TaskInfo.Builder taskBuilder = launchGroup.getTaskGroupBuilder().addTasksBuilder()
                .setName(TaskSpec.getInstanceName(podInstanceRequirement.getPodInstance(), taskSpec))
                .setTaskId(CommonIdUtils.emptyTaskId())
                .setSlaveId(CommonIdUtils.emptyAgentId());

        // create default labels:
        taskBuilder.setLabels(new TaskLabelWriter(taskBuilder)
                .setTargetConfiguration(targetConfigurationId)
                .setGoalState(taskSpec.getGoal())
                .setType(podInstanceRequirement.getPodInstance().getPod().getType())
                .setIndex(podInstanceRequirement.getPodInstance().getIndex())
                .toProto());

        if (taskSpec.getCommand().isPresent()) {
            Protos.CommandInfo.Builder commandBuilder = taskBuilder.getCommandBuilder()
                    .setValue(taskSpec.getCommand().get().getValue())
                    .setEnvironment(EnvUtils.toProto(getTaskEnvironment(
                            serviceName, podInstanceRequirement.getPodInstance(), taskSpec)));
            setBootstrapConfigFileEnv(taskBuilder.getCommandBuilder(), taskSpec);
            extendEnv(taskBuilder.getCommandBuilder(), podInstanceRequirement.getEnvironment());

            // Any URIs defined in PodSpec itself.
            for (URI uri : podInstanceRequirement.getPodInstance().getPod().getUris()) {
                commandBuilder.addUrisBuilder().setValue(uri.toString());
            }

            for (ConfigFileSpec config : taskSpec.getConfigFiles()) {
                commandBuilder.addUrisBuilder()
                        .setValue(ArtifactResource.getTemplateUrl(
                                serviceName,
                                targetConfigurationId,
                                podInstanceRequirement.getPodInstance().getPod().getType(),
                                taskSpec.getName(),
                                config.getName()))
                        .setOutputFile(getConfigTemplateDownloadPath(config))
                        .setExtract(false);
            }

            // Secrets are constructed differently from other envvars where the proto is concerned:
            for (SecretSpec secretSpec : podInstanceRequirement.getPodInstance().getPod().getSecrets()) {
                if (secretSpec.getEnvKey().isPresent()) {
                    commandBuilder.getEnvironmentBuilder().addVariablesBuilder()
                            .setName(secretSpec.getEnvKey().get())
                            .setType(Protos.Environment.Variable.Type.SECRET)
                            .setSecret(getReferenceSecret(secretSpec.getSecretPath()));
                }
            }

            if (podInstanceRequirement.getPodInstance().getPod().getUser().isPresent()) {
                commandBuilder.setUser(podInstanceRequirement.getPodInstance().getPod().getUser().get());
            }
        }

        if (taskSpec.getDiscovery().isPresent()) {
            taskBuilder.setDiscovery(getDiscoveryInfo(
                    taskSpec.getDiscovery().get(),
                    podInstanceRequirement.getPodInstance().getIndex()));
        }
        taskBuilder.setContainer(getContainerInfo(podInstanceRequirement.getPodInstance().getPod()));
        setHealthCheck(taskBuilder, serviceName, podInstanceRequirement.getPodInstance(), taskSpec);
        setReadinessCheck(taskBuilder, serviceName, podInstanceRequirement.getPodInstance(), taskSpec);

        setTaskKillGracePeriod(taskBuilder, taskSpec);

        return taskSpec;
    }

    @Override
    public void finalize(TaskSpec taskSpec) {
        isTaskActive = false;
    }

    @Override
    public ResourceSpec visitImplementation(ResourceSpec resourceSpec) {
        Protos.Resource.Builder resource = resourceSpec.getResource();
        addResource(resource);

        return resourceSpec;
    }

    @Override
    public VolumeSpec visitImplementation(VolumeSpec volumeSpec) {
        visitImplementation((ResourceSpec) volumeSpec);

        if (!isTaskActive) {
            Protos.Volume.Builder volumeBuilder = Protos.Volume.newBuilder();
            Protos.Volume.Source.SandboxPath.Builder sandboxPathBuilder = Protos.Volume.Source.SandboxPath.newBuilder();

            sandboxPathBuilder.setType(Protos.Volume.Source.SandboxPath.Type.PARENT)
                    .setPath(volumeSpec.getContainerPath());
            volumeBuilder.setMode(Protos.Volume.Mode.RW)
                    .setContainerPath(volumeSpec.getContainerPath())
                    .setSource(Protos.Volume.Source.newBuilder()
                            .setType(Protos.Volume.Source.Type.SANDBOX_PATH)
                            .setSandboxPath(sandboxPathBuilder));

            for (Protos.TaskInfo.Builder t : launchGroup.getTaskGroupBuilder().getTasksBuilderList()) {
                t.getContainerBuilder().addVolumes(volumeBuilder);
            }
        }

        return volumeSpec;
    }

    @Override
    public PortSpec visitImplementation(PortSpec portSpec) {
        visitImplementation((ResourceSpec) portSpec);

        return portSpec;
    }

    @Override
    public Optional<SpecVisitor> getDelegate() {
        return Optional.ofNullable(delegate);
    }

    @Override
    public void compileResultImplementation() {
        getVisitorResultCollector().setResult(operations);
    }

    @Override
    public VisitorResultCollector getVisitorResultCollector() {
        return collector;
    }

    private Protos.ExecutorInfo getExecutorInfo(PodSpec podSpec) {
        if (taskInfos.isEmpty()) {
            Protos.ExecutorInfo.Builder executorBuilder = Protos.ExecutorInfo.newBuilder()
                    .setType(Protos.ExecutorInfo.Type.DEFAULT)
                    .setName(podSpec.getType())
                    .setFrameworkId(frameworkID)
                    .setExecutorId(Protos.ExecutorID.newBuilder().setValue(""));
            AuxLabelAccess.setDcosSpace(executorBuilder, schedulerFlags.getDcosSpaceLabelValue());

            return executorBuilder.build();
        } else {
            return taskInfos.stream().findFirst().get().getExecutor();
        }
    }

    /**
     * Generates a Task environment containing the configured environment values from the {@link CommandSpec}, along
     * with a set of default environment variables that all SDK tasks get for free.
     */
    @VisibleForTesting
    public static Map<String, String> getTaskEnvironment(
            String serviceName, PodInstance podInstance, TaskSpec taskSpec) {
        Map<String, String> environmentMap = new TreeMap<>();

        // Task envvars from either of the following sources:
        // - ServiceSpec (provided by developer)
        // - TASKCFG_<podname>_* (provided by user, handled when parsing YAML, potentially overrides ServiceSpec)
        if (taskSpec.getCommand().isPresent()) {
            environmentMap.putAll(taskSpec.getCommand().get().getEnvironment());
        }

        // Default envvars for use by executors/developers
        // Unline the envvars added in getExecutorEnvironment(), these are specific to individual tasks and currently
        // aren't visible to sidecar tasks (as they would need to be added at the executor...):

        // Inject Pod Instance Index
        environmentMap.put(EnvConstants.POD_INSTANCE_INDEX_TASKENV, String.valueOf(podInstance.getIndex()));
        // Inject Framework Name (raw, not safe for use in hostnames)
        environmentMap.put(EnvConstants.FRAMEWORK_NAME_TASKENV, serviceName);
        // Inject Framework host domain (with hostname-safe framework name)
        environmentMap.put(EnvConstants.FRAMEWORK_HOST_TASKENV, EndpointUtils.toAutoIpDomain(serviceName));

        // Inject TASK_NAME as KEY:VALUE
        environmentMap.put(EnvConstants.TASK_NAME_TASKENV, TaskSpec.getInstanceName(podInstance, taskSpec));
        // Inject TASK_NAME as KEY for conditional mustache templating
        environmentMap.put(TaskSpec.getInstanceName(podInstance, taskSpec), "true");

        return environmentMap;
    }

    private static void setBootstrapConfigFileEnv(Protos.CommandInfo.Builder commandInfoBuilder, TaskSpec taskSpec) {
        if (taskSpec.getConfigFiles() == null) {
            return;
        }
        for (ConfigFileSpec config : taskSpec.getConfigFiles()) {
            // For use by bootstrap process: an environment variable pointing to (comma-separated):
            // a. where the template file was downloaded (by the mesos fetcher)
            // b. where the rendered result should go
            commandInfoBuilder.setEnvironment(EnvUtils.withEnvVar(
                    commandInfoBuilder.getEnvironment(),
                    String.format(CONFIG_TEMPLATE_KEY_FORMAT, EnvUtils.toEnvName(config.getName())),
                    String.format("%s,%s", getConfigTemplateDownloadPath(config), config.getRelativePath())));
        }
    }

    private static void extendEnv(Protos.CommandInfo.Builder builder, Map<String, String> environment) {
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            builder.getEnvironmentBuilder().addVariablesBuilder().setName(entry.getKey()).setValue(entry.getValue());
        }
    }

    private static Protos.Secret getReferenceSecret(String secretPath) {
        return Protos.Secret.newBuilder()
                .setType(Protos.Secret.Type.REFERENCE)
                .setReference(Protos.Secret.Reference.newBuilder().setName(secretPath))
                .build();
    }

    private static String getConfigTemplateDownloadPath(ConfigFileSpec config) {
        // Name is unique.
        return String.format("%s%s", CONFIG_TEMPLATE_DOWNLOAD_PATH, config.getName());
    }

    private void setHealthCheck(
            Protos.TaskInfo.Builder taskInfo,
            String serviceName,
            PodInstance podInstance,
            TaskSpec taskSpec) {
        if (!taskSpec.getHealthCheck().isPresent()) {
            LOGGER.debug("No health check defined for taskSpec: {}", taskSpec.getName());
            return;
        }

        HealthCheckSpec healthCheckSpec = taskSpec.getHealthCheck().get();
        Protos.HealthCheck.Builder healthCheckBuilder = taskInfo.getHealthCheckBuilder();
        healthCheckBuilder
                .setDelaySeconds(healthCheckSpec.getDelay())
                .setIntervalSeconds(healthCheckSpec.getInterval())
                .setTimeoutSeconds(healthCheckSpec.getTimeout())
                .setConsecutiveFailures(healthCheckSpec.getMaxConsecutiveFailures())
                .setGracePeriodSeconds(healthCheckSpec.getGracePeriod());

        healthCheckBuilder.setType(Protos.HealthCheck.Type.COMMAND);

        healthCheckBuilder.getCommandBuilder()
                .setValue(healthCheckSpec.getCommand())
                .setEnvironment(EnvUtils.toProto(getTaskEnvironment(serviceName, podInstance, taskSpec)));
    }

    private void setReadinessCheck(
            Protos.TaskInfo.Builder taskInfoBuilder,
            String serviceName,
            PodInstance podInstance,
            TaskSpec taskSpec) {
        if (!taskSpec.getReadinessCheck().isPresent()) {
            LOGGER.debug("No readiness check defined for taskSpec: {}", taskSpec.getName());
            return;
        }

        ReadinessCheckSpec readinessCheckSpec = taskSpec.getReadinessCheck().get();

        Protos.CheckInfo.Builder builder = taskInfoBuilder.getCheckBuilder()
                .setType(Protos.CheckInfo.Type.COMMAND)
                .setDelaySeconds(readinessCheckSpec.getDelay())
                .setIntervalSeconds(readinessCheckSpec.getInterval())
                .setTimeoutSeconds(readinessCheckSpec.getTimeout());
        builder.getCommandBuilder().getCommandBuilder()
                .setValue(readinessCheckSpec.getCommand())
                .setEnvironment(EnvUtils.toProto(getTaskEnvironment(serviceName, podInstance, taskSpec)));
    }

    private static void setTaskKillGracePeriod(
            Protos.TaskInfo.Builder taskInfoBuilder,
            TaskSpec taskSpec) throws InvalidRequirementException {
        Integer taskKillGracePeriodSeconds = taskSpec.getTaskKillGracePeriodSeconds();
        if (taskKillGracePeriodSeconds == null) {
            taskKillGracePeriodSeconds = 0;
        } else if (taskKillGracePeriodSeconds < 0) {
            throw new InvalidRequirementException(String.format(
                    "kill-grace-period must be zero or a positive integer, received: %d",
                    taskKillGracePeriodSeconds));
        }
        long taskKillGracePeriodNanoseconds = 1000000000L * taskKillGracePeriodSeconds;
        Protos.DurationInfo taskKillGracePeriodDuration = Protos.DurationInfo.newBuilder()
                .setNanoseconds(taskKillGracePeriodNanoseconds)
                .build();

        Protos.KillPolicy.Builder killPolicyBuilder = Protos.KillPolicy.newBuilder()
                .setGracePeriod(taskKillGracePeriodDuration);

        taskInfoBuilder.setKillPolicy(killPolicyBuilder.build());
    }

    private static Protos.DiscoveryInfo getDiscoveryInfo(DiscoverySpec discoverySpec, int index) {
        Protos.DiscoveryInfo.Builder builder = Protos.DiscoveryInfo.newBuilder();
        if (discoverySpec.getPrefix().isPresent()) {
            builder.setName(String.format("%s-%d", discoverySpec.getPrefix().get(), index));
        }
        if (discoverySpec.getVisibility().isPresent()) {
            builder.setVisibility(discoverySpec.getVisibility().get());
        } else {
            builder.setVisibility(Constants.DEFAULT_TASK_DISCOVERY_VISIBILITY);
        }

        return builder.build();
    }

    /**
     * Get the ContainerInfo for either an Executor or a Task. Since we support both default and custom executors at
     * the moment, there is some conditional logic in here -- with the default executor, things like rlimits and images
     * must be specified at the task level only, while secrets volumes must be specified at the executor level.
     *
     * @param podSpec The Spec for the task or executor that this container is being attached to
     * @return the ContainerInfo to be attached
     */
    private Protos.ContainerInfo getContainerInfo(PodSpec podSpec) {
        Protos.ContainerInfo.Builder containerInfo = Protos.ContainerInfo.newBuilder()
                .setType(Protos.ContainerInfo.Type.MESOS);

        if (isTaskActive) {
            containerInfo.getLinuxInfoBuilder().setSharePidNamespace(podSpec.getSharePidNamespace());

            if (podSpec.getImage().isPresent()) {
                containerInfo.getMesosBuilder().getImageBuilder()
                        .setType(Protos.Image.Type.DOCKER)
                        .getDockerBuilder().setName(podSpec.getImage().get());
            }

            if (!podSpec.getRLimits().isEmpty()) {
                containerInfo.setRlimitInfo(getRLimitInfo(podSpec.getRLimits()));
            }
        } else {
            if (!podSpec.getNetworks().isEmpty()) {
                containerInfo.addAllNetworkInfos(
                        podSpec.getNetworks().stream()
                                .map(LaunchGroupVisitor::getNetworkInfo)
                                .collect(Collectors.toList()));
            }

            for (Protos.Volume secretVolume : getExecutorInfoSecretVolumes(podSpec.getSecrets())) {
                containerInfo.addVolumes(secretVolume);
            }
        }

        return containerInfo.build();
    }

    private static Protos.RLimitInfo getRLimitInfo(Collection<RLimit> rlimits) {
        Protos.RLimitInfo.Builder rLimitInfoBuilder = Protos.RLimitInfo.newBuilder();

        for (RLimit rLimit : rlimits) {
            Optional<Long> soft = rLimit.getSoft();
            Optional<Long> hard = rLimit.getHard();
            Protos.RLimitInfo.RLimit.Builder rLimitsBuilder = Protos.RLimitInfo.RLimit.newBuilder()
                    .setType(rLimit.getEnum());

            // RLimit itself validates that both or neither of these are present.
            if (soft.isPresent() && hard.isPresent()) {
                rLimitsBuilder.setSoft(soft.get()).setHard(hard.get());
            }
            rLimitInfoBuilder.addRlimits(rLimitsBuilder);
        }

        return rLimitInfoBuilder.build();
    }

    private static Collection<Protos.Volume> getExecutorInfoSecretVolumes(Collection<SecretSpec> secretSpecs) {
        Collection<Protos.Volume> volumes = new ArrayList<>();

        for (SecretSpec secretSpec: secretSpecs) {
            if (secretSpec.getFilePath().isPresent()) {
                volumes.add(Protos.Volume.newBuilder()
                        .setSource(Protos.Volume.Source.newBuilder()
                                .setType(Protos.Volume.Source.Type.SECRET)
                                .setSecret(getReferenceSecret(secretSpec.getSecretPath()))
                                .build())
                        .setContainerPath(secretSpec.getFilePath().get())
                        .setMode(Protos.Volume.Mode.RO)
                        .build());
            }
        }
        return volumes;
    }

    private static Protos.NetworkInfo getNetworkInfo(NetworkSpec networkSpec) {
        LOGGER.info("Loading NetworkInfo for network named \"{}\"", networkSpec.getName());
        Protos.NetworkInfo.Builder netInfoBuilder = Protos.NetworkInfo.newBuilder();
        netInfoBuilder.setName(networkSpec.getName());
        DcosConstants.warnIfUnsupportedNetwork(networkSpec.getName());

        if (!networkSpec.getPortMappings().isEmpty()) {
            for (Map.Entry<Integer, Integer> e : networkSpec.getPortMappings().entrySet()) {
                netInfoBuilder.addPortMappingsBuilder()
                        .setHostPort(e.getKey())
                        .setContainerPort(e.getValue());
            }
        }

        if (!networkSpec.getLabels().isEmpty()) {
            AuxLabelAccess.setNetworkLabels(netInfoBuilder, networkSpec.getLabels());
        }

        return netInfoBuilder.build();
    }
    private void addResource(Protos.Resource.Builder resource) {
        if (isTaskActive) {
            launchGroup.getTaskGroupBuilder().getTasksBuilder(0).addResources(resource);
        } else {
            launchGroup.getExecutorBuilder().addResources(resource);
        }
    }
}
