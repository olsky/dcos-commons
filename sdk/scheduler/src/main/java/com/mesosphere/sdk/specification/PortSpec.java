package com.mesosphere.sdk.specification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.mesos.Protos;

import java.util.Collection;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
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
                portSpec.getEnvKey().get(),
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
