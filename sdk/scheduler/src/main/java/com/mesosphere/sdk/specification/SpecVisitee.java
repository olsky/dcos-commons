package com.mesosphere.sdk.specification;

import com.mesosphere.sdk.offer.evaluate.SpecVisitor;
import com.mesosphere.sdk.offer.evaluate.SpecVisitorException;

public interface SpecVisitee {
    void accept(SpecVisitor visitor) throws SpecVisitorException;
}
