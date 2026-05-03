package com.jk.fx.trade_mgmt.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * Auto-wires the OpenSearch client factory in any consuming Spring Boot
 * service. Three services depend on this module today:
 *   - fx-trade-service        (search path)
 *   - fx-opensearch-indexer   (write path)
 *   - fx-masterdata-service   (admin sync — optional, also has its own
 *                              AwsClientsConfig for control-plane SDK clients)
 *
 * <p>Each service's @SpringBootApplication scans its own package; this class
 * lives in {@code com.jk.fx.trade_mgmt.search} which is under all services'
 * base scan package ({@code com.jk.fx.trade_mgmt}), so component-scan picks
 * it up automatically. No explicit {@code @Import} required.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({
        OpenSearchBackendsProperties.class,
        AwsCredentialsProperties.class,
        BackendsSourceProperties.class
})
public class SearchClientAutoConfiguration {

    /**
     * AWS credentials for OpenSearch SigV4 signing. Static keys from
     * {@code fx.aws.*} take precedence; otherwise the SDK default chain
     * (env vars → ~/.aws/credentials → ECS task role → instance profile).
     *
     * <p>Marked {@code @ConditionalOnMissingBean} so a service that already
     * defines its own {@code AwsCredentialsProvider} bean (e.g. masterdata's
     * {@code AwsClientsConfig}) wins — both code paths then share one
     * provider and one credentials decision.
     */
    @Bean
    @ConditionalOnMissingBean(AwsCredentialsProvider.class)
    public AwsCredentialsProvider fxAwsCredentialsProvider(AwsCredentialsProperties props) {
        if (props.hasStatic()) {
            log.info("fx-search-client: using static AWS credentials from fx.aws.access-key");
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
        }
        log.info("fx-search-client: fx.aws.access-key not set — using DefaultCredentialsProvider chain");
        return DefaultCredentialsProvider.create();
    }

    /**
     * Backend resolution strategy. Default {@code yaml} reads
     * {@code fx.opensearch.backends} from {@code application.yml};
     * {@code masterdata} hits the masterdata REST endpoint and refreshes
     * every {@code fx.opensearch.source.ttl-seconds} seconds.
     */
    @Bean
    @ConditionalOnMissingBean(BackendsSource.class)
    public BackendsSource backendsSource(
            BackendsSourceProperties sourceProps,
            OpenSearchBackendsProperties yamlProps) {
        return switch (sourceProps.getType()) {
            case masterdata -> {
                log.info("fx-search-client: BackendsSource = masterdata ({}), ttl={}s",
                        sourceProps.getMasterdataUrl(), sourceProps.getTtlSeconds());
                yield new MasterDataBackendsSource(sourceProps);
            }
            case yaml -> {
                int n = yamlProps.getBackends() == null ? 0 : yamlProps.getBackends().size();
                log.info("fx-search-client: BackendsSource = yaml ({} entries from fx.opensearch.backends)", n);
                yield new YamlBackendsSource(yamlProps);
            }
        };
    }

    @Bean
    public OpenSearchClientFactory openSearchClientFactory(
            BackendsSource backendsSource,
            AwsCredentialsProvider awsCredentialsProvider) {
        return new DefaultOpenSearchClientFactory(backendsSource, awsCredentialsProvider);
    }
}
