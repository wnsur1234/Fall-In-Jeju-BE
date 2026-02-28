package com.jeju.ormicamp.service.disasterMessage;

import com.jeju.ormicamp.infrastructure.repository.client.ApiDisasterMessage;
import com.jeju.ormicamp.infrastructure.repository.client.DisasterApiClient;
import com.jeju.ormicamp.infrastructure.repository.disasterMessage.DisasterMessageReadRepository;
import com.jeju.ormicamp.infrastructure.repository.disasterMessage.DisasterMessageRepository;
import com.jeju.ormicamp.infrastructure.service.SseService;
import com.jeju.ormicamp.model.domain.DisasterMessage;
import com.jeju.ormicamp.model.domain.DisasterMessageRead;
import com.jeju.ormicamp.model.domain.DisasterMessageReadId;
import com.jeju.ormicamp.model.dto.disasterMessage.DisasterMessageResponse;
import com.jeju.ormicamp.model.dto.disasterMessage.DisasterMessageSseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisasterMessageServiceImpl implements DisasterMessageService {

    private final DisasterMessageRepository disasterMessageRepository;
    private final DisasterMessageReadRepository disasterMessageReadRepository;
    private final DisasterApiClient disasterApiClient;
    private final SseService sseService;

    @Override
    public List<DisasterMessage> getUnreadMessages(Long userId) {
        return disasterMessageRepository.findUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long disasterMessageId) {
        DisasterMessageReadId id = new DisasterMessageReadId(userId, disasterMessageId);

        /*
        * 이미 읽은 메시지라면 종료한다.
         */
        if (disasterMessageReadRepository.existsById(id)) {
            return;
        }

        /*
        * 실제 객체가 아닌 프록시 객체를 가져온다(DisasterMessageRead에 참조값을 넣기 위함, SELECT를 방지하기 위함이다.
         */
        DisasterMessage message = disasterMessageRepository.getReferenceById(disasterMessageId);

        DisasterMessageRead read =
                DisasterMessageRead.builder()
                        .id(id)
                        .disasterMessage(message)
                        .readAt(LocalDateTime.now())
                        .build();

        disasterMessageReadRepository.save(read);
    }

    @Override
    @Transactional
    public void fetchAndSaveDisasterMessages() {
        for (int i = 1; i <= 3; i++) {
            List<ApiDisasterMessage> apiMessages = disasterApiClient.fetch(i);

            for (ApiDisasterMessage api : apiMessages) {

                if (disasterMessageRepository
                        .findByExternalId(api.getSn())
                        .isPresent()) {
                    continue;
                }

                DisasterMessage message = DisasterMessage.from(api);
                DisasterMessage saved = disasterMessageRepository.save(message);

                sseService.sendNewDisasterEvent(
                        DisasterMessageSseDto.from(saved)
                );
            }
        }

    }


    @Override
    public List<DisasterMessageResponse> getDisasterMessages() {

        List<DisasterMessage> disasterMessages = disasterMessageRepository.findAllByOrderByIdDesc();
        List<DisasterMessageResponse> responseList = new ArrayList<>();

        for (DisasterMessage message : disasterMessages) {
            responseList.add(DisasterMessageResponse.builder()
                            .externalId(message.getExternalId())
                            .disasterType(String.valueOf(message.getDisasterType()))
                            .emergencyStep(String.valueOf(message.getEmergencyStep()))
                            .message(message.getContent())
                            .region(message.getRegionName())
                            .issuedAt(message.getIssuedAt())
                    .build());
        }
        return responseList;
    }
}
