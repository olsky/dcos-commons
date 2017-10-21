package com.mesosphere.sdk.specification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.mesos.Protos;

import java.util.Collection;

/**
 * Created by mbrowning on 10/17/17.
 */
public interface PortSpec extends ResourceSpec {
    /**
     * Returns a copy of the provided {@link DefaultPortSpec} which has been updated to have the provided {@code value}.
     */
    @JsonIgnore
    static PortSpec withValue(PortSpec portSpec, Protos.Value value) {
        return new DefaultPortSpec(
                value,
                portSpec.getRole(),
                portSpec.getPreReservedRole(),
                portSpec.getPrincipal(),
                portSpec.getEnvKey(),
                portSpec.getPortName(),
                portSpec.getVisibility(),
                portSpec.getNetworkNames());
    }

    @JsonProperty("port-name")
    String getPortName();

    @JsonProperty("visibility")
    Protos.DiscoveryInfo.Visibility getVisibility();

    @JsonProperty("network-names")
    Collection<String> getNetworkNames();

    @JsonIgnore
    long getPort();
}
