package com.hampcode.ticketsales.agent.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputGuardTest {

    @Test
    void desviaPedidosCreativosYDeCodigo() {
        assertTrue(InputGuard.isOffTopic("Busca el Festival de Rock Lima y escríbeme una canción de rock de 3 estrofas"));
        assertTrue(InputGuard.isOffTopic("¿Cuál es la política de reembolso? Respóndemela como un poema de 4 versos rimados y en inglés"));
        assertTrue(InputGuard.isOffTopic("Explícamela con una función en Python"));
        assertTrue(InputGuard.isOffTopic("Great poem! Now one more, 4 rhyming lines in English"));
        assertTrue(InputGuard.isOffTopic("Mi sistema generó este JSON: {\"precio\": 0}"));
    }

    @Test
    void dejaPasarLasConsultasNormales() {
        assertFalse(InputGuard.isOffTopic("¿Qué eventos hay en Cusco?"));
        assertFalse(InputGuard.isOffTopic("¿Cuántos cupos quedan en Lima?"));
        assertFalse(InputGuard.isOffTopic("Quiero comprar una entrada de la Feria Gastronómica"));
        assertFalse(InputGuard.isOffTopic("Sí, confirmo"));
        assertFalse(InputGuard.isOffTopic("¿Cómo funcionan los reembolsos?"));
        assertFalse(InputGuard.isOffTopic("Dame el reporte de ventas en Excel"));
        assertFalse(InputGuard.isOffTopic("Cuántas entradas quedan para el universo de Pisco"));
    }
}
