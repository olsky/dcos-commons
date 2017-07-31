package com.mesosphere.sdk.offer.evaluate;

public class ExecutorCursor implements PodEntityCursor {
    private LaunchSetBuilder launchSetBuilder;

    public ExecutorCursor with(LaunchSetBuilder launchSetBuilder) {
        this.launchSetBuilder = launchSetBuilder;

        return this;
    }

    @Override
    public LaunchSetBuilder appendResource(ResourceCreator resourceCreator) {
        return launchSetBuilder.createExecutorResource(resourceCreator);
    }
}
