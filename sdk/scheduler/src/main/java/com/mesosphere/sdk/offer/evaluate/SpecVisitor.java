package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.scheduler.plan.PodInstanceRequirement;
import com.mesosphere.sdk.specification.PodSpec;
import com.mesosphere.sdk.specification.PortSpec;
import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.TaskSpec;
import com.mesosphere.sdk.specification.VolumeSpec;

import java.util.Optional;

public interface SpecVisitor<T> {

    default void visit(PodInstanceRequirement podInstanceRequirement) throws SpecVisitorException {
        PodInstanceRequirement visited = visitImplementation(podInstanceRequirement);
        visit(podInstanceRequirement.getPodInstance().getPod());

        if (getDelegate().isPresent()) {
            getDelegate().get().visit(visited);
        }
    }

    PodInstanceRequirement visitImplementation(
            PodInstanceRequirement podInstanceRequirement) throws SpecVisitorException;

    default void visit(PodSpec podSpec) throws SpecVisitorException {
        PodSpec visited = visitImplementation(podSpec);

        if (getDelegate().isPresent()) {
            getDelegate().get().visit(visited);
        }
    }

    PodSpec visitImplementation(PodSpec podSpec) throws SpecVisitorException;

    default void visit(TaskSpec taskSpec) throws SpecVisitorException {
        TaskSpec visited = visitImplementation(taskSpec);

        if (getDelegate().isPresent()) {
            getDelegate().get().visit(visited);
        }
    }

    TaskSpec visitImplementation(TaskSpec taskSpec) throws SpecVisitorException;

    default void visit(ResourceSpec resourceSpec) throws SpecVisitorException {
        ResourceSpec visited = visitImplementation(resourceSpec);

        if (getDelegate().isPresent()) {
            getDelegate().get().visit(visited);
        }
    }

    ResourceSpec visitImplementation(ResourceSpec resourceSpec) throws SpecVisitorException;

    default void visit(VolumeSpec volumeSpec) throws SpecVisitorException {
        VolumeSpec visited = visitImplementation(volumeSpec);

        if (getDelegate().isPresent()) {
            getDelegate().get().visit(visited);
        }
    }

    VolumeSpec visitImplementation(VolumeSpec volumeSpec) throws SpecVisitorException;

    default void visit(PortSpec portSpec) throws SpecVisitorException {
        PortSpec visited = visitImplementation(portSpec);

        if (getDelegate().isPresent()) {
            getDelegate().get().visit(visited);
        }
    }

    PortSpec visitImplementation(PortSpec portSpec) throws SpecVisitorException;

    default void finalize(PodSpec podSpec) throws SpecVisitorException { }

    default void finalize(TaskSpec taskSpec) throws SpecVisitorException { }

    default void finalize(ResourceSpec resourceSpec) throws SpecVisitorException { }

    default void finalize(VolumeSpec volumeSpec) throws SpecVisitorException { }

    default void finalize(PortSpec portSpec) throws SpecVisitorException { }

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
