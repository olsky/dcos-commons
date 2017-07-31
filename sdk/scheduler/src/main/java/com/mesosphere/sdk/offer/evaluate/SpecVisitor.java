package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.specification.PodSpec;
import com.mesosphere.sdk.specification.PortSpec;
import com.mesosphere.sdk.specification.ResourceSpec;
import com.mesosphere.sdk.specification.TaskSpec;
import com.mesosphere.sdk.specification.VolumeSpec;

public interface SpecVisitor {

    void visit(PodSpec podSpec);

    void visit(TaskSpec taskSpec);

    void visit(ResourceSpec resourceSpec);

    void visit(VolumeSpec volumeSpec);

    void visit(PortSpec portSpec);

    default void finalize(PodSpec podSpec) { }

    default void finalize(TaskSpec taskSpec) { }

    default void finalize(ResourceSpec resourceSpec) { }

    default void finalize(VolumeSpec volumeSpec) { }

    default void finalize(PortSpec portSpec) { }
}
