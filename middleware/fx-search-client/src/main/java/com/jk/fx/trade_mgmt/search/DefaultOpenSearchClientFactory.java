package com.jk.fx.trade_mgmt.search;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

@Slf4j
public class DefaultOpenSearchClientFactory implements OpenSearchClientFactory {

    private final Map<String, OpenSearchBackend> byRegion;
    private final Map<String, OpenSearchClient> cache = new ConcurrentHashMap<>();

    /** Lazily-built shared SDK HTTP client for AWS-signed transports. */
    private volatile SdkHttpClient awsHttpClient;

    public DefaultOpenSearchClientFactory(OpenSearchBackendsProperties props) {
        if (props == null || props.getBackends() == null || props.getBackends().isEmpty()) {
            log.warn("No fx.opensearch.backends configured — OpenSearchClientFactory will reject every clientFor() call.");
            this.byRegion = Map.of();
        } else {
            this.byRegion = new ConcurrentHashMap<>();
            for (OpenSearchBackend b : props.getBackends()) {
                if (b.getRegion() == null || b.getRegion().isBlank()) {
                    throw new IllegalStateException("fx.opensearch.backends entry missing region: " + b);
                }
                if (b.getEndpoint() == null || b.getEndpoint().isBlank()) {
                    throw new IllegalStateException("fx.opensearch.backends[" + b.getRegion() + "] missing endpoint");
                }
                if (b.getProvider() == null) {
                    throw new IllegalStateException("fx.opensearch.backends[" + b.getRegion() + "] missing provider (local|aws)");
                }
                this.byRegion.put(b.getRegion(), b);
            }
            log.info("OpenSearchClientFactory initialised with {} backend(s): {}",
                    byRegion.size(), byRegion.keySet());
        }
    }

    @Override
    public OpenSearchClient clientFor(String region) {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("OpenSearchClientFactory.clientFor: region is required");
        }
        OpenSearchBackend backend = byRegion.get(region);
        if (backend == null) {
            throw new IllegalArgumentException(
                    "No OpenSearch backend configured for region '" + region + "'. Configured: " + byRegion.keySet());
        }
        return cache.computeIfAbsent(region, r -> build(backend));
    }

    @Override
    public boolean hasRegion(String region) {
        return region != null && byRegion.containsKey(region);
    }

    private OpenSearchClient build(OpenSearchBackend backend) {
        OpenSearchTransport transport = switch (backend.getProvider()) {
            case local -> buildLocalTransport(backend);
            case aws   -> buildAwsTransport(backend);
        };
        log.info("Built OpenSearch client for region={} provider={} endpoint={}",
                backend.getRegion(), backend.getProvider(), backend.getEndpoint());
        return new OpenSearchClient(transport);
    }

    private OpenSearchTransport buildLocalTransport(OpenSearchBackend backend) {
        try {
            HttpHost host = HttpHost.create(backend.getEndpoint());
            return ApacheHttpClient5TransportBuilder.builder(host).build();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(
                    "Invalid local OpenSearch endpoint: " + backend.getEndpoint(), e);
        }
    }

    private OpenSearchTransport buildAwsTransport(OpenSearchBackend backend) {
        URI uri = URI.create(backend.getEndpoint());
        String hostOnly = uri.getHost();
        if (hostOnly == null) {
            throw new IllegalStateException(
                    "AWS OpenSearch endpoint must be a full URL (https://host[:port]); got: " + backend.getEndpoint());
        }
        SdkHttpClient http = ensureAwsHttpClient();
        AwsSdk2TransportOptions opts = AwsSdk2TransportOptions.builder()
                .setCredentials(DefaultCredentialsProvider.create())
                .build();
        return new AwsSdk2Transport(http, hostOnly, "es", Region.of(backend.getRegion()), opts);
    }

    private SdkHttpClient ensureAwsHttpClient() {
        SdkHttpClient local = awsHttpClient;
        if (local == null) {
            synchronized (this) {
                local = awsHttpClient;
                if (local == null) {
                    local = ApacheHttpClient.builder().build();
                    awsHttpClient = local;
                }
            }
        }
        return local;
    }
}
