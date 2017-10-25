package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.scheduler.plan.PodInstanceRequirement;
import com.mesosphere.sdk.specification.PodSpec;
import com.mesosphere.sdk.specification.PortSpec;
import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.TaskSpec;
import com.mesosphere.sdk.specification.VolumeSpec;

import java.util.Optional;

public interface SpecVisitor<T> {

    default PodInstanceRequirement visit(PodInstanceRequirement podInstanceRequirement) throws SpecVisitorException {
        PodInstanceRequirement visited = visitImplementation(podInstanceRequirement);

        if (getDelegate().isPresent()) {
            visited = getDelegate().get().visit(visited);
        }

        return visited;
    }

    PodInstanceRequirement visitImplementation(
            PodInstanceRequirement podInstanceRequirement) throws SpecVisitorException;

    default PodSpec visit(PodSpec podSpec) throws SpecVisitorException {
        PodSpec visited = visitImplementation(podSpec);

        if (getDelegate().isPresent()) {
            visited = getDelegate().get().visit(visited);
        }

        return visited;
    }

    PodSpec visitImplementation(PodSpec podSpec) throws SpecVisitorException;

    default TaskSpec visit(TaskSpec taskSpec) throws SpecVisitorException {
        TaskSpec visited = visitImplementation(taskSpec);

        if (getDelegate().isPresent()) {
            visited = getDelegate().get().visit(visited);
        }

        return visited;
    }

    TaskSpec visitImplementation(TaskSpec taskSpec) throws SpecVisitorException;

    default ResourceSpec visit(ResourceSpec resourceSpec) throws SpecVisitorException {
        ResourceSpec visited = visitImplementation(resourceSpec);

        if (getDelegate().isPresent()) {
            visited = getDelegate().get().visit(visited);
        }

        return visited;
    }

    ResourceSpec visitImplementation(ResourceSpec resourceSpec) throws SpecVisitorException;

    default VolumeSpec visit(VolumeSpec volumeSpec) throws SpecVisitorException {
        VolumeSpec visited = visitImplementation(volumeSpec);

        if (getDelegate().isPresent()) {
            visited = getDelegate().get().visit(visited);
        }

        return visited;
    }

    VolumeSpec visitImplementation(VolumeSpec volumeSpec) throws SpecVisitorException;

    default PortSpec visit(PortSpec portSpec) throws SpecVisitorException {
        PortSpec visited = visitImplementation(portSpec);

        if (getDelegate().isPresent()) {
            visited = getDelegate().get().visit(visited);
        }

        return visited;
    }

    PortSpec visitImplementation(PortSpec portSpec) throws SpecVisitorException;

    default PodSpec finalize(PodSpec podSpec) throws SpecVisitorException {
        PodSpec finalized = finalizeImplementation(podSpec);

        if (getDelegate().isPresent()) {
            finalized = getDelegate().get().finalize(finalized);
        }

        return finalized;
    }

    default PodSpec finalizeImplementation(PodSpec podSpec) throws SpecVisitorException {
        return podSpec;
    }

    default TaskSpec finalize(TaskSpec taskSpec) throws SpecVisitorException {
        TaskSpec finalized = finalizeImplementation(taskSpec);

        if (getDelegate().isPresent()) {
            finalized = getDelegate().get().finalize(finalized);
        }

        return finalized;
    }

    default TaskSpec finalizeImplementation(TaskSpec taskSpec) throws SpecVisitorException {
        return taskSpec;
    }

    default ResourceSpec finalize(ResourceSpec resourceSpec) throws SpecVisitorException {
        ResourceSpec finalized = finalizeImplementation(resourceSpec);

        if (getDelegate().isPresent()) {
            finalized = getDelegate().get().finalize(finalized);
        }

        return finalized;
    }

    default ResourceSpec finalizeImplementation(ResourceSpec resourceSpec) throws SpecVisitorException {
        return resourceSpec;
    }

    default VolumeSpec finalize(VolumeSpec volumeSpec) throws SpecVisitorException {
        VolumeSpec finalized = finalizeImplementation(volumeSpec);

        if (getDelegate().isPresent()) {
            finalized = getDelegate().get().finalize(finalized);
        }

        return finalized;
    }

    default VolumeSpec finalizeImplementation(VolumeSpec volumeSpec) throws SpecVisitorException {
        return volumeSpec;
    }

    default PortSpec finalize(PortSpec portSpec) throws SpecVisitorException {
        PortSpec finalized = finalizeImplementation(portSpec);

        if (getDelegate().isPresent()) {
            finalized = getDelegate().get().finalize(finalized);
        }

        return finalized;
    }

    default PortSpec finalizeImplementation(PortSpec portSpec) throws SpecVisitorException {
        return portSpec;
    }

    Optional<SpecVisitor> getDelegate();

    default void compileResult() {
        compileResultImplementation();

        Optional<SpecVisitor> delegate = getDelegate();
        if (delegate.isPresent()) {
            delegate.get().compileResult();
        }
    }

    void compileResultImplementation();

    default VisitorResultCollector<T> createVisitorResultCollector() {
        return new VisitorResultCollector<T>() {
            private T result;

            @Override
            public void setResult(T result) {
                this.result = result;
            }

            @Override
            public T getResult() {
                return result;
            }
        };
    }

    VisitorResultCollector<T> getVisitorResultCollector();
}
