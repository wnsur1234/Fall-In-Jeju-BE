package com.jeju.ormicamp.controller.map;

import com.jeju.ormicamp.model.dto.map.RouteReqDto;
import com.jeju.ormicamp.model.dto.map.RouteResponse;
import com.jeju.ormicamp.service.map.TmapRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final TmapRouteService routeService;

    @PostMapping
    public Object route(@RequestBody RouteReqDto req) {
        return switch (req.mode()) {
            case CAR, WALK -> routeService.getRouteCarAndWalk(req);
            case TRANSIT -> routeService.getRouteTransit(req);
        };
    }

}
