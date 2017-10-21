package com.mesosphere.sdk.offer.evaluate.placement;

import com.google.protobuf.TextFormat;
import com.mesosphere.sdk.dcos.DcosConstants;
import com.mesosphere.sdk.offer.Constants;
import com.mesosphere.sdk.offer.CreateOfferRecommendation;
import com.mesosphere.sdk.offer.MesosResource;
import com.mesosphere.sdk.offer.MesosResourcePool;
import com.mesosphere.sdk.offer.OfferRecommendation;
import com.mesosphere.sdk.offer.RangeUtils;
import com.mesosphere.sdk.offer.ReserveOfferRecommendation;
import com.mesosphere.sdk.offer.ResourceBuilder;
import com.mesosphere.sdk.offer.ResourceUtils;
import com.mesosphere.sdk.offer.UnreserveOfferRecommendation;
import com.mesosphere.sdk.offer.ValueUtils;
import com.mesosphere.sdk.offer.evaluate.EvaluationOutcome;
import com.mesosphere.sdk.offer.evaluate.ReservationCreator;
import com.mesosphere.sdk.offer.evaluate.SpecVisitor;
import com.mesosphere.sdk.offer.evaluate.VisitorResultCollector;
import com.mesosphere.sdk.scheduler.plan.PodInstanceRequirement;
import com.mesosphere.sdk.specification.DefaultResourceSpec;
import com.mesosphere.sdk.specification.PodSpec;
import com.mesosphere.sdk.specification.PortSpec;
import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.TaskSpec;
import com.mesosphere.sdk.specification.VolumeSpec;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.mesosphere.sdk.offer.evaluate.EvaluationOutcome.fail;
import static com.mesosphere.sdk.offer.evaluate.EvaluationOutcome.pass;

