package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.specification.ResourceSpec;
import org.apache.mesos.Protos;

public class PortPreparer extends ResourcePreparer implements ResourceCreator {
    // I believe the dynamic port type logic can be sufficiently captured here -- the evaluation stage asks the
    // ask launch set builder to assign port?

    @Override
    public LaunchSetBuilder prepare(LaunchSetBuilder launchSetBuilder) {
        return null;
    }

    @Override
    public LaunchSetPreparer fromFormerLaunch(Protos.Offer.Operation.LaunchGroup launchGroup) {
        return null;
    }

    @Override
    public OfferEvaluationStage getEvaluationStage() {
        return null;
    }

    @Override
    protected LaunchSetBuilder performExtraCleanup(LaunchSetBuilder launchSetBuilder, LaunchedPodEntity launchedPodEntity) {
        return null;
    }

    @Override
    public ResourceSpec getResourceSpec() {
        return null;
    }
}
