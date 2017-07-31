package com.mesosphere.sdk.offer.evaluate;

import org.apache.mesos.Protos;

public interface ResourceAppender {
    PodInfoBuilder appendResource(PodInfoBuilder podInfoBuilder);

    Protos.Resource getResource();
}
