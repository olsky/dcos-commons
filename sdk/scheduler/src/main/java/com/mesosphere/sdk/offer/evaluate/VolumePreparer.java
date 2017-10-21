package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.specification.VolumeSpec;
import org.apache.mesos.Protos;

import java.util.Optional;
import java.util.UUID;

public abstract class VolumePreparer extends ResourcePreparer implements ResourceCreator {

    protected abstract Optional<String> getExistingPersistenceId();

    protected abstract PodEntityCursor getPodEntityCursor();

    protected abstract VolumeSpec getVolumeSpec();

    protected abstract LaunchSetBuilder setVolume(LaunchSetBuilder launchSetBuilder);

    protected LaunchSetBuilder appendResource(LaunchSetBuilder launchSetBuilder) {
        getPodEntityCursor().with(launchSetBuilder).appendResource(this);

        return launchSetBuilder;
    }

    @Override
    public Protos.Resource.Builder getResource() {
        Protos.Resource.Builder builder = ResourceCreator.super.getResource();
        Protos.Resource.DiskInfo.Builder diskBuilder = builder.getDiskBuilder();
        VolumeSpec volumeSpec = getVolumeSpec();

        diskBuilder.getVolumeBuilder()
                .setContainerPath(volumeSpec.getContainerPath())
                .setMode(Protos.Volume.Mode.RW);
        diskBuilder.getPersistenceBuilder()
                .setPrincipal(volumeSpec.getPrincipal())
                .setId(getPersistenceId());

        if (volumeSpec.getType().equals(VolumeSpec.Type.MOUNT)) {
            diskBuilder.getSourceBuilder()
                    .setType(Protos.Resource.DiskInfo.Source.Type.MOUNT);
        }

        return builder;
    }

    protected String getPersistenceId() {
        Optional<String> existingPersistenceId = getExistingPersistenceId();

        return existingPersistenceId.isPresent() ? existingPersistenceId.get() : UUID.randomUUID().toString();
    }

    @Override
    protected LaunchSetBuilder performExtraCleanup(
            LaunchSetBuilder launchSetBuilder, LaunchedPodEntity launchedPodEntity) {
        return launchSetBuilder;
    }

    @Override
    public LaunchSetBuilder prepare(LaunchSetBuilder launchSetBuilder) {
        appendResource(launchSetBuilder);
        setVolume(launchSetBuilder);

        return launchSetBuilder;
    }
}
