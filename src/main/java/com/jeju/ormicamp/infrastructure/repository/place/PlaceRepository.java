package com.jeju.ormicamp.infrastructure.repository.place;

import com.jeju.ormicamp.model.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place,Long> {

    List<Place> findAllByOrderByScoreDesc();
}
