package com.mesosphere.sdk.scheduler;

import com.mesosphere.sdk.dcos.Capabilities;
import com.mesosphere.sdk.offer.MesosResource;
import com.mesosphere.sdk.offer.MesosResourcePool;
import com.mesosphere.sdk.offer.OfferRecommendation;
import com.mesosphere.sdk.offer.evaluate.ExistingPodVisitor;
import com.mesosphere.sdk.offer.evaluate.HierarchicalReservationCreator;
import com.mesosphere.sdk.offer.evaluate.LaunchGroupVisitor;
import com.mesosphere.sdk.offer.evaluate.LaunchVisitor;
import com.mesosphere.sdk.offer.evaluate.LegacyReservationCreator;
import com.mesosphere.sdk.offer.evaluate.ReservationCreator;
import com.mesosphere.sdk.offer.evaluate.SpecVisitor;
import com.mesosphere.sdk.offer.evaluate.placement.OfferConsumptionVisitor;
import org.apache.mesos.Protos;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class OfferEvaluationComponentFactory {
    private final Capabilities capabilities;
    private final String serviceName;
    private final Protos.FrameworkID frameworkID;
    private final UUID targetConfigurationId;
    private final SchedulerFlags schedulerFlags;

    private ReservationCreator reservationCreator;

    public OfferEvaluationComponentFactory(
            Capabilities capabilities,
            String serviceName,
            Protos.FrameworkID frameworkID,
            UUID targetConfigurationId,
            SchedulerFlags schedulerFlags) {
        this.capabilities = capabilities;
        this.serviceName = serviceName;
        this.frameworkID = frameworkID;
        this.targetConfigurationId = targetConfigurationId;
        this.schedulerFlags = schedulerFlags;
    }

    public ReservationCreator getReservationCreator() {
        if (reservationCreator == null) {
            if (capabilities.supportsPreReservedResources()) {
                reservationCreator = new HierarchicalReservationCreator();
            } else {
                reservationCreator = new LegacyReservationCreator();
            }
        }

        return reservationCreator;
    }

    public ExistingPodVisitor getExistingPodVisitor(
            MesosResourcePool mesosResourcePool, Collection<Protos.TaskInfo> taskInfos, SpecVisitor delegate) {
        return new ExistingPodVisitor(mesosResourcePool, taskInfos, getReservationCreator(), delegate);
    }

    public OfferConsumptionVisitor getOfferConsumptionVisitor(
            MesosResourcePool mesosResourcePool, SpecVisitor delegate) {
        return new OfferConsumptionVisitor(mesosResourcePool, getReservationCreator(), delegate);
    }

    public SpecVisitor<List<OfferRecommendation>> getLaunchOperationVisitor(
            MesosResourcePool mesosResourcePool, Collection<Protos.TaskInfo> taskInfos, SpecVisitor delegate) {
        if (capabilities.supportsDefaultExecutor()) {
            return new LaunchGroupVisitor(
                    taskInfos,
                    mesosResourcePool.getOffer(),
                    serviceName,
                    frameworkID,
                    targetConfigurationId,
                    schedulerFlags,
                    delegate);
        }

        return new LaunchVisitor(
                taskInfos,
                mesosResourcePool.getOffer(),
                serviceName,
                frameworkID,
                targetConfigurationId,
                schedulerFlags,
                delegate);
    }
}
