package com.hampcode.ticketsales.agent.core;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Filtro previo al enrutador: pedidos de canciones, poemas, código, traducciones o JSON pegado van directo al agente sin tools. */
final class InputGuard {

    private static final Pattern OFF_TOPIC = Pattern.compile(
            "\\b(cancion(es)?|poema(s)?|poesia|rima(s|do|dos|da|das)?|verso(s)?|estrofa(s)?|soneto|haiku|rap|cuento(s)?|letra de|"
            + "codigo|script|python|java|javascript|sql|programa(r|cion)?|algoritmo|funcion en|traduc\\w*|en ingles|in english|"
            + "poem|song|lyrics|rhym\\w*|json|prompt|jailbreak)\\b");

    private InputGuard() {}

    static boolean isOffTopic(String message) {
        if (message == null) return false;
        String plain = Normalizer.normalize(message, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        return OFF_TOPIC.matcher(plain).find();
    }
}
