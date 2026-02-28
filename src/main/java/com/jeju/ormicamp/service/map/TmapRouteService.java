package com.jeju.ormicamp.service.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeju.ormicamp.model.dto.map.*;
import com.jeju.ormicamp.model.dto.map.car.SimpleRouteResponse;
import com.jeju.ormicamp.model.dto.map.car.TmapCarRawResponse;
import com.jeju.ormicamp.model.dto.map.car.TmapCarRouteMapper;
import com.jeju.ormicamp.model.dto.map.transit.TransitRawResponse;
import com.jeju.ormicamp.model.dto.map.transit.TransitRouteMapper;
import com.jeju.ormicamp.model.dto.map.walk.TmapWalkRawResponse;
import com.jeju.ormicamp.model.dto.map.walk.TmapWalkRouteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmapRouteService {

    @Qualifier("tmapWebClient")
    private final WebClient tmapWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /* =========================
       PUBLIC
     ========================= */

    public SimpleRouteResponse getRouteCarAndWalk(RouteReqDto req) {
        validate(req);

        try {
            return switch (req.mode()) {
                case CAR -> callCar(req, req.waypoints());
                case WALK -> callWalk(req);
                default -> SimpleRouteResponse.unavailable();
            };
        } catch (Exception e) {
            log.error("[ROUTE ERROR]", e);
            return SimpleRouteResponse.unavailable();
        }
    }

    public RouteResponse getRouteTransit(RouteReqDto req) {
        validate(req);

        try {
            return callTransit(req);
        } catch (Exception e) {
            log.error("[ROUTE ERROR]", e);
            return RouteResponse.unavailable();
        }
    }

    /* =========================
       VALIDATE
     ========================= */

    private void validate(RouteReqDto req) {
        if (req == null) throw new IllegalArgumentException("req null");
        if (req.mode() == null) throw new IllegalArgumentException("mode null");
        if (req.origin() == null || req.destination() == null)
            throw new IllegalArgumentException("origin/destination 필수");
    }

    /* =========================
       🚗 CAR
     ========================= */

    private SimpleRouteResponse callCar(RouteReqDto req, List<PointDto> waypoints) {

        Map<String, Object> body = new HashMap<>();
        body.put("startX", req.origin().lng());
        body.put("startY", req.origin().lat());
        body.put("endX", req.destination().lng());
        body.put("endY", req.destination().lat());
        body.put("reqCoordType", "WGS84GEO");
        body.put("resCoordType", "WGS84GEO");
        body.put("searchOption", "0");
        body.put("option", "0");
        body.put("startTime",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")));

        if (waypoints != null && !waypoints.isEmpty()) {
            String passList = waypoints.stream()
                    .map(p -> p.lng() + "," + p.lat())
                    .collect(Collectors.joining("_"));
            body.put("passList", passList);
        }

        return tmapWebClient.post()
                .uri("/tmap/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TmapCarRawResponse.class)
                .map(TmapCarRouteMapper::toRouteResponse)
                .onErrorReturn(SimpleRouteResponse.unavailable())
                .block(Duration.ofSeconds(7));
    }

    /* =========================
       🚶 WALK
     ========================= */

    private SimpleRouteResponse callWalk(RouteReqDto req) {

        Map<String, Object> body = new HashMap<>();
        body.put("startX", req.origin().lng());
        body.put("startY", req.origin().lat());
        body.put("endX", req.destination().lng());
        body.put("endY", req.destination().lat());
        body.put("startName",
                Optional.ofNullable(req.origin().name()).orElse("출발지"));
        body.put("endName",
                Optional.ofNullable(req.destination().name()).orElse("도착지"));
        body.put("reqCoordType", "WGS84GEO");
        body.put("resCoordType", "WGS84GEO");

        TmapWalkRawResponse raw = tmapWebClient.post()
                .uri("/tmap/routes/pedestrian")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TmapWalkRawResponse.class)
                .block(Duration.ofSeconds(7));

        return TmapWalkRouteMapper.toRouteResponse(raw);
    }

    /* =========================
       🚌 TRANSIT
     ========================= */

    private RouteResponse callTransit(RouteReqDto req) {

        Map<String, Object> body = new HashMap<>();
        body.put("startX", req.origin().lng());
        body.put("startY", req.origin().lat());
        body.put("endX", req.destination().lng());
        body.put("endY", req.destination().lat());
        body.put("reqCoordType", "WGS84GEO");
        body.put("resCoordType", "WGS84GEO");
        body.put("count", 3);
        body.put("lang", 0);
        body.put("format", "json");

        TransitRawResponse raw = tmapWebClient.post()
                .uri("/transit/routes/sub")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TransitRawResponse.class)
                .onErrorResume(e -> {
                    log.error("[TMAP TRANSIT FAIL]", e);
                    return Mono.empty();
                })
                .block(Duration.ofSeconds(7));

        if (raw == null) {
            return RouteResponse.unavailable();
        }

        return TransitRouteMapper.toRouteResponse(raw);
    }

    /* =========================
       🧭 OPTIMIZATION (CAR ONLY)
     ========================= */

    private List<PointDto> callOptimization(RouteReqDto req) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("startName", Optional.ofNullable(req.origin().name()).orElse("출발지"));
        body.put("startX", req.origin().lng());
        body.put("startY", req.origin().lat());
        body.put("endName", Optional.ofNullable(req.destination().name()).orElse("도착지"));
        body.put("endX", req.destination().lng());
        body.put("endY", req.destination().lat());
        body.put("reqCoordType", "WGS84GEO");
        body.put("resCoordType", "WGS84GEO");
        body.put("searchOption", "0");
        body.put("startTime",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")));

        // 🔑 viaPoints는 Map + 숫자 타입
        Map<String, Object> viaPoints = new LinkedHashMap<>();

        int idx = 1;
        for (PointDto wp : req.waypoints()) {

            if (isSamePoint(wp, req.destination())) continue;

            Map<String, Object> via = new HashMap<>();
            via.put("viaPointId", idx);          // int
            via.put("viaX", wp.lng());           // double
            via.put("viaY", wp.lat());           // double
            via.put("viaPointName",
                    Optional.ofNullable(wp.name()).orElse("경유지"));

            viaPoints.put(String.valueOf(idx), via);
            idx++;
        }

        body.put("viaPoints", viaPoints);

        try {
            log.info("[TMAP OPT REQ JSON] {}", objectMapper.writeValueAsString(body));
        } catch (Exception ignored) {}

        JsonNode raw = tmapWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/tmap/routes/routeOptimization10")
                        .queryParam("version", "1")
                        .build()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class).flatMap(msg -> {
                            log.error("[TMAP OPT ERROR] {}", msg);
                            return Mono.error(new IllegalStateException("TMAP OPT FAIL"));
                        })
                )
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(7));

        return extractOrder(raw, req.waypoints());
    }

    /* =========================
       UTIL
     ========================= */

    private List<PointDto> extractOrder(JsonNode raw, List<PointDto> original) {
        if (raw == null) return Collections.emptyList();

        JsonNode via = raw.at("/features/0/properties/viaPoints");
        if (!via.isArray()) return Collections.emptyList();

        List<PointDto> ordered = new ArrayList<>();
        for (JsonNode v : via) {
            double x = v.get("viaX").asDouble();
            double y = v.get("viaY").asDouble();
            ordered.add(findClosest(original, y, x));
        }
        return ordered.stream().filter(Objects::nonNull).toList();
    }

    private PointDto findClosest(List<PointDto> points, double lat, double lng) {
        return points.stream()
                .min(Comparator.comparingDouble(
                        p -> Math.pow(p.lat() - lat, 2) + Math.pow(p.lng() - lng, 2)
                ))
                .orElse(null);
    }

    private boolean isSamePoint(PointDto a, PointDto b) {
        return Double.compare(a.lat(), b.lat()) == 0
                && Double.compare(a.lng(), b.lng()) == 0;
    }

    private List<PointDto> deduplicateByCoordinate(List<PointDto> points) {
        List<PointDto> result = new ArrayList<>();
        for (PointDto p : points) {
            boolean exists = result.stream().anyMatch(r ->
                    Double.compare(r.lat(), p.lat()) == 0 &&
                            Double.compare(r.lng(), p.lng()) == 0
            );
            if (!exists) result.add(p);
        }
        return result;
    }
}
