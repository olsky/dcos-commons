package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.specification.VolumeSpec;
import org.apache.mesos.Protos;

public class DefaultExecutorVolumePreparer extends VolumePreparer {
    private final VolumeSpec volumeSpec;
    private final PodEntityCursor podEntityCursor;

    public DefaultExecutorVolumePreparer(VolumeSpec volumeSpec, PodEntityCursor podEntityCursor) {
        this.volumeSpec = volumeSpec;
        this.podEntityCursor = podEntityCursor;
    }

    @Override
    protected PodEntityCursor getPodEntityCursor() {
        return podEntityCursor;
    }

    @Override
    public LaunchSetPreparer fromFormerLaunch(Protos.Offer.Operation.LaunchGroup launchGroup) {
        String reservationId = "";
        String persistenceId = "";

        return new VolumeReservationPreparer(this, reservationId, persistenceId);
    }

    @Override
    protected VolumeSpec getVolumeSpec() {
        return volumeSpec;
    }

    @Override
    protected PodInfoBuilder setVolume(PodInfoBuilder podInfoBuilder) {
        // I wonder if these classes should live inside the podinfobuilder, given what they know
        return podInfoBuilder;
    }
}
