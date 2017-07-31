package com.mesosphere.sdk.offer.evaluate;

import org.apache.mesos.Protos;

public abstract class LaunchBuilder implements SpecVisitor {

    public abstract Protos.Offer.Operation getLaunch();
}
