package com.hampcode.ticketsales.agent.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputGuardTest {

    @Test
    void reemplazaCualquierRespuestaConBloqueDeCodigo() {
        assertEquals(OutputGuard.NO_CODE, OutputGuard.clean("```python\ndef f(): pass\n```"));
        assertEquals(OutputGuard.NO_CODE, OutputGuard.clean("Aquí tienes: ```json {} ```"));
    }

    @Test
    void dejaPasarElTextoNormal() {
        assertEquals("Hay 3 eventos en Lima.", OutputGuard.clean("Hay 3 eventos en Lima."));
        assertEquals(null, OutputGuard.clean(null));
    }
}
