package com.mesosphere.sdk.offer;

import org.apache.mesos.Protos;

public interface TaskLaunchCreator {

    Protos.Offer.Operation getTaskLaunchOperation(
}
