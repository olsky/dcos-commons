package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.offer.Constants;
import com.mesosphere.sdk.specification.ResourceSpec;
import org.apache.mesos.Protos;

// TODO(mrb): Some sort of override layer to remove the role from the resource itself? That seems
// A little brittle, like maybe this should be handled via composition...so many goddamn axes
// to account for with this shit, ugh.
public interface ResourceCreator {

    ResourceSpec getResourceSpec();

    default Protos.Resource.Builder getResource() {
        ResourceSpec resourceSpec = getResourceSpec();
        Protos.Resource.Builder builder = Protos.Resource.newBuilder();

        Protos.Value resourceValue = resourceSpec.getValue();
        builder.setName(resourceSpec.getName()).setType(resourceValue.getType());

        switch (resourceValue.getType()) {
            case SCALAR:
                builder.setScalar(resourceValue.getScalar());
                break;
            case RANGES:
                builder.setRanges(resourceValue.getRanges());
            case SET:
                builder.setSet(resourceValue.getSet());
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported spec value type: %s", resourceValue.getType()));
        }

        return builder;
    }
}
