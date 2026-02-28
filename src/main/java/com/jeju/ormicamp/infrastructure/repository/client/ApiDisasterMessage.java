package com.jeju.ormicamp.infrastructure.repository.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ApiDisasterMessage {
    @JsonProperty("SN")
    private String sn;

    @JsonProperty("RCPTN_RGN_NM")
    private String regionName;

    @JsonProperty("DST_SE_NM")
    private String disasterType;

    @JsonProperty("EMRG_STEP_NM")
    private String emergencyStep;

    @JsonProperty("MSG_CN")
    private String message;

    @JsonProperty("REG_YMD")
    private String registeredDate;

    @JsonProperty("CRT_DT")
    private String createdAt;
}
