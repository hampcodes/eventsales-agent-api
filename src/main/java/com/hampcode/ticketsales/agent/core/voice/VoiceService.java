package com.hampcode.ticketsales.agent.core.voice;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.voice.enabled", havingValue = "true", matchIfMissing = true)
public class VoiceService {

    private static final String DEFAULT_FILE_NAME = "audio.webm";

    private final TranscriptionModel transcriptionModel;
    private final TextToSpeechModel textToSpeechModel;

    public String transcribe(byte[] audio, String fileName) {
        String name = fileName == null || fileName.isBlank() ? DEFAULT_FILE_NAME : fileName;
        var resource = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return name;
            }
        };
        return transcriptionModel.transcribe(resource).trim();
    }

    public byte[] speak(String text) {
        return textToSpeechModel.call(text);
    }
}
