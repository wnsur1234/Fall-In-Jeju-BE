package com.jeju.ormicamp.service.disasterMessage;

import com.jeju.ormicamp.model.domain.DisasterMessage;
import com.jeju.ormicamp.model.dto.disasterMessage.DisasterMessageResponse;

import java.util.List;

public interface DisasterMessageService {

    /*
    * 사용자 별로 읽지 않은 데이터를 조회하는 Service 메서드입니다.
     */
    List<DisasterMessage> getUnreadMessages(Long userId);

    /*
     * 메시지를 읽음 처리하는 메서드입니다.
     */
    void markAsRead(Long userId, Long disasterMessageId);

    /*
    * 재난 문자를 스케줄러로 가져오고 저장하는 메서드입니다.
     */
    void fetchAndSaveDisasterMessages();

    List<DisasterMessageResponse> getDisasterMessages();
}
