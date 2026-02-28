package com.jeju.ormicamp.infrastructure.repository.disasterMessage;

import com.jeju.ormicamp.model.domain.DisasterMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DisasterMessageRepository extends JpaRepository<DisasterMessage, Long> {

    /**
     * 스케줄링에서 중복된 재난 문자를 판별하기 위한 메서드이다.
     */
    Optional<DisasterMessage> findByExternalId(String externalId);

    @Query("""
    SELECT dm
    FROM DisasterMessage dm
    WHERE NOT EXISTS (
      SELECT 1
      FROM DisasterMessageRead r
      WHERE r.id.userId = :userId
        AND r.disasterMessage = dm
    )
    ORDER BY dm.issuedAt DESC
    """)
    List<DisasterMessage> findUnreadByUserId(@Param("userId") Long userId);

    List<DisasterMessage> findAllByOrderByIdDesc();
}
