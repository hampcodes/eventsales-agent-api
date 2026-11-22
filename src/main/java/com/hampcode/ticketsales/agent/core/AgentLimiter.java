package com.hampcode.ticketsales.agent.core;

import com.hampcode.ticketsales.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Protege la base y a OpenAI de ráfagas (por ejemplo un script que lanza muchas peticiones en paralelo):
 * limita las peticiones por usuario y por hora (más una guarda corta contra ráfagas) y cuántas llamadas al agente corren a la vez.
 * El exceso recibe 429 de inmediato, sin tocar la base de datos.
 */
@Component
public class AgentLimiter {

    static final int AGENT_CALLS_PER_HOUR = 60;
    static final int HISTORY_CALLS_PER_HOUR = 300;
    static final int BURST_AGENT_CALLS = 5;
    static final int BURST_HISTORY_CALLS = 20;
    static final int CONCURRENT_AGENT_CALLS = 3;
    private static final long HOUR_MS = 3_600_000;
    private static final long BURST_MS = 10_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final Semaphore concurrent = new Semaphore(CONCURRENT_AGENT_CALLS);
    LongSupplier clock = System::currentTimeMillis;

    /** Una llamada al agente (chat o voz): cuenta contra el límite por minuto y ocupa un cupo mientras se ejecuta. */
    public <T> T runAgent(String user, Supplier<T> task) {
        allow("agent:" + user, AGENT_CALLS_PER_HOUR, BURST_AGENT_CALLS);
        if (!concurrent.tryAcquire()) {
            throw new TooManyRequestsException("El agente está atendiendo muchas solicitudes. Intenta de nuevo en unos segundos.");
        }
        try {
            return task.get();
        } finally {
            concurrent.release();
        }
    }

    /** Consultas de historial: límite más amplio. */
    public void checkHistory(String user) {
        allow("history:" + user, HISTORY_CALLS_PER_HOUR, BURST_HISTORY_CALLS);
    }

    private void allow(String key, int perHour, int burst) {
        long now = clock.getAsLong();
        Deque<Long> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() >= HOUR_MS) times.pollFirst();
            if (times.size() >= perHour) {
                throw new TooManyRequestsException("Llegaste al límite de consultas por hora. Intenta de nuevo más tarde.");
            }
            long recent = times.stream().filter(time -> now - time < BURST_MS).count();
            if (recent >= burst) {
                throw new TooManyRequestsException("Demasiadas solicitudes seguidas. Espera unos segundos e intenta de nuevo.");
            }
            times.addLast(now);
        }
    }
}
