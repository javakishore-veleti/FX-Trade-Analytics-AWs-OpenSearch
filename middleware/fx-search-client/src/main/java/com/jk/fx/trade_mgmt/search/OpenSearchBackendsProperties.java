package com.jk.fx.trade_mgmt.search;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Container for the list of {@link OpenSearchBackend}s declared under {@code fx.opensearch}. */
@Data
@ConfigurationProperties(prefix = "fx.opensearch")
public class OpenSearchBackendsProperties {
    private List<OpenSearchBackend> backends = new ArrayList<>();
}