public class OfferConsumptionVisitor implements SpecVisitor<OfferConsumptionVisitor.OfferConsumptionResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfferConsumptionVisitor.class);

    private final MesosResourcePool mesosResourcePool;
    private final ReservationCreator reservationCreator;
    private final SpecVisitor delegate;
    private final List<EvaluationOutcome> evaluationOutcomes;
    private final List<OfferRecommendation> offerRecommendations;
    private VisitorResultCollector<OfferConsumptionResult> collector;
    private PodSpec currentPodSpec;
    // TODO(mrb)
    // APPROACHES: traverse task infos in both offer consumption and reservation visitors;
    // pass the resource pool to the existing pod visitor; this is slightly distasteful but probably the simplest way;
    // do some sort of spec arithmetic and put the unreserves in a completely separate stage entirely.

    public OfferConsumptionVisitor(
            MesosResourcePool mesosResourcePool, ReservationCreator reservationCreator, SpecVisitor delegate) {
        this.mesosResourcePool = mesosResourcePool;
        this.delegate = delegate;
        this.evaluationOutcomes = new ArrayList<>();
        this.offerRecommendations = new ArrayList<>();
        this.collector = createVisitorResultCollector();
        this.reservationCreator = reservationCreator;
    }

    @Override
    public PodInstanceRequirement visitImplementation(PodInstanceRequirement podInstanceRequirement) {
        return podInstanceRequirement;
    }

    @Override
    public PodSpec visitImplementation(PodSpec podSpec) {
        this.currentPodSpec = podSpec;
        return podSpec;
    }

    @Override
    public TaskSpec visitImplementation(TaskSpec taskSpec) {
        return taskSpec;
    }

    @Override
    public ResourceSpec visitImplementation(ResourceSpec resourceSpec) {
        Optional<String> resourceId = ResourceUtils.getResourceId(resourceSpec.getResource().build());
        Optional<MesosResource> mesosResourceOptional = consume(resourceSpec, resourceId, mesosResourcePool);
        if (!mesosResourceOptional.isPresent()) {
            evaluationOutcomes.add(fail(
                    this,
                    "Offer failed to satisfy: %s with resourceId: %s",
                    resourceSpec,
                    resourceId).build());
        }

        OfferRecommendation offerRecommendation = null;
        MesosResource mesosResource = mesosResourceOptional.get();

        if (ValueUtils.equal(mesosResource.getValue(), resourceSpec.getValue())) {
            LOGGER.info("    Resource '{}' matches required value: {}",
                    resourceSpec.getName(),
                    TextFormat.shortDebugString(mesosResource.getValue()),
                    TextFormat.shortDebugString(resourceSpec.getValue()));

            if (!resourceId.isPresent()) {
                // Initial reservation of resources
                LOGGER.info("    Resource '{}' requires a RESERVE operation", resourceSpec.getName());
                offerRecommendation = new ReserveOfferRecommendation(
                        mesosResourcePool.getOffer(), resourceSpec.getResource(), reservationCreator);
                evaluationOutcomes.add(pass(
                        this,
                        Arrays.asList(offerRecommendation),
                        "Offer contains sufficient '%s': for resource: '%s' with resourceId: '%s'",
                        resourceSpec.getName(),
                        resourceSpec,
                        resourceId)
                        .build());
            } else {
                evaluationOutcomes.add(pass(
                        this,
                        Collections.emptyList(),
                        "Offer contains sufficient previously reserved '%s':" +
                                " for resource: '%s' with resourceId: '%s'",
                        resourceSpec.getName(),
                        resourceSpec,
                        resourceId)
                        .build());
            }
        } else {
            Protos.Value difference = ValueUtils.subtract(resourceSpec.getValue(), mesosResource.getValue());
            if (ValueUtils.compare(difference, ValueUtils.getZero(difference.getType())) > 0) {
                LOGGER.info("    Reservation for resource '{}' needs increasing from current {} to required {}",
                        resourceSpec.getName(),
                        TextFormat.shortDebugString(mesosResource.getValue()),
                        TextFormat.shortDebugString(resourceSpec.getValue()));

                ResourceSpec requiredAdditionalResources = DefaultResourceSpec.newBuilder(resourceSpec)
                        .value(difference)
                        .build();
                mesosResourceOptional = mesosResourcePool.consumeReservableMerged(
                        requiredAdditionalResources.getName(),
                        requiredAdditionalResources.getValue(),
                        Constants.ANY_ROLE);

                if (!mesosResourceOptional.isPresent()) {
                    evaluationOutcomes.add(fail(
                            this,
                            "Insufficient resources to increase reservation of resource '%s' with resourceId",
                            resourceSpec,
                            resourceId)
                            .build());
                }

                // Reservation of additional resources
                offerRecommendation = new ReserveOfferRecommendation(
                        mesosResourcePool.getOffer(), resourceSpec.getResource(), reservationCreator);
                evaluationOutcomes.add(pass(
                        this,
                        Arrays.asList(offerRecommendation),
                        "Offer contains sufficient '%s': for increasing resource: '%s' with resourceId: '%s'",
                        resourceSpec.getName(),
                        resourceSpec,
                        resourceId)
                        .build());
            } else {
                // TODO(mrb): Where should unreserves happen??? Seems orthogonal to here...
                LOGGER.info("    Reservation for resource '%s' needs decreasing from current %s to required {}",
                        resourceSpec.getName(),
                        TextFormat.shortDebugString(mesosResource.getValue()),
                        TextFormat.shortDebugString(resourceSpec.getValue()));

                Protos.Value unreserve = ValueUtils.subtract(mesosResource.getValue(), resourceSpec.getValue());
                Protos.Resource resource = ResourceBuilder.fromSpec(resourceSpec, resourceId)
                        .setValue(unreserve)
                        .build();
                // Unreservation of no longer needed resources
                offerRecommendation = new UnreserveOfferRecommendation(
                        mesosResourcePool.getOffer(),
                        resource);
                evaluationOutcomes.add(pass(
                        this,
                        Arrays.asList(offerRecommendation),
                        "Decreased '%s': for resource: '%s' with resourceId: '%s'",
                        resourceSpec.getName(),
                        resourceSpec,
                        resourceId)
                        .build());
            }
        }

        return resourceSpec;
    }

    @Override
    public VolumeSpec visitImplementation(VolumeSpec volumeSpec) {
        Optional<String> resourceId = ResourceUtils.getResourceId(volumeSpec.getResource().build());
        String detailsClause = resourceId.isPresent() ? "previously reserved " : "";

        // if has running exec
        Optional<MesosResource> mesosResourceOptional;
        if (volumeSpec.getType().equals(VolumeSpec.Type.ROOT)) {
            return (VolumeSpec) visitImplementation((ResourceSpec) volumeSpec);
        } else {
            if (!resourceId.isPresent()) {
                mesosResourceOptional =
                        mesosResourcePool.consumeAtomic(Constants.DISK_RESOURCE_TYPE, volumeSpec.getValue());
            } else {
                mesosResourceOptional =
                        mesosResourcePool.getReservedResourceById(resourceId.get());
            }

            if (!mesosResourceOptional.isPresent()) {
                evaluationOutcomes.add(fail(this, "Failed to find MOUNT volume for '%s'.", volumeSpec).build());
            }

            evaluationOutcomes.add(pass(
                    this,
                    Collections.emptyList(),
                    "Offer has sufficient %s'disk': for resource: '%s' with resourceId: '%s' and persistenceId: '%s'",
                    detailsClause, volumeSpec, resourceId, "PERSISTENCE ID")
                    .build());
        }

        Optional<String> persistenceId = ResourceUtils.getPersistenceId(volumeSpec.getResource().build());
        if (!persistenceId.isPresent()) {
            // TODO(mrb): fix
            offerRecommendations.add(
                    new CreateOfferRecommendation(mesosResourcePool.getOffer(), volumeSpec.getResource().build()));
        }

        return volumeSpec;
    }

    @Override
    public PortSpec visitImplementation(PortSpec portSpec) {
        Long assignedPort = portSpec.getPort();
        if (assignedPort == 0) {
            assignedPort = mesosResourcePool.getUnassignedPort(currentPodSpec);
            if (assignedPort == null) {
                evaluationOutcomes.add(fail(
                        this,
                        "No ports were available for dynamic claim in offer," +
                                " and no matching port %s was present in prior task: %s %s",
                        portSpec.getPortName(),
                        TextFormat.shortDebugString(mesosResourcePool.getOffer()))
                        .build());
            }
            portSpec = PortSpec.withValue(
                    portSpec,
                    Protos.Value.newBuilder()
                            .setType(Protos.Value.Type.RANGES)
                            .setRanges(RangeUtils.fromSingleValue(assignedPort))
                            .build());
        }

        if (requiresHostPorts(portSpec.getNetworkNames())) {
            portSpec = (PortSpec) visitImplementation((ResourceSpec) portSpec);
        } else {
            evaluationOutcomes.add(pass(
                    this,
                    "Port %s doesn't require resource reservation, ignoring resource requirements and using port %d",
                    portSpec.getPortName(),
                    assignedPort)
                    .build());
        }

        return portSpec;
    }

    @Override
    public Optional<SpecVisitor> getDelegate() {
        return Optional.ofNullable(delegate);
    }

    @Override
    public void compileResultImplementation() {
        getVisitorResultCollector().setResult(new OfferConsumptionResult(offerRecommendations, evaluationOutcomes));
    }

    @Override
    public VisitorResultCollector<OfferConsumptionResult> getVisitorResultCollector() {
        return collector;
    }

    private static Optional<MesosResource> consume(
            ResourceSpec resourceSpec,
            Optional<String> resourceId,
            MesosResourcePool pool) {
        if (!resourceId.isPresent()) {
            return pool.consumeReservableMerged(
                    resourceSpec.getName(),
                    resourceSpec.getValue(),
                    resourceSpec.getPreReservedRole());
        } else {
            return pool.consumeReserved(resourceSpec.getName(), resourceSpec.getValue(), resourceId.get());
        }
    }

    private static boolean requiresHostPorts(Collection<String> networkNames) {
        if (networkNames.isEmpty()) {  // no network names, must be on host network and use the host IP
            return true;
        } else {
            return networkNames.stream()
                    .filter(DcosConstants::networkSupportsPortMapping)
                    .count() > 0;
        }
    }

    public static class OfferConsumptionResult {
        private final List<OfferRecommendation> offerRecommendations;
        private final List<EvaluationOutcome> evaluationOutcomes;

        OfferConsumptionResult(
                List<OfferRecommendation> offerRecommendations, List<EvaluationOutcome> evaluationOutcomes) {
            this.offerRecommendations = offerRecommendations;
            this.evaluationOutcomes = evaluationOutcomes;
        }

        public List<OfferRecommendation> getOfferRecommendations() {
            return offerRecommendations;
        }

        public List<EvaluationOutcome> getEvaluationOutcomes() {
            return evaluationOutcomes;
        }
    }
}
