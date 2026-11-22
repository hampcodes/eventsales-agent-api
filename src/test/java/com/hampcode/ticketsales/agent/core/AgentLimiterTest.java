package com.hampcode.ticketsales.agent.core;

import com.hampcode.ticketsales.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentLimiterTest {

    @Test
    void frenaLaRafagaCorta() {
        var limiter = new AgentLimiter();
        var now = new AtomicLong(1_000);
        limiter.clock = now::get;

        for (int i = 0; i < AgentLimiter.BURST_AGENT_CALLS; i++) assertEquals("ok", limiter.runAgent("3", () -> "ok"));
        assertThrows(TooManyRequestsException.class, () -> limiter.runAgent("3", () -> "ok"));
        assertEquals("ok", limiter.runAgent("4", () -> "ok"));

        now.addAndGet(11_000);
        assertEquals("ok", limiter.runAgent("3", () -> "ok"));
    }

    @Test
    void aplicaElLimitePorHoraYLoLiberaAlPasarLaHora() {
        var limiter = new AgentLimiter();
        var now = new AtomicLong(1_000);
        limiter.clock = now::get;

        for (int i = 0; i < AgentLimiter.AGENT_CALLS_PER_HOUR; i++) {
            now.addAndGet(11_000);
            assertEquals("ok", limiter.runAgent("3", () -> "ok"));
        }
        now.addAndGet(11_000);
        assertThrows(TooManyRequestsException.class, () -> limiter.runAgent("3", () -> "ok"));

        now.addAndGet(3_600_000);
        assertEquals("ok", limiter.runAgent("3", () -> "ok"));
    }

    @Test
    void limitaLasLlamadasSimultaneasAlAgente() throws Exception {
        var limiter = new AgentLimiter();
        var started = new CountDownLatch(AgentLimiter.CONCURRENT_AGENT_CALLS);
        var release = new CountDownLatch(1);
        var threads = new Thread[AgentLimiter.CONCURRENT_AGENT_CALLS];
        for (int i = 0; i < threads.length; i++) {
            String user = "u" + i;
            threads[i] = new Thread(() -> limiter.runAgent(user, () -> {
                started.countDown();
                try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
                return "ok";
            }));
            threads[i].start();
        }
        started.await(5, TimeUnit.SECONDS);

        assertThrows(TooManyRequestsException.class, () -> limiter.runAgent("otro", () -> "ok"));

        release.countDown();
        for (var t : threads) t.join();
        assertEquals("ok", limiter.runAgent("otro", () -> "ok"));
    }

    @Test
    void elHistorialTieneUnLimiteMasAmplio() {
        var limiter = new AgentLimiter();
        var now = new AtomicLong(1_000);
        limiter.clock = now::get;
        for (int i = 0; i < AgentLimiter.BURST_HISTORY_CALLS; i++) limiter.checkHistory("3");
        assertThrows(TooManyRequestsException.class, () -> limiter.checkHistory("3"));
        now.addAndGet(11_000);
        limiter.checkHistory("3");
    }
}
