package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.TaskSpec;
import com.mesosphere.sdk.specification.VolumeSpec;
import org.apache.mesos.Protos;

import java.util.ArrayList;
import java.util.List;

public abstract class OperationBuilder implements SpecVisitor {

    protected void reserve(ResourceSpec resourceSpec) {
        getOperations().add(Protos.Offer.Operation.newBuilder()
                .setType(Protos.Offer.Operation.Type.RESERVE)
                .setReserve(Protos.Offer.Operation.Reserve.newBuilder().addResources(resourceSpec.getResource()))
                .build());
    }

    protected void unreserve(ResourceSpec resourceSpec) {
        getOperations().add(Protos.Offer.Operation.newBuilder()
                .setType(Protos.Offer.Operation.Type.UNRESERVE)
                .setReserve(Protos.Offer.Operation.Reserve.newBuilder().addResources(resourceSpec.getResource()))
                .build());
    }

    protected void unreserve(VolumeSpec volumeSpec) {
        Protos.Resource.Builder resourceBuilder = volumeSpec.getResource();
        getOperations().add(Protos.Offer.Operation.newBuilder()
                .setType(Protos.Offer.Operation.Type.DESTROY)
                .setDestroy(Protos.Offer.Operation.Destroy.newBuilder().addVolumes(resourceBuilder.build()))
                .build());

        if (resourceBuilder.hasDisk() && resourceBuilder.getDisk().hasSource()) {
            resourceBuilder.setDisk(
                    Protos.Resource.DiskInfo.newBuilder().setSource(resourceBuilder.getDisk().getSource()));
        } else {
            resourceBuilder.clearDisk().clearRevocable();
        }

        getOperations().add(Protos.Offer.Operation.newBuilder()
                .setType(Protos.Offer.Operation.Type.UNRESERVE)
                .setReserve(Protos.Offer.Operation.Reserve.newBuilder().addResources(resourceBuilder.build()))
                .build());
    }

    protected void create(VolumeSpec volumeSpec) {
        getOperations().add(Protos.Offer.Operation.newBuilder()
                .setType(Protos.Offer.Operation.Type.CREATE)
                .setCreate(Protos.Offer.Operation.Create.newBuilder().addVolumes(volumeSpec.getResource()))
                .build());
    }

    protected abstract void launch(TaskSpec taskSpec);

    protected abstract List<Protos.Offer.Operation> getOperations();

    public List<Protos.Offer.Operation> build() {
        return new ArrayList<>(getOperations());
    }
}
