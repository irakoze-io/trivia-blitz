package dev.irakodes.triviablitz.service;

import dev.irakodes.triviablitz.event.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CountdownService {

    private final static Logger log = LoggerFactory
            .getLogger(CountdownService.class);

    private final TaskScheduler taskScheduler;
    private final SimpMessagingTemplate msgTemplate;
    private final Map<String, ScheduledFuture<?>> countdowns = new ConcurrentHashMap<>();

    public CountdownService(@Qualifier("taskScheduler") TaskScheduler taskScheduler, SimpMessagingTemplate msgTemplate) {
        this.taskScheduler = taskScheduler;
        this.msgTemplate = msgTemplate;
    }

    public void startCountdown(String roomCode, int seconds, Runnable onExpiry) {
        cancelCountdown(roomCode);
        var secondsLeft = new AtomicInteger(seconds);
        /* var future = taskScheduler.scheduleAtFixedRate(() -> {
            int remaining = secondsLeft.getAndDecrement();
            if (remaining >= 0) {
                msgTemplate.convertAndSend("/topic/countdown/" + roomCode, remaining);
            } else {
                onExpiry.run();
                cancelCountdown(roomCode);
            }
        }, 0, 1, TimeUnit.SECONDS);
        countdowns.put(roomCode, future); */

        msgTemplate.convertAndSend(topic(roomCode), new TickEvent("TICK", roomCode, secondsLeft.get()));
        var future = taskScheduler.scheduleAtFixedRate(() -> {
            var remaining = secondsLeft.decrementAndGet();
            if (remaining > 0) {
                msgTemplate.convertAndSend(topic(roomCode), new TickEvent("TICK", roomCode, remaining));
                return;
            }

            cancelCountdown(roomCode);
            onExpiry.run();
        }, Instant.now().plusSeconds(1), Duration.ofSeconds(1));
        countdowns.put(roomCode, future);
    }

    public void cancelCountdown(String roomCode) {
        var future = countdowns.remove(roomCode);
        if (future != null) {
            future.cancel(false);
        }
    }

    private String topic(String roomCode) {
        return "/topic/room/" + roomCode;
    }
}