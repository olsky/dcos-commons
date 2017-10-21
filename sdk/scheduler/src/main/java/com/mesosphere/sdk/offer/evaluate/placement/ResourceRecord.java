package com.mesosphere.sdk.offer.evaluate.placement;

import com.mesosphere.sdk.specification.ResourceSpec;
import org.apache.mesos.Protos;

import java.util.Optional;

public class ResourceRecord implements ResourceSpec {
    private final ResourceSpec delegate;
    private final String resourceId;

    public ResourceRecord(ResourceSpec delegate, String resourceId) {
        this.delegate = delegate;
        this.resourceId = resourceId;
    }

    @Override
    public ResourceSpec getResourceSpec() {
        return delegate.getResourceSpec();
    }

    @Override
    public Protos.Value getValue() {
        return delegate.getValue();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getRole() {
        return delegate.getRole();
    }

    @Override
    public String getPreReservedRole() {
        return delegate.getPreReservedRole();
    }

    @Override
    public String getPrincipal() {
        return delegate.getPrincipal();
    }

    public Optional<String> getResourceId() {
        return Optional.ofNullable(resourceId);
    }
}
