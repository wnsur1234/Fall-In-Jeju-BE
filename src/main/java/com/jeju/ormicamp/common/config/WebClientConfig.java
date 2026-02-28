package com.jeju.ormicamp.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Slf4j
@Configuration
@EnableConfigurationProperties(TmapProperties.class)
public class WebClientConfig {

    private static final String TMAP_BASE_URL = "https://apis.openapi.sk.com";

    @Bean(name = "disasterWebClient")
    public WebClient disasterWebClient() {
        return WebClient.builder()
                .baseUrl("https://www.safetydata.go.kr")
                .uriBuilderFactory(
                        new DefaultUriBuilderFactory() {{
                            setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
                        }}
                )
                .build();
    }

    @Bean(name = "tmapWebClient")
    public WebClient tmapWebClient(TmapProperties props) {

        // appKey가 비어있으면 시작부터 터뜨리는 게 디버깅 빠름
        if (props.getAppKey() == null || props.getAppKey().isBlank()) {
            throw new IllegalStateException("TMAP_APP_KEY가 설정되지 않았습니다. (tmap.app-key)");
        }

        return WebClient.builder()
                .baseUrl(TMAP_BASE_URL)
                // ✅ TMAP 핵심: 헤더 appKey
                .defaultHeader("appKey", props.getAppKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return (request, next) -> {
            // appKey는 로그에 노출하지 말자
            log.info("[TMAP API REQUEST] {} {}", request.method(), request.url());
            return next.exchange(request);
        };
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("[TMAP API RESPONSE] status={}", response.statusCode());
            return reactor.core.publisher.Mono.just(response);
        });
    }

}
