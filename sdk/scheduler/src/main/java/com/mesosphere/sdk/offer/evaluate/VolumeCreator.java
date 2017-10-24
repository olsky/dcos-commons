package com.mesosphere.sdk.offer.evaluate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mesosphere.sdk.specification.VolumeSpec;
import org.apache.mesos.Protos;

public interface VolumeCreator extends ResourceCreator {

    @JsonIgnore
    VolumeSpec getVolumeSpec();

    @Override
    default Protos.Resource.Builder getResource() {
        Protos.Resource.Builder builder = ResourceCreator.super.getResource();
        Protos.Resource.DiskInfo.Builder diskBuilder = builder.getDiskBuilder();
        VolumeSpec volumeSpec = getVolumeSpec();

        diskBuilder.getVolumeBuilder()
                .setContainerPath(volumeSpec.getContainerPath())
                .setMode(Protos.Volume.Mode.RW);

        if (volumeSpec.getType().equals(VolumeSpec.Type.MOUNT)) {
            diskBuilder.getSourceBuilder()
                    .setType(Protos.Resource.DiskInfo.Source.Type.MOUNT);
        }

        return builder;
    }
}
