package io.github.irakoze.triviabl.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import io.github.irakoze.triviabl.dto.TickEvent;

@Service
public class CountdownService {

    private final TaskScheduler taskScheduler;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, ScheduledFuture<?>> countdowns = new ConcurrentHashMap<>();

    public CountdownService(TaskScheduler taskScheduler, SimpMessagingTemplate messagingTemplate) {
        this.taskScheduler = taskScheduler;
        this.messagingTemplate = messagingTemplate;
    }

    public void startCountdown(String roomCode, int seconds, Runnable onExpiry) {
        cancelCountdown(roomCode);
        AtomicInteger secondsLeft = new AtomicInteger(seconds);
        messagingTemplate.convertAndSend(topic(roomCode), new TickEvent("TICK", roomCode, secondsLeft.get()));
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
            int remaining = secondsLeft.decrementAndGet();
            if (remaining > 0) {
                messagingTemplate.convertAndSend(topic(roomCode), new TickEvent("TICK", roomCode, remaining));
                return;
            }
            cancelCountdown(roomCode);
            onExpiry.run();
        }, Instant.now().plusSeconds(1), Duration.ofSeconds(1));
        countdowns.put(roomCode, future);
    }

    public void cancelCountdown(String roomCode) {
        ScheduledFuture<?> future = countdowns.remove(roomCode);
        if (future != null) {
            future.cancel(false);
        }
    }

    private String topic(String roomCode) {
        return "/topic/room/" + roomCode;
    }
}
