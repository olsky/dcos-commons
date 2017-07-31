package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.offer.MesosResourcePool;
import com.mesosphere.sdk.specification.ResourceSpec;
import org.apache.mesos.Protos;

public class PortCreator implements ResourceCreator {
    private final ResourceSpec resourceSpec;
    private final MesosResourcePool mesosResourcePool;

    public PortCreator(ResourceSpec resourceSpec, MesosResourcePool mesosResourcePool) {
        this.resourceSpec = assignPort(resourceSpec);
        this.mesosResourcePool = mesosResourcePool;
    }

    private ResourceSpec assignPort(ResourceSpec resourceSpec) {
        // TODO(mrb): Need to check task info ports and existing ports here too :/
        int port = mesosResourcePool.getUnreservedPort();

        return new ResourceSpec() {
            @Override
            public Protos.Value getValue() {
                Protos.Value.Builder value = resourceSpec.getValue().toBuilder();

                value.getRangesBuilder().clear().addRangeBuilder().setBegin(port).setEnd(port);
                return value.build();
            }

            @Override
            public String getName() {
                return resourceSpec.getName();
            }

            @Override
            public String getRole() {
                return resourceSpec.getRole();
            }

            @Override
            public String getPreReservedRole() {
                return resourceSpec.getPreReservedRole();
            }

            @Override
            public String getPrincipal() {
                return resourceSpec.getPrincipal();
            }

            @Override
            public ResourceSpec getResourceSpec() {
                return this;
            }
        };
    }

    @Override
    public ResourceSpec getResourceSpec() {
        return resourceSpec;
    }

    @Override
    public Protos.Resource.Builder getResource() {
        return null;
    }
}
