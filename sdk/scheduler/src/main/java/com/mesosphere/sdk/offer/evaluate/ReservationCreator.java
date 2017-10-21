package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.specification.ResourceSpec;
import org.apache.mesos.Protos;

import java.util.Optional;

public interface ReservationCreator {

    Protos.Resource.Builder withReservation(ResourceSpec resourceSpec, Optional<String> resourceId);

    Protos.Resource.Builder withResourceId(Protos.Resource.Builder resourceBuilder);
}
