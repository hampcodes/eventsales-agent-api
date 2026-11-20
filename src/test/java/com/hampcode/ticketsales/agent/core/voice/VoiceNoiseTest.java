package com.hampcode.ticketsales.agent.core.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceNoiseTest {

    @Test
    void descartaFrasesInventadasPorElModeloDeAudio() {
        assertTrue(VoiceService.isNoise("Subtítulos realizados por la comunidad de Amara.org"));
        assertTrue(VoiceService.isNoise("   "));
        assertTrue(VoiceService.isNoise(null));
    }

    @Test
    void aceptaUnaPreguntaNormal() {
        assertFalse(VoiceService.isNoise("¿Qué eventos hay en Cusco?"));
    }
}
