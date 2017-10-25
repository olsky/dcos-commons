package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.specification.ResourceSpec;
import org.apache.mesos.Protos;

import java.util.Optional;

public interface ReservationCreator {

    Protos.Resource.Builder withReservation(
            ResourceSpec resourceSpec, Protos.Resource.Builder resourceBuilder, Optional<String> resourceId);

    default Protos.Resource.Builder withReservation(ResourceSpec resourceSpec, Optional<String> resourceId) {
        return withReservation(resourceSpec, resourceSpec.getResource(), resourceId);
    }

    Protos.Resource.Builder withResourceId(Protos.Resource.Builder resourceBuilder);
}
