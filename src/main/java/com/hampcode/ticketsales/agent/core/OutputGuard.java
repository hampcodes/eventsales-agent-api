package com.hampcode.ticketsales.agent.core;

/** Última barrera en código: el agente no es de programación, así que no se devuelve código aunque el modelo lo escriba. */
final class OutputGuard {

    static final String NO_CODE = "Solo puedo ayudarte con eventos y entradas, no con código. ¿Quieres que busque algún evento para ti?";

    private OutputGuard() {}

    static String clean(String reply) {
        return reply != null && reply.contains("```") ? NO_CODE : reply;
    }
}
