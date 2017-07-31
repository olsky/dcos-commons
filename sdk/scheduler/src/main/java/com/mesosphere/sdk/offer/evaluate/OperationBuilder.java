package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.TaskSpec;
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
        // TODO(mrb): need to create unreserve volume spec
        getOperations().add(Protos.Offer.Operation.newBuilder()
                .setType(Protos.Offer.Operation.Type.UNRESERVE)
                .setReserve(Protos.Offer.Operation.Reserve.newBuilder().addResources(resourceSpec.getResource()))
                .build());
    }

    protected abstract void launch(TaskSpec taskSpec);

    protected abstract List<Protos.Offer.Operation> getOperations();

    public List<Protos.Offer.Operation> build() {
        return new ArrayList<>(getOperations());
    }
}
