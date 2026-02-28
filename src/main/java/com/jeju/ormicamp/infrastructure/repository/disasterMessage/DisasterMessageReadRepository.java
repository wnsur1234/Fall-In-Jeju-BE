package com.jeju.ormicamp.infrastructure.repository.disasterMessage;

import com.jeju.ormicamp.model.domain.DisasterMessageRead;
import com.jeju.ormicamp.model.domain.DisasterMessageReadId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisasterMessageReadRepository extends JpaRepository<DisasterMessageRead, DisasterMessageReadId> {

    /**
     * 사용자가 재난 메시지를 읽었는지 확인하기 위한 메서드이다.
     */
    boolean existsById(DisasterMessageReadId id);
}
