package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.offer.ValueUtils;
import com.mesosphere.sdk.specification.DefaultResourceSpec;
import com.mesosphere.sdk.specification.PodSpec;
import com.mesosphere.sdk.specification.PortSpec;
import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.TaskSpec;
import com.mesosphere.sdk.specification.VolumeSpec;
import org.apache.mesos.Protos;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExistingSpecVisitor extends OperationBuilder {
    private final OperationBuilder delegate;
    private final PodSpec existingPodSpec;
    private Map<String, ResourceSpec> existingResources;

    public ExistingSpecVisitor(OperationBuilder delegate, PodSpec existingPodSpec) {
        this.delegate = delegate;
        this.existingPodSpec = existingPodSpec;
    }

    @Override
    public void visit(PodSpec podSpec) {
        delegate.visit(podSpec);
    }

    @Override
    public void visit(TaskSpec taskSpec) {
        Optional<TaskSpec> existingTask = existingPodSpec.getTasks().stream()
                .filter(t -> t.getName().equals(taskSpec.getName()))
                .findAny();

        if (existingTask.isPresent()) {
            existingResources = existingTask.get().getResourceSet().getResources().stream()
                    .collect(Collectors.toMap(r -> resourceName(r), Function.identity()));
            existingResources.putAll(existingTask.get().getResourceSet().getVolumes().stream()
                    .collect(Collectors.toMap(v -> resourceName(v), Function.identity())));
        }
        delegate.visit(taskSpec);
    }

    @Override
    public void visit(ResourceSpec resourceSpec) {
        String resourceName = resourceName(resourceSpec);
        Optional<ResourceSpec> existingResource = Optional.ofNullable(existingResources.get(resourceName));

        if (existingResource.isPresent()) {
            Protos.Value unusedResource = ValueUtils.subtract(
                    existingResource.get().getValue(), resourceSpec.getValue());
            Protos.Value extraResource = ValueUtils.subtract(
                    resourceSpec.getValue(), existingResource.get().getValue());

            // Layer the operation creator doodad too; visitors can just create launch groups;
            // Then a operation builder does the right thing here.
            if (ValueUtils.isPositive(unusedResource)) {
                // Create UNRESERVE.
                delegate.unreserve(withValue(resourceSpec, unusedResource));
            } else if (ValueUtils.isPositive(extraResource)) {
                // Create RESERVE.
                delegate.reserve(withValue(resourceSpec, extraResource));
            }

            existingResources.remove(resourceName);
            // Build the merged resource spec, *with* reservation.
            delegate.visit(resourceSpec);
        } else {
            delegate.visit(resourceSpec);
        }
    }

    private static ResourceSpec withValue(ResourceSpec resourceSpec, Protos.Value value) {
        return DefaultResourceSpec.newBuilder(resourceSpec).value(value).build();
    }

    @Override
    public void visit(VolumeSpec volumeSpec) {
        String resourceName = resourceName(volumeSpec);
        // Unfortunately, the type system can't do the work for us here, since we don't want to achieve polymorphism by
        // attaching behavior to Specs. This is the safest place to do it and lets us override the reserve()/unreserve()
        // methods in OperationBuilder rather than conducting volume handling separately from other resources.
        Optional<VolumeSpec> existingVolume = Optional.ofNullable((VolumeSpec) existingResources.get(resourceName));

        if (existingVolume.isPresent()) {
            Protos.Value unusedResource = ValueUtils.subtract(
                    existingVolume.get().getValue(), volumeSpec.getValue());
            Protos.Value extraResource = ValueUtils.subtract(
                    volumeSpec.getValue(), existingVolume.get().getValue());

            if (ValueUtils.isPositive(unusedResource) || ValueUtils.isPositive(extraResource)) {
                // Can't do!
            } else {
                // Build reservation volume spec.
                // TODO(mrb): is this wrong?
                delegate.visit(volumeSpec);
            }
            existingResources.remove(resourceName);
        } else {
            delegate.visit(volumeSpec);
        }

    }

    @Override
    public void visit(PortSpec portSpec) {

    }

    @Override
    public void finalize(PodSpec podSpec) {
        // Presumably won't need to unreserve executor resources the same way...?
    }

    @Override
    public void finalize(TaskSpec taskSpec) {
        Optional<TaskSpec> existingTask = existingPodSpec.getTasks().stream()
                .filter(t -> t.getName().equals(taskSpec.getName()))
                .findAny();

        if (existingTask.isPresent()) {
            for (ResourceSpec r : existingTask.get().getResourceSet().getResources()) {
                if (existingResources.containsKey(resourceName(r))) {
                    unreserve(r);
                }
            }

            for (VolumeSpec v : existingTask.get().getResourceSet().getVolumes()) {
                if (existingResources.containsKey(resourceName(v))) {
                    unreserve(v);
                }
            }
        }
    }

    @Override
    public void finalize(ResourceSpec resourceSpec) {

    }

    @Override
    public void finalize(VolumeSpec volumeSpec) {

    }

    @Override
    public void finalize(PortSpec portSpec) {

    }

    private String resourceName(ResourceSpec resourceSpec) {
        return resourceSpec.getName();
    }

    private String resourceName(VolumeSpec volumeSpec) {
        return volumeSpec.getName() + "_" + volumeSpec.getContainerPath();
    }

    @Override
    protected void launch(TaskSpec taskSpec) {

    }

    @Override
    protected List<Protos.Offer.Operation> getOperations() {
        return delegate.getOperations();
    }
}
