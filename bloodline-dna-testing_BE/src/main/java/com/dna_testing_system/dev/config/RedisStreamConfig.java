package com.dna_testing_system.dev.config;

import com.dna_testing_system.dev.constant.StreamConstants;
import com.dna_testing_system.dev.event.consumer.MailEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.time.Duration;

/**
 * Configures the Redis Stream listener container and initialises the consumer group.
 * Uses the MKSTREAM flag so the stream is created automatically on startup.
 */
@Slf4j
@Configuration
public class RedisStreamConfig {

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> mailStreamListenerContainer(
            RedisConnectionFactory connectionFactory,
            StringRedisTemplate redisTemplate,
            MailEventConsumer mailEventConsumer) {

        initConsumerGroup(redisTemplate);

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(200))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        container.receiveAutoAck(
                Consumer.from(StreamConstants.MAIL_CONSUMER_GROUP, StreamConstants.MAIL_CONSUMER_NAME),
                StreamOffset.create(StreamConstants.NOTIFICATION_STREAM, ReadOffset.lastConsumed()),
                mailEventConsumer::consume);

        container.start();
        return container;
    }

    /**
     * Creates the stream and consumer group if they do not already exist.
     * MKSTREAM ensures the stream is created even before the first message is published.
     */
    private void initConsumerGroup(StringRedisTemplate redisTemplate) {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.execute("XGROUP",
                        "CREATE".getBytes(),
                        StreamConstants.NOTIFICATION_STREAM.getBytes(),
                        StreamConstants.MAIL_CONSUMER_GROUP.getBytes(),
                        "0".getBytes(),
                        "MKSTREAM".getBytes());
                return null;
            });
            log.info("Initialised Redis stream '{}' with consumer group '{}'",
                    StreamConstants.NOTIFICATION_STREAM, StreamConstants.MAIL_CONSUMER_GROUP);
        } catch (Exception e) {
            log.warn("Consumer group '{}' already exists or stream initialisation skipped: {}",
                    StreamConstants.MAIL_CONSUMER_GROUP, e.getMessage());
        }
    }
}
