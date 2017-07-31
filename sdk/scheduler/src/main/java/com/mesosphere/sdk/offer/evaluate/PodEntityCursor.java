package com.mesosphere.sdk.offer.evaluate;

public interface PodEntityCursor {

    PodEntityCursor with(LaunchSetBuilder launchSetBuilder);

    boolean hasRunningExecutor();

    void appendResource(ResourceCreator resourceCreator);
}
