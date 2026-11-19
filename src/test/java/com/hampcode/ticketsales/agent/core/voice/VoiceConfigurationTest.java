package com.hampcode.ticketsales.agent.core.voice;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceConfigurationTest {

    @Test
    void speechSettingsOfApplicationYmlReachTheModelOptions() throws Exception {
        var sources = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        var binder = new Binder(ConfigurationPropertySources.from(sources.get(0)));

        var options = binder.bind("spring.ai.openai.audio.speech", OpenAiAudioSpeechProperties.class).get().toOptions();

        assertEquals("gpt-4o-mini-tts", options.getModel());
        assertEquals("alloy", options.getVoice());
        assertEquals("mp3", options.getResponseFormat());
        assertTrue(options.getInstructions().contains("español latinoamericano"));
    }
}
