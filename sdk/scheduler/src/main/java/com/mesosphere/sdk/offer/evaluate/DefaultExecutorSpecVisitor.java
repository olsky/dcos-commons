package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.offer.MesosResourcePool;
import com.mesosphere.sdk.specification.PodSpec;
import com.mesosphere.sdk.specification.PortSpec;
import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.TaskSpec;
import com.mesosphere.sdk.specification.VolumeSpec;
import org.apache.mesos.Protos;

import java.util.ArrayList;
import java.util.List;

public class DefaultExecutorSpecVisitor extends OperationBuilder {
    private final MesosResourcePool mesosResourcePool;
    private final List<Protos.Offer.Operation> operations;
    private final Protos.Offer.Operation.LaunchGroup.Builder launchGroup;
    private Protos.TaskInfo.Builder activeTask;
    private Protos.ExecutorInfo.Builder activeExecutor;
    private PodSpec podSpec;

    public DefaultExecutorSpecVisitor(MesosResourcePool mesosResourcePool) {
        this.mesosResourcePool = mesosResourcePool;
        this.launchGroup = Protos.Offer.Operation.LaunchGroup.newBuilder();
        this.operations = new ArrayList<>();
        // Create executor?
    }

    @Override
    public void visit(PodSpec podSpec) {
        setPodSpec(podSpec);
    }

    @Override
    public void visit(TaskSpec taskSpec) {
        setActiveTask(launchGroup.getTaskGroupBuilder().addTasksBuilder());
    }

    @Override
    public void visit(ResourceSpec resourceSpec) {
        Protos.Resource.Builder resource = resourceSpec.getResource();

        if (isTaskActive()) {
            activeTask.addResources(resource);
        } else {
            activeExecutor.addResources(resource);
        }
    }

    @Override
    public void visit(VolumeSpec volumeSpec) {
        Protos.Resource.Builder resource = volumeSpec.getResource();

        addResource(resource);
        if (!isTaskActive()) {
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
    }

    @Override
    public void visit(PortSpec portSpec) {
        long port = portSpec.getPort() == 0 ? mesosResourcePool.getUnassignedPort(podSpec) : portSpec.getPort();
        Protos.Value.Ranges.Builder rangesBuilder = Protos.Value.Ranges.newBuilder();

        rangesBuilder.addRangeBuilder().setBegin(port).setEnd(port);
        Protos.Resource.Builder resource = PortSpec.withValue(
                portSpec, Protos.Value.newBuilder().setRanges(rangesBuilder).build()).getResource();
        addResource(resource);
    }

    @Override
    public void finalize(PodSpec podSpec) {

    }

    @Override
    public void finalize(TaskSpec taskSpec) {
        launch(taskSpec);
    }

    private void addResource(Protos.Resource.Builder resource) {
        if (isTaskActive()) {
            activeTask.addResources(resource);
        } else {
            activeExecutor.addResources(resource);
        }
    }

    private boolean isTaskActive() {
        return activeTask != null;
    }

    private void setActiveTask(Protos.TaskInfo.Builder task) {
        activeExecutor = null;
        activeTask = task;
    }

    private void setActiveExecutor() {
        activeExecutor = launchGroup.getExecutorBuilder();
    }

    private void setPodSpec(PodSpec podSpec) {
        this.podSpec = podSpec;
    }

    @Override
    protected void launch(TaskSpec taskSpec) {

    }

    @Override
    protected List<Protos.Offer.Operation> getOperations() {
        return operations;
    }
}
