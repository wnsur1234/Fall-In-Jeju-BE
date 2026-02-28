package com.jeju.ormicamp.model.dto.planner;

import com.jeju.ormicamp.model.code.Region;
import com.jeju.ormicamp.model.code.Theme;
import com.jeju.ormicamp.model.code.Language;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;


@Getter
@Setter
public class TravelDateReqDto {

    private LocalDate startDate;

    private LocalDate endDate;

    private Region region;  // 여행 지역

    private List<Theme> themes;  // 여행 테마 (복수 선택 가능)

    private Language language;  // 언어 선택
}
