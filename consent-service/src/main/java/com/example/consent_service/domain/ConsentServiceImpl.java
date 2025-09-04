package com.example.consent_service.domain;

import com.example.consent_service.application.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
class ConsentServiceImpl implements ConsentService {
    private final ConsentRepository repo;
    private final ConsentProducer producer;

  @Override
  public Boolean handleInboundSms(String receiver, String optInText) {
      String normalized = optInText.trim().toUpperCase();
      Boolean optIn = switch (normalized) {
          case "TAK" -> Boolean.TRUE;
          case "NIE" -> Boolean.FALSE;
          default -> throw new IllegalArgumentException("Invalid text: " + optInText);
      };
      Consent consent = new Consent(receiver, optIn, Instant.now());
      repo.save(consent);
      producer.publishConsent(receiver, optIn);
      return optIn;
  }

  @Override
  public Boolean getConsent(String receiver) {
      Consent consent = repo.findById(receiver).orElseThrow();
      return consent.getOptIn();
  }
}
