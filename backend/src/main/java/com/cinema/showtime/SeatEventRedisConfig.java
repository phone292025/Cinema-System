package com.cinema.showtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class SeatEventRedisConfig {
    @Bean
    RedisMessageListenerContainer seatEventListenerContainer(RedisConnectionFactory connectionFactory, SeatEventPublisher publisher) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(publisher, new ChannelTopic(SeatEventPublisher.CHANNEL));
        return container;
    }
}
