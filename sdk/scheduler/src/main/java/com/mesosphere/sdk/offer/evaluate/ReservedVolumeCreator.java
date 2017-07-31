package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.VolumeSpec;
import org.apache.mesos.Protos;

import java.util.Optional;
import java.util.UUID;

public class ReservedVolumeCreator implements VolumeCreator {
    private final VolumeSpec volumeSpec;
    private final Optional<String> resourceId;
    private final Optional<String> persistenceId;

    public ReservedVolumeCreator(
            VolumeSpec volumeSpec, Optional<String> resourceId, Optional<String> persistenceId) {
        this.volumeSpec = volumeSpec;
        this.resourceId = resourceId;
        this.persistenceId = persistenceId;
    }

    @Override
    public ResourceSpec getResourceSpec() {
        return getVolumeSpec();
    }

    @Override
    public VolumeSpec getVolumeSpec() {
        return volumeSpec;
    }

    @Override
    public Protos.Resource.Builder getResource() {
        Protos.Resource.Builder resourceBuilder = volumeSpec.getResource();
        String finalResourceId = resourceId.isPresent() ? resourceId.get() : UUID.randomUUID().toString();
        String finalPersistenceId = persistenceId.isPresent() ? persistenceId.get() : UUID.randomUUID().toString();

        // TODO(mrb): This needs to take into account resource refinement, ugh
        resourceBuilder.addReservationsBuilder()
                .setRole(volumeSpec.getRole())
                .setPrincipal(volumeSpec.getPrincipal())
                .getLabelsBuilder()
                // set constant
                        .addLabelsBuilder().setKey("resource_id").setValue(finalResourceId);
        resourceBuilder.getDiskBuilder().getPersistenceBuilder().setId(finalPersistenceId);

        return resourceBuilder;
    }
}
