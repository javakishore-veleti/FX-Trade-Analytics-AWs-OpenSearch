package com.jk.fx.trade_mgmt.search;

import java.util.Collection;
import java.util.List;

/**
 * Default {@link BackendsSource} — returns whatever was bound from
 * {@code fx.opensearch.backends} at startup. The list is immutable for the
 * lifetime of the JVM; restart the service to pick up changes.
 */
public class YamlBackendsSource implements BackendsSource {

    private final List<OpenSearchBackend> snapshot;

    public YamlBackendsSource(OpenSearchBackendsProperties props) {
        this.snapshot = props.getBackends() == null
                ? List.of()
                : List.copyOf(props.getBackends());
    }

    @Override
    public Collection<OpenSearchBackend> load() {
        return snapshot;
    }
}
