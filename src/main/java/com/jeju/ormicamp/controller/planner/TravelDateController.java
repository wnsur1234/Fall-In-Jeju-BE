package com.jeju.ormicamp.controller.planner;

import com.jeju.ormicamp.common.customUserDetail.UserPrincipal;
import com.jeju.ormicamp.common.dto.BaseResponse;
import com.jeju.ormicamp.model.domain.User;
import com.jeju.ormicamp.model.dto.planner.TravelDateReqDto;
import com.jeju.ormicamp.model.dto.planner.TravelDateResDto;
import com.jeju.ormicamp.service.planner.TravelInfoService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planner")
@AllArgsConstructor
public class TravelDateController {

    private final TravelInfoService travelInfoService;

    @PostMapping("/date")
    public ResponseEntity<BaseResponse<TravelDateResDto>> date(
            @RequestBody TravelDateReqDto dto,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {

        Long user = userPrincipal.userId();
        TravelDateResDto result = TravelDateResDto.from(travelInfoService.saveDate(dto,user));

        return ResponseEntity.ok()
                .body(BaseResponse.success("날짜 저장성공",result));
    }

    @PatchMapping("/update/{travelDateId}")
    public ResponseEntity<BaseResponse<TravelDateResDto>> update(
            @PathVariable Long travelDateId,
            @RequestBody TravelDateReqDto dto
    ){
        TravelDateResDto result = TravelDateResDto.from(travelInfoService.updateDate(travelDateId,dto));

        return ResponseEntity.ok()
                .body(BaseResponse.success("날짜 정보 수정",result));
    }
}
