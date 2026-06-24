package dev.irakodes.triviablitz.config;

import dev.irakodes.triviablitz.utils.UuidGenerator;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(WebSocketConfig.class);

    @Override
    public void registerStompEndpoints(StompEndpointRegistry ser) {
        ser.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new DefaultHandshakeHandler() {

                    @Override
                    protected @NonNull Principal determineUser(@NonNull ServerHttpRequest request,
                                                               @NonNull WebSocketHandler wsHandler,
                                                               @NonNull Map<String, Object> attributes) {
                        return () -> UuidGenerator.newId().toString();
                    }
                }).withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry mbr) {
        mbr.setApplicationDestinationPrefixes("/app");
        mbr.enableSimpleBroker("/topic", "/queue", "/user");
        mbr.setUserDestinationPrefix("/user");
    }

    @OnError
    private void onError(WebSocketSession session, Exception exception) {
        log.error("WebSocket error", exception);
    }

    @OnClose
    private void onClose(WebSocketSession session) {
        log.info("WebSocket closed");
    }

    @Bean
    public TaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("trivia-blitz-");
        scheduler.initialize();

        return scheduler;
    }
}
