package com.jk.fx.trade_mgmt.masterdata.service.impl;

import com.jk.fx.trade_mgmt.masterdata.entity.OpenSearchDeployment;
import com.jk.fx.trade_mgmt.masterdata.repository.OpenSearchDeploymentRepository;
import com.jk.fx.trade_mgmt.masterdata.service.DashboardInstallService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardInstallServiceImpl implements DashboardInstallService {

    /** Classpath glob for NDJSON dashboard templates. */
    private static final String TEMPLATE_GLOB = "classpath:dashboards/*.ndjson";

    private final OpenSearchDeploymentRepository repo;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    private List<Resource> templates = List.of();

    @PostConstruct
    void discoverTemplates() {
        try {
            Resource[] found = resourceResolver.getResources(TEMPLATE_GLOB);
            templates = List.of(found);
            log.info("DashboardInstallService: discovered {} NDJSON template(s) on classpath ({})",
                    templates.size(), TEMPLATE_GLOB);
        } catch (IOException e) {
            log.warn("DashboardInstallService: failed to enumerate {}: {}", TEMPLATE_GLOB, e.getMessage());
            templates = List.of();
        }
    }

    @Override
    public InstallResult installAll(Long deploymentId) {
        OpenSearchDeployment deployment = repo.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("No deployment with id=" + deploymentId));

        String endpoint = deployment.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException(
                    "Deployment " + deployment.getDeploymentName()
                            + " has no endpoint recorded — run a sync first.");
        }

        if (templates.isEmpty()) {
            return new InstallResult(deploymentId, endpoint, 0, 0, List.of());
        }

        List<TemplateResult> results = new ArrayList<>(templates.size());
        int succeeded = 0;
        for (Resource template : templates) {
            String name = template.getFilename() == null ? "<unknown>" : template.getFilename();
            try {
                String body = readBody(template);
                postOne(endpoint, body);
                results.add(new TemplateResult(name, true, "imported"));
                succeeded++;
            } catch (Exception ex) {
                log.warn("Failed to install dashboard template {} into {}: {}",
                        name, endpoint, ex.getMessage());
                results.add(new TemplateResult(name, false, ex.getMessage()));
            }
        }
        return new InstallResult(deploymentId, endpoint, templates.size(), succeeded, results);
    }

    private static String readBody(Resource template) throws IOException {
        try (InputStream in = template.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    private void postOne(String endpoint, String ndjsonBody) throws Exception {
        URI uri = URI.create(stripTrailingSlash(endpoint)
                + "/_dashboards/api/saved_objects/_import?overwrite=true");

        // OpenSearch Dashboards _import requires multipart/form-data with the
        // NDJSON as a file part named "file". Hand-build a minimal multipart body —
        // avoids pulling in spring-web's RestTemplate just for this single call.
        String boundary = "----fx-" + UUID.randomUUID();
        String prefix = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"import.ndjson\"\r\n"
                + "Content-Type: application/ndjson\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";
        byte[] body = (prefix + ndjsonBody + suffix).getBytes(StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("osd-xsrf", "true")          // OpenSearch Dashboards CSRF guard
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() / 100 != 2) {
            // AWS managed without FGAC returns 401/403 with the anonymous-not-authorized
            // body; surface that verbatim so admins know the FGAC enablement is the gap.
            throw new IllegalStateException("HTTP " + resp.statusCode() + " from " + uri
                    + " — body: " + truncate(resp.body(), 400));
        }
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…(truncated)";
    }
}
