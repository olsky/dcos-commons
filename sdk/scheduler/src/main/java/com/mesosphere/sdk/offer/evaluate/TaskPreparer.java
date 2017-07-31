package com.mesosphere.sdk.offer.evaluate;

import org.apache.mesos.Protos;

public class TaskPreparer implements LaunchSetPreparer {
    private final boolean shouldLaunch;
    private final TaskCursor taskCursor;

    public TaskPreparer(boolean shouldLaunch, TaskCursor taskCursor) {
        this.shouldLaunch = shouldLaunch;
        this.taskCursor = taskCursor;
    }

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
}
