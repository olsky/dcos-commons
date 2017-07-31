package com.mesosphere.sdk.offer.evaluate.placement;

import com.mesosphere.sdk.offer.evaluate.LaunchSetBuilder;
import com.mesosphere.sdk.offer.evaluate.LaunchSetPreparer;
import com.mesosphere.sdk.offer.evaluate.PodEntityCursor;
import com.mesosphere.sdk.offer.evaluate.VolumePreparer;
import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.ServiceSpec;
import com.mesosphere.sdk.specification.VolumeSpec;
import org.apache.mesos.Protos;

import java.util.Collection;
import java.util.Optional;

public interface LaunchSetFactory {

    Collection<ResourceSpec> getExecutorResources();

    LaunchSetPreparer getResourcePreparer(ResourceSpec resourceSpec, PodEntityCursor podEntityCursor);

    LaunchSetBuilder getLaunchSetBuilder(Optional<ServiceSpec> lastServiceSpec, Protos.Offer offer);

    VolumePreparer getVolumePreparer(VolumeSpec volumeSpec, PodEntityCursor podEntityCursor);
}
