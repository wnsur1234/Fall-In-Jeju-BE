package com.jeju.ormicamp.controller.place;

import com.jeju.ormicamp.common.dto.BaseResponse;
import com.jeju.ormicamp.model.dto.place.PlaceResDto;
import com.jeju.ormicamp.service.place.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<PlaceResDto>>> responseEntity(
            ) {

        return ResponseEntity.ok()
                .body(BaseResponse.success("여행지 응답", placeService.findAllByOrderByScoreDesc()));
    }
}
