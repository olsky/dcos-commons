package com.mesosphere.sdk.offer.evaluate;

import org.apache.mesos.Protos;

public interface ReservationCreator {
    Protos.Resource.ReservationInfo getReservation();
}
