package com.mesosphere.sdk.offer.evaluate;

import org.apache.mesos.Protos;

public interface LaunchSetPreparer {

    LaunchSetBuilder prepare(LaunchSetBuilder launchSetBuilder);

    LaunchSetPreparer fromFormerLaunch(Protos.Offer.Operation.LaunchGroup launchGroup);

    OfferEvaluationStage getEvaluationStage();
}
