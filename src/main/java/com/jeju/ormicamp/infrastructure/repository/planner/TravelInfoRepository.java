package com.jeju.ormicamp.infrastructure.repository.planner;

import com.jeju.ormicamp.model.domain.TravelInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TravelInfoRepository extends JpaRepository<TravelInfo, Long> {

    @Query("select t from TravelInfo t where t.user.id = :userId order by t.createDate desc limit 1")
    Optional<TravelInfo> findByUserId(@Param("userId") Long userId);
}
