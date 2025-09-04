package com.example.consent_service.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsentController.class)
class ConsentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsentService service;

    @Test
    void inbound_shouldReturnOptInTrue() throws Exception {
        when(service.handleInboundSms("123", "TAK")).thenReturn(true);

        mockMvc.perform(post("/consents/inbound-sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiver\":\"123\",\"text\":\"TAK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverNumber").value("123"))
                .andExpect(jsonPath("$.optIn").value(true));
    }

    @Test
    void inbound_shouldReturnOptInFalse() throws Exception {
        when(service.handleInboundSms("456", "NIE")).thenReturn(false);

        mockMvc.perform(post("/consents/inbound-sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiver\":\"456\",\"text\":\"NIE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverNumber").value("456"))
                .andExpect(jsonPath("$.optIn").value(false));
    }

    @Test
    void getConsent_shouldReturnOptInTrue() throws Exception {
        when(service.getConsent("123")).thenReturn(true);

        mockMvc.perform(get("/consents/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverNumber").value("123"))
                .andExpect(jsonPath("$.optIn").value(true));
    }

    @Test
    void getConsent_shouldReturnOptInFalse() throws Exception {
        when(service.getConsent("456")).thenReturn(false);

        mockMvc.perform(get("/consents/456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverNumber").value("456"))
                .andExpect(jsonPath("$.optIn").value(false));
    }
}

