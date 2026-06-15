package com.tracemind.mcp.client;

import com.tracemind.mcp.config.SplunkProperties;
import com.tracemind.mcp.exception.SplunkClientException;
import com.tracemind.mcp.model.SplunkSearchRequest;
import com.tracemind.mcp.model.SplunkSearchResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class SplunkClientImpl implements SplunkClient {

    private static final Logger log = LoggerFactory.getLogger(SplunkClientImpl.class);

    private final RestClient restClient;
    private final SplunkProperties properties;

    public SplunkClientImpl(SplunkProperties splunkProperties) {
        this.properties = splunkProperties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", basicAuth(properties.getUsername(), properties.getPassword()))
                .defaultStatusHandler(status -> status.value() >= 400, this::handleError)
                .requestFactory(properties.isVerifySsl() ? defaultRequestFactory() : trustingRequestFactory())
                .build();
    }

    private static HttpComponentsClientHttpRequestFactory defaultRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory();
    }

    private static HttpComponentsClientHttpRequestFactory trustingRequestFactory() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (cert, chain) -> true)
                    .build();

            HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(sslContext)
                            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .build())
                    .build();

            return new HttpComponentsClientHttpRequestFactory(
                    HttpClientBuilder.create()
                            .setConnectionManager(cm)
                            .build());
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException("Failed to create trusting SSL context", e);
        }
    }

    private void handleError(HttpRequest httpRequest, ClientHttpResponse clientHttpResponse) {
        try {
            log.error("Handle Error {}", clientHttpResponse.getStatusText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SplunkSearchResponse execute(SplunkSearchRequest request) {
        log.debug("Executing SPL search: {}", request.getQuery());

        try {
            String rawResponse = restClient.post()
                    .uri("/services/search/jobs/export")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(buildSearchBody(request))
                    .retrieve()
                    .body(String.class);

            return SplunkSearchResponse.builder()
                    .success(true)
                    .rawResponse(rawResponse)
                    .build();
        } catch (SplunkClientException e) {
            throw e;
        } catch (Exception e) {
            throw new SplunkClientException("Failed to execute Splunk search: " + e.getMessage(), e);
        }
    }

    private String buildSearchBody(SplunkSearchRequest request) {
        return "search=" + URLEncoder.encode(request.getQuery(), StandardCharsets.UTF_8)
                + "&output_mode=json";
    }

    private void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus status = (HttpStatus) response.getStatusCode();
        throw new SplunkClientException("Splunk returned HTTP " + status.value() + ": " + status.getReasonPhrase());
    }

    private static String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
