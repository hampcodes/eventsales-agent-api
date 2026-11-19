package com.hampcode.ticketsales.agent.core.voice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceServiceTest {

    @Mock TranscriptionModel transcriptionModel;
    @Mock TextToSpeechModel textToSpeechModel;
    @InjectMocks VoiceService voiceService;

    @Test
    void sendsTheAudioWithItsFileNameSoTheFormatIsDetected() {
        var resource = ArgumentCaptor.forClass(Resource.class);
        when(transcriptionModel.transcribe(resource.capture())).thenReturn("  hola  ");

        String transcript = voiceService.transcribe(new byte[]{1, 2, 3}, "grabacion.mp4");

        assertEquals("hola", transcript);
        assertEquals("grabacion.mp4", resource.getValue().getFilename());
    }

    @Test
    void usesAWebmFileNameWhenNoneArrives() {
        var resource = ArgumentCaptor.forClass(Resource.class);
        when(transcriptionModel.transcribe(resource.capture())).thenReturn("hola");

        voiceService.transcribe(new byte[]{1}, null);

        assertEquals("audio.webm", resource.getValue().getFilename());
    }

    @Test
    void turnsTheTextIntoAudio() {
        when(textToSpeechModel.call("Hola")).thenReturn(new byte[]{9, 8});

        assertArrayEquals(new byte[]{9, 8}, voiceService.speak("Hola"));
        verify(textToSpeechModel).call("Hola");
    }
}
