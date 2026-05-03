package com.jk.fx.trade_mgmt.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link BackendsSource} backed by fx-masterdata-service's
 * {@code GET /api/admin/opensearch-deployments} endpoint. Filters to rows
 * with {@code status=ACTIVE} that carry a non-blank {@code endpoint}, and
 * maps {@code provisionType=managed} → {@code provider=aws} (the search
 * client's {@code aws} transport works for both managed domains and
 * eventually serverless once we set the SigV4 service name dynamically).
 *
 * <p>Memoised with a TTL — the masterdata endpoint is cheap but called per
 * search request would be wasteful. On any failure (DNS, timeout, 5xx) the
 * last known good snapshot is returned so a transient masterdata outage
 * doesn't kill the search/indexer paths.
 */
@Slf4j
public class MasterDataBackendsSource implements BackendsSource {

    private final BackendsSourceProperties props;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicReference<List<OpenSearchBackend>> cache = new AtomicReference<>(List.of());
    private volatile Instant lastFetchedAt = Instant.EPOCH;

    public MasterDataBackendsSource(BackendsSourceProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getRequestTimeoutMillis()))
                .build();
    }

    @Override
    public Collection<OpenSearchBackend> load() {
        Instant now = Instant.now();
        if (now.minusSeconds(props.getTtlSeconds()).isBefore(lastFetchedAt)) {
            return cache.get();
        }
        try {
            List<OpenSearchBackend> fresh = fetchOnce();
            cache.set(fresh);
            lastFetchedAt = now;
            return fresh;
        } catch (Exception ex) {
            log.warn("MasterDataBackendsSource: refresh failed ({}); returning {} cached entries",
                    ex.getMessage(), cache.get().size());
            return cache.get();
        }
    }

    private List<OpenSearchBackend> fetchOnce() throws Exception {
        URI uri = URI.create(props.getMasterdataUrl().replaceAll("/+$", "")
                + "/api/admin/opensearch-deployments");
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(props.getRequestTimeoutMillis()))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + " from " + uri);
        }
        JsonNode root = mapper.readTree(resp.body());
        if (!root.isArray()) {
            throw new IllegalStateException("Expected JSON array from " + uri + "; got " + root.getNodeType());
        }

        List<OpenSearchBackend> out = new ArrayList<>(root.size());
        for (JsonNode row : root) {
            String status = text(row, "status");
            String endpoint = text(row, "endpoint");
            String region = text(row, "region");
            String provisionType = text(row, "provisionType");
            if (!"ACTIVE".equals(status) || endpoint == null || endpoint.isBlank() || region == null) {
                continue;
            }
            // managed → AWS SigV4 ('es' service name); local stays local; serverless
            // would need the 'aoss' service name — DefaultOpenSearchClientFactory
            // currently hardcodes 'es' so serverless is intentionally skipped here.
            OpenSearchBackend.Provider provider = switch (provisionType) {
                case "managed"     -> OpenSearchBackend.Provider.aws;
                case "local-docker"-> OpenSearchBackend.Provider.local;
                default            -> null;
            };
            if (provider == null) continue;
            out.add(new OpenSearchBackend(region, provider, endpoint));
        }
        log.info("MasterDataBackendsSource: refreshed — {} backend(s) from {}", out.size(), uri);
        return List.copyOf(out);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
