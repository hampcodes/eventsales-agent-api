package com.hampcode.ticketsales.agent.app.tools;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseToolsConfirmTest {

    @Test
    void elModeloNoTieneNingunaToolParaConfirmarCompras() {
        var tools = Arrays.stream(PurchaseTools.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Tool.class)).map(Method::getName).toList();
        assertTrue(tools.contains("buy"));
        assertTrue(tools.stream().noneMatch(n -> n.toLowerCase().contains("confirm")),
                "Solo el boton (POST /agent/confirm) puede ejecutar la compra: " + tools);
    }
}
