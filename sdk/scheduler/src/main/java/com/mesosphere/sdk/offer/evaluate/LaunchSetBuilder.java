package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.specification.VolumeSpec;
import org.apache.mesos.Protos;

public abstract class LaunchSetBuilder {

    public abstract LaunchSetBuilder createTaskResource(String taskName, ResourceCreator resourceCreator);

    public abstract LaunchSetBuilder createExecutorResource(ResourceCreator resourceCreator);

    public abstract void registerPodVolume(VolumeSpec volumeSpec);

    public boolean hasRunningExecutor() {
        return false;
    }

    // THe port preparer will call this shit inside
    public abstract long getUnassignedPort(Protos.Offer offer);
    /*

        boolean isRunningExecutor = podInfoBuilder.getExecutorBuilder().isPresent() &&
                isRunningExecutor(podInfoBuilder.getExecutorBuilder().get().build(), mesosResourcePool.getOffer());
        if (taskName == null && isRunningExecutor && resourceId.isPresent() && persistenceId.isPresent()) {
            // This is a volume on a running executor, so it isn't present in the offer, but we need to make sure to
            // add it to the ExecutorInfo as well as whatever task is being launched.
            podInfoBuilder.setExecutorVolume(volumeSpec);
            mesosResource = new MesosResource(
                    PodInfoBuilder.getExistingExecutorVolume(volumeSpec, resourceId.get(), persistenceId.get()));

            return pass(
                    this,
                    Collections.emptyList(),
                    "Offer contains executor with existing volume with resourceId: '%s' and persistenceId: '%s'",
                    resourceId,
                    persistenceId)
                    .mesosResource(mesosResource)
                    .build();
        }
    private static boolean isRunningExecutor(Protos.ExecutorInfo executorInfo, Protos.Offer offer) {
        for (Protos.ExecutorID execId : offer.getExecutorIdsList()) {
            if (execId.equals(executorInfo.getExecutorId())) {
                return true;
            }
        }

        return false;
    }
     */
}
