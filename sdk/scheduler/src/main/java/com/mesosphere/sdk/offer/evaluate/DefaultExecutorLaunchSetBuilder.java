package com.mesosphere.sdk.offer.evaluate;

import org.apache.mesos.Protos;

import java.util.HashMap;
import java.util.Map;

public class DefaultExecutorLaunchSetBuilder extends LaunchSetBuilder {
    private final Map<String, Protos.TaskInfo.Builder> tasks;
    private final Protos.Offer.Operation.LaunchGroup.Builder launchGroup;

    public DefaultExecutorLaunchSetBuilder() {
        tasks = new HashMap<>();
        launchGroup = Protos.Offer.Operation.LaunchGroup.newBuilder();
    }

    @Override
    public LaunchSetBuilder createTaskResource(String taskName, ResourceCreator resourceCreator) {
        tasks.get();
        return null;
    }

    @Override
    public LaunchSetBuilder createExecutorResource(ResourceCreator resourceCreator) {
        return null;
    }

    @Override
    public long getUnassignedPort(Protos.Offer offer) {
        return 0;
    }
}
