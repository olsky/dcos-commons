package com.mesosphere.sdk.specification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.mesosphere.sdk.offer.evaluate.VolumeCreator;

/**
 * A VolumeSpec defines the features of a Volume.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public interface VolumeSpec extends ResourceSpec, VolumeCreator {

    /**
     * Types of Volumes.
     */
    enum Type {
        ROOT,
        PATH,
        MOUNT
    }

    @JsonProperty("type")
    Type getType();

    @JsonProperty("container-path")
    String getContainerPath();
}
