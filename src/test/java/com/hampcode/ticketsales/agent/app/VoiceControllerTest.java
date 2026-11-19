package com.hampcode.ticketsales.agent.app;

import com.hampcode.ticketsales.agent.app.dto.AgentResponse;
import com.hampcode.ticketsales.agent.app.dto.VoiceResponse;
import com.hampcode.ticketsales.agent.app.tools.PurchaseTools;
import com.hampcode.ticketsales.agent.core.AgentService;
import com.hampcode.ticketsales.agent.core.voice.VoiceService;
import com.hampcode.ticketsales.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceControllerTest {

    @Mock VoiceService voiceService;
    @Mock AgentService agentService;
    @Mock PurchaseTools purchaseTools;
    @InjectMocks VoiceController voiceController;

    final MockMultipartFile audio = new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});
    Authentication auth;

    @BeforeEach
    void setUp() {
        auth = new UsernamePasswordAuthenticationToken("ana", null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
    }

    @Test
    void transcribesTheAudioAndAnswersThroughTheSameAgent() throws Exception {
        when(voiceService.transcribe(any(), anyString())).thenReturn("qué eventos hay");
        when(purchaseTools.hasPending("ana")).thenReturn(false);
        when(agentService.chat("ana", Set.of("PARTICIPANT"), "qué eventos hay", "c1")).thenReturn(AgentResponse.onlyMessage("c1", "Encontré 3 evento(s)."));
        when(voiceService.speak("Encontré 3 evento(s).")).thenReturn(new byte[]{7});

        VoiceResponse result = voiceController.voice(audio, "c1", true, auth);

        assertEquals("qué eventos hay", result.transcript());
        assertEquals("Encontré 3 evento(s).", result.response().message());
        assertArrayEquals(new byte[]{7}, result.audio());
    }

    @Test
    void doesNotGenerateAudioWhenSpeakIsFalse() throws Exception {
        when(voiceService.transcribe(any(), anyString())).thenReturn("hola");
        when(agentService.chat(anyString(), any(), anyString(), any())).thenReturn(AgentResponse.onlyMessage("c1", "Hola"));

        VoiceResponse result = voiceController.voice(audio, null, false, auth);

        assertNull(result.audio());
        verify(voiceService, never()).speak(anyString());
    }

    @Test
    void neverLetsTheVoiceConfirmAPendingPurchase() throws Exception {
        when(voiceService.transcribe(any(), anyString())).thenReturn("sí, confirmo");
        when(purchaseTools.hasPending("ana")).thenReturn(true);

        VoiceResponse result = voiceController.voice(audio, "c1", false, auth);

        assertEquals(VoiceController.PENDING_PURCHASE_MESSAGE, result.response().message());
        verify(agentService, never()).chat(anyString(), any(), anyString(), any());
    }

    @Test
    void rejectsAnAudioThatCannotBeUnderstood() {
        when(voiceService.transcribe(any(), anyString())).thenReturn("");

        assertThrows(InvalidRequestException.class, () -> voiceController.voice(audio, null, true, auth));
    }

    @Test
    void rejectsAnEmptyUpload() {
        var empty = new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[0]);

        assertThrows(InvalidRequestException.class, () -> voiceController.voice(empty, null, true, auth));
        verify(voiceService, never()).transcribe(any(), anyString());
    }
}
