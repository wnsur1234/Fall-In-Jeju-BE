package com.jeju.ormicamp.model.dto.planner;

import com.jeju.ormicamp.model.code.Region;
import com.jeju.ormicamp.model.code.Theme;
import com.jeju.ormicamp.model.code.Language;
import com.jeju.ormicamp.model.domain.TravelInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class TravelDateResDto {

    // travelDate_id
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private Region region;  // 여행 지역
    private List<Theme> themes;  // 여행 테마
    private Language language;  // 언어 선택

    public static TravelDateResDto from(TravelInfo travelInfo) {
        return new TravelDateResDto(
                travelInfo.getId(),
                travelInfo.getStartDate(),
                travelInfo.getEndDate(),
                travelInfo.getRegion(),
                travelInfo.getThemes(),
                travelInfo.getLanguage()
        );
    }


}
