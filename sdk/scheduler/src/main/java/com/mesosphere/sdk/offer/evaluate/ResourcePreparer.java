package com.mesosphere.sdk.offer.evaluate;

public abstract class ResourcePreparer implements LaunchSetPreparer {

    protected abstract LaunchSetBuilder performExtraCleanup(
            LaunchSetBuilder launchSetBuilder, LaunchedPodEntity launchedPodEntity);

    public LaunchSetBuilder prepareResourceCleanup(
            LaunchSetBuilder launchSetBuilder, LaunchedPodEntity launchedPodEntity) {
        performExtraCleanup(launchSetBuilder, launchedPodEntity);
        return launchSetBuilder;
    }
}
