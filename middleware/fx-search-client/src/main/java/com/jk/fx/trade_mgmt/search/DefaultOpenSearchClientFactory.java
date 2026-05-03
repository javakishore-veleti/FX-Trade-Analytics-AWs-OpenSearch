package com.jk.fx.trade_mgmt.search;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

@Slf4j
public class DefaultOpenSearchClientFactory implements OpenSearchClientFactory {

    private final BackendsSource source;
    private final AwsCredentialsProvider awsCredentialsProvider;
    private final Map<String, CachedClient> cache = new ConcurrentHashMap<>();

    /** Lazily-built shared SDK HTTP client for AWS-signed transports. */
    private volatile SdkHttpClient awsHttpClient;

    /** Snapshot of the backend used when the cached client was built; we
     *  rebuild lazily if the source ever returns a different one for the
     *  same region (e.g. masterdata sync swapped a domain endpoint). */
    private record CachedClient(OpenSearchBackend backend, OpenSearchClient client) {}

    /**
     * Back-compat constructor used by tests + services that still wire only
     * {@link OpenSearchBackendsProperties} directly. Falls back to
     * {@link DefaultCredentialsProvider} for AWS backends.
     */
    public DefaultOpenSearchClientFactory(OpenSearchBackendsProperties props) {
        this(new YamlBackendsSource(props), DefaultCredentialsProvider.create());
    }

    /** Back-compat: explicit credentials, YAML source. */
    public DefaultOpenSearchClientFactory(
            OpenSearchBackendsProperties props,
            AwsCredentialsProvider awsCredentialsProvider) {
        this(new YamlBackendsSource(props), awsCredentialsProvider);
    }

    /** Primary constructor — pluggable backend source + credentials. */
    public DefaultOpenSearchClientFactory(
            BackendsSource source,
            AwsCredentialsProvider awsCredentialsProvider) {
        this.source = source;
        this.awsCredentialsProvider = awsCredentialsProvider;
        log.info("OpenSearchClientFactory initialised with source={}", source.getClass().getSimpleName());
    }

    @Override
    public OpenSearchClient clientFor(String region) {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("OpenSearchClientFactory.clientFor: region is required");
        }
        OpenSearchBackend backend = lookup(region)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No OpenSearch backend configured for region '" + region
                                + "'. Source: " + source.getClass().getSimpleName()));
        CachedClient cached = cache.get(region);
        if (cached != null && cached.backend().equals(backend)) {
            return cached.client();
        }
        OpenSearchClient fresh = build(backend);
        cache.put(region, new CachedClient(backend, fresh));
        return fresh;
    }

    @Override
    public boolean hasRegion(String region) {
        return region != null && lookup(region).isPresent();
    }

    private Optional<OpenSearchBackend> lookup(String region) {
        return source.load().stream()
                .filter(b -> region.equals(b.getRegion()))
                .findFirst();
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
                .setCredentials(awsCredentialsProvider)
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
