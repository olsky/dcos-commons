package com.mesosphere.sdk.offer.evaluate;

public class TaskCursor implements PodEntityCursor {
    private final String taskName;

    private LaunchSetBuilder launchSetBuilder;

    public TaskCursor(String taskName) {
        this.taskName = taskName;
    }

    @Override
    public PodEntityCursor with(LaunchSetBuilder launchSetBuilder) {
        this.launchSetBuilder = launchSetBuilder;

        return this;
    }

    @Override
    public boolean hasRunningExecutor() {
        return launchSetBuilder.hasRunningExecutor();
    }

    @Override
    public void appendResource(ResourceCreator resourceCreator) {
        launchSetBuilder.createTaskResource(taskName, resourceCreator);
    }
}
