package com.jeju.ormicamp.service.planner;

import com.jeju.ormicamp.common.customUserDetail.UserPrincipal;
import com.jeju.ormicamp.common.exception.CustomException;
import com.jeju.ormicamp.common.exception.ErrorCode;
import com.jeju.ormicamp.infrastructure.repository.planner.TravelInfoRepository;
import com.jeju.ormicamp.infrastructure.repository.user.UserRepository;
import com.jeju.ormicamp.model.domain.TravelInfo;
import com.jeju.ormicamp.model.domain.User;
import com.jeju.ormicamp.model.dto.planner.TravelDateReqDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TravelInfoService {

    private final TravelInfoRepository travelInfoRepository;
    private final UserRepository userRepository;

    public TravelInfo saveDate(TravelDateReqDto dto,Long userId) {

        if(!dto.getStartDate().isBefore(dto.getEndDate())) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        TravelInfo travelInfo = TravelInfo.builder()
                .startDate(dto.getStartDate())
                .user(user)
                .endDate(dto.getEndDate())
                .region(dto.getRegion())
                .themes(dto.getThemes())
                .language(dto.getLanguage())
                .build();
        return travelInfoRepository.save(travelInfo);
    }

    public TravelInfo updateDate(Long travelDateId, TravelDateReqDto dto) {

        TravelInfo update = travelInfoRepository.findById(travelDateId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        update.updateDate(dto.getStartDate(), dto.getEndDate());

        return travelInfoRepository.save(update);
    }
}
