package com.example.consent_service.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsentServiceTest {

    private ConsentRepository repo;
    private ConsentProducer producer;
    private ConsentServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(ConsentRepository.class);
        producer = mock(ConsentProducer.class);
        service = new ConsentServiceImpl(repo, producer);
    }

    @Test
    void handleInboundSms_shouldReturnTrueForTAK_andSaveConsent_andPublish() {
        String receiver = "123";
        String optInText = "TAK";

        Boolean result = service.handleInboundSms(receiver, optInText);

        assertTrue(result);
        verify(repo).save(argThat(c -> c.getReceiverNumber().equals(receiver) && c.getOptIn().equals(true)));
        verify(producer).publishConsent(receiver, true);
    }

    @Test
    void handleInboundSms_shouldReturnFalseForNIE_andSaveConsent_andPublish() {
        String receiver = "456";
        String optInText = "NIE";

        Boolean result = service.handleInboundSms(receiver, optInText);

        assertFalse(result);
        verify(repo).save(argThat(c -> c.getReceiverNumber().equals(receiver) && c.getOptIn().equals(false)));
        verify(producer).publishConsent(receiver, false);
    }

    @Test
    void handleInboundSms_shouldThrowForInvalidText() {
        String receiver = "789";
        String optInText = "INVALID";

        assertThrows(IllegalArgumentException.class, () -> service.handleInboundSms(receiver, optInText));
        verify(repo, never()).save(any());
        verify(producer, never()).publishConsent(anyString(), anyBoolean());
    }

    @Test
    void getConsent_shouldReturnOptInValue() {
        String receiver = "123";
        Consent consent = new Consent(receiver, true, Instant.now());
        when(repo.findById(receiver)).thenReturn(Optional.of(consent));

        Boolean result = service.getConsent(receiver);

        assertTrue(result);
        verify(repo).findById(receiver);
    }

    @Test
    void getConsent_shouldThrowIfNotFound() {
        String receiver = "notfound";
        when(repo.findById(receiver)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.getConsent(receiver));
        verify(repo).findById(receiver);
    }
}