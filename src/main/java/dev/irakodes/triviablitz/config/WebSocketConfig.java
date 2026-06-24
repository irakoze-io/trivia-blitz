package dev.irakodes.triviablitz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry ser) {
        ser.addEndpoint("/trivia").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry mbr) {
        mbr.enableSimpleBroker("/topic", "/queue");
        mbr.setApplicationDestinationPrefixes("/app");
    }
}
