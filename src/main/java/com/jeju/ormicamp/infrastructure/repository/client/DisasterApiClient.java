package com.jeju.ormicamp.infrastructure.repository.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DisasterApiClient {

    @Qualifier("disasterWebClient")
    private final WebClient disasterWebClient;

    public List<ApiDisasterMessage> fetch(int pageNo) {

        DisasterApiResponse response =

        disasterWebClient.get()
                .uri("https://www.safetydata.go.kr/V2/api/DSSP-IF-00247"
                        + "?serviceKey=" + "6TUY6NQ533289K20"
                        + "&returnType=json"
                        + "&crtDt=20251218"
                        + "&pageNo=" + pageNo)
                        //+ "&rgnNm=제주도")
                .retrieve()
                .bodyToMono(DisasterApiResponse.class)
                .block();

        if (response == null || response.getHeader() == null) {
            return List.of();
        }

        if (!"00".equals(response.getHeader().getResultCode())) {
            return List.of();
        }

        if (response.getBody() == null) {
            return List.of();
        }

        return response.getBody();
    }

}
