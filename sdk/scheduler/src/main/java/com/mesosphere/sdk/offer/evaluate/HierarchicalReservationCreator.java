package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.offer.taskdata.AuxLabelAccess;
import com.mesosphere.sdk.specification.ResourceSpec;
import org.apache.mesos.Protos;

import java.util.Optional;
import java.util.UUID;

public class HierarchicalReservationCreator implements ReservationCreator {

    @Override
    public Protos.Resource.Builder withReservation(ResourceSpec resourceSpec, Optional<String> resourceId) {
        Protos.Resource.Builder resourceBuilder = resourceSpec.getResource();
        Protos.Resource.ReservationInfo.Builder reservationBuilder =
                Protos.Resource.ReservationInfo.newBuilder()
                        .setRole(resourceSpec.getRole())
                        .setType(Protos.Resource.ReservationInfo.Type.DYNAMIC)
                        .setPrincipal(resourceSpec.getPrincipal());

        if (resourceId.isPresent()) {
            AuxLabelAccess.setResourceId(reservationBuilder, resourceId.get());
        }
        // TODO(mrb): deal with ANY_ROLE stuff...
        resourceBuilder.addReservations(reservationBuilder);

        return resourceBuilder;
    }

    @Override
    public Protos.Resource.Builder withResourceId(Protos.Resource.Builder resourceBuilder) {
        Optional<String> resourceId = AuxLabelAccess.getResourceId(resourceBuilder.getReservations(0));

        if (!resourceId.isPresent()) {
            AuxLabelAccess.setResourceId(resourceBuilder.getReservationsBuilder(0), UUID.randomUUID().toString());
        }

        return resourceBuilder;
    }
}
