package com.mesosphere.sdk.offer.evaluate;

import org.apache.mesos.Protos;

public interface ReservationPreparer {
    Protos.Resource getResource();
}
