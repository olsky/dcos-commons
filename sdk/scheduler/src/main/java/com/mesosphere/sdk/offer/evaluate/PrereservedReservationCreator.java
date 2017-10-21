package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.offer.taskdata.AuxLabelAccess;
import com.mesosphere.sdk.specification.ResourceSpec;
import org.apache.mesos.Protos;

import java.util.UUID;

public class PrereservedReservationCreator implements ReservationCreator {
    private final ResourceCreator delegate;
    private final String resourceId;

    public PrereservedReservationCreator(ResourceCreator delegate, String resourceId) {
        this.delegate = delegate;
        this.resourceId = resourceId == null ? UUID.randomUUID().toString() : resourceId;
    }

    @Override
    public ResourceSpec getResourceSpec() {
        return delegate.getResourceSpec();
    }

    @Override
    public Protos.Resource.Builder getResource() {
        ResourceSpec resourceSpec = getResourceSpec();
        Protos.Resource.Builder builder = delegate.getResource();

        Protos.Resource.ReservationInfo.Builder reservationBuilder = builder.addReservationsBuilder()
                .setRole(resourceSpec.getRole())
                .setType(Protos.Resource.ReservationInfo.Type.DYNAMIC)
                .setPrincipal(resourceSpec.getPrincipal());
        AuxLabelAccess.setResourceId(reservationBuilder, resourceId);

        return builder;
    }
}
