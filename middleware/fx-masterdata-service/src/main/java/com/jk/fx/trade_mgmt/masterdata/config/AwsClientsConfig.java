package com.jk.fx.trade_mgmt.masterdata.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * AWS credential + region configuration used by the OpenSearch deployment
 * sync feature.
 *
 * <p>Credential resolution:
 *   <ol>
 *     <li>If {@code fx.aws.access-key} and {@code fx.aws.secret-key} are set
 *         (typically via the gitignored {@code application-local-secrets.yml}),
 *         use them via {@link StaticCredentialsProvider}.</li>
 *     <li>Otherwise fall back to the AWS SDK's
 *         {@link DefaultCredentialsProvider} chain (env vars,
 *         {@code ~/.aws/credentials} profile, ECS task role, EC2 IMDS).</li>
 *   </ol>
 *
 * <p>Region list comes from {@code fx.aws.regions} (the regions to enumerate
 * during sync). If empty, the sync simply finds nothing.
 */
@Configuration
public class AwsClientsConfig {

    @Bean
    @ConfigurationProperties(prefix = "fx.aws")
    public AwsProperties awsProperties() {
        return new AwsProperties();
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(AwsProperties props) {
        if (props.getAccessKey() != null && !props.getAccessKey().isBlank()
                && props.getSecretKey() != null && !props.getSecretKey().isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
        }
        return DefaultCredentialsProvider.create();
    }

    @Data
    public static class AwsProperties {
        /** Optional admin / read-only AWS access key id. Leave blank to use the SDK's default chain. */
        private String accessKey;
        /** Optional admin / read-only AWS secret access key. */
        private String secretKey;
        /** Regions the sync workflow enumerates. */
        private List<String> regions = new ArrayList<>();
    }
}
