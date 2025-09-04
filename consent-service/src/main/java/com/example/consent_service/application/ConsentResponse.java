package com.example.consent_service.application;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConsentResponse {
    private String receiverNumber;
    private Boolean optIn;
}
