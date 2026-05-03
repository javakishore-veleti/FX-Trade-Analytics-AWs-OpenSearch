package com.jk.fx.trade_mgmt.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional static AWS credentials shared across all services in this repo.
 * Populated by {@code application-local-secrets.yml} (gitignored, generated
 * by {@code npm run local:app:generate-app-secrets-yaml}).
 *
 * <p>When both {@code accessKey} and {@code secretKey} are set, the search
 * client uses a {@code StaticCredentialsProvider}. Otherwise it falls back
 * to {@code DefaultCredentialsProvider} (env vars, ~/.aws/credentials, ECS
 * task role, etc.) — which is what we want in production.
 */
@Data
@ConfigurationProperties(prefix = "fx.aws")
public class AwsCredentialsProperties {
    private String accessKey;
    private String secretKey;

    public boolean hasStatic() {
        return accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
