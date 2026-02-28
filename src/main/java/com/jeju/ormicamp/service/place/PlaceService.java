package com.jeju.ormicamp.service.place;

import com.jeju.ormicamp.infrastructure.repository.place.PlaceRepository;
import com.jeju.ormicamp.model.domain.Place;
import com.jeju.ormicamp.model.dto.place.PlaceResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    @Transactional
    public List<PlaceResDto> findAllByOrderByScoreDesc() {
        List<Place> places = placeRepository.findAllByOrderByScoreDesc();
        List<PlaceResDto> placeResDto = new ArrayList<>();

        for (Place place : places) {
            placeResDto.add(PlaceResDto.from(place));
        }

        return placeResDto;
    }
}
