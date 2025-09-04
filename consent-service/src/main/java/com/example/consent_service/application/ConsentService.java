package com.example.consent_service.application;

public interface ConsentService {
    Boolean handleInboundSms(String receiver, String optInText);

    Boolean getConsent(String receiver);
}
