package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.offer.evaluate.placement.PlacementRule;
import org.apache.mesos.Protos;

import java.util.Collection;

public class PlacementRulePreparer implements LaunchSetPreparer {
    private final PlacementRule placementRule;
    private final Collection<Protos.TaskInfo> serviceTasks;

    public PlacementRulePreparer(PlacementRule placementRule, Collection<Protos.TaskInfo> serviceTasks) {
        this.placementRule = placementRule;
        this.serviceTasks = serviceTasks;
    }

    @Override
    public LaunchSetBuilder prepare(LaunchSetBuilder launchSetBuilder) {
        if (placementRule == null) {
            return launchSetBuilder.pass(this, "No placement rule defined");
        }

        return placementRule.filter(
                mesosResourcePool.getOffer(),
                podInfoBuilder.getPodInstance(),
                deployedTasks);
    }

    @Override
    public LaunchSetPreparer fromFormerLaunch(Protos.Offer.Operation.LaunchGroup launchGroup) {
        return null;
    }

    @Override
    public OfferEvaluationStage getEvaluationStage() {
        return null;
    }
}
