package com.mesosphere.sdk.specification;

import com.mesosphere.sdk.offer.evaluate.SpecVisitor;

public interface SpecVisitee {
    void accept(SpecVisitor visitor);
}
