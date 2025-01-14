package com.pacvue.segment.event.springboot.configuration;

import com.pacvue.segment.event.client.SegmentEventClientAnalytics;
import com.pacvue.segment.event.client.SegmentEventClientRabbit;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentLogMessage;
import com.pacvue.segment.event.springboot.properties.LoggerProperties;
import com.pacvue.segment.event.springboot.properties.impl.RabbitMQRemoteStoreProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientProperties;
import com.pacvue.segment.event.buffer.RabbitMQDistributedBuffer;
import com.pacvue.segment.event.buffer.Buffer;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.segment.analytics.Analytics;
import com.segment.analytics.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;


@Configuration
@ComponentScan(basePackages = "com.pacvue")
@ConfigurationPropertiesScan(basePackages = {
        "com.pacvue.segment.event.springboot.properties"
})
public class SegmentEventAutoConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = SegmentEventClientProperties.PROPERTIES_PREFIX, name = "secret")
    @ConditionalOnMissingBean
    public Analytics segmentAnalytics(SegmentEventClientProperties properties) {
        Logger log = LoggerFactory.getLogger(Analytics.class);
        return Analytics.builder(properties.getSecret())
                .log(new Log() {
                    @Override
                    public void print(Level level, String format, Object... args) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format(format, args));
                        }
                    }

                    @Override
                    public void print(Level level, Throwable error, String format, Object... args) {
                        log.error(String.format(format, args), error);
                    }
                }).build();
    }

    @Bean
    @ConditionalOnBean(Analytics.class)
    @ConditionalOnMissingBean
    public SegmentEventClientAnalytics segmentEventClientAnalytics(Analytics segmentAnalytics) throws NoSuchFieldException, IllegalAccessException {
        return SegmentEventClientAnalytics.builder().analytics(segmentAnalytics).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientRegistry segmentEventClientRegistry) {
        return SegmentEventReporter.builder().registry(segmentEventClientRegistry).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = LoggerProperties.PROPERTIES_PREFIX, name = "clazz", havingValue = "com.pacvue.segment.event.springboot.properties.impl.RabbitMQRemoteStoreProperties")
    @ConditionalOnMissingBean(name = "eventLogger")
    public SegmentEventClientRabbit eventLogger(LoggerProperties<RabbitMQRemoteStoreProperties> properties) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        RabbitMQRemoteStoreProperties config = properties.getConfig();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(config.getUri());
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(config.getExchangeName(), BuiltinExchangeType.DIRECT, true, false, null);
        channel.queueDeclare(config.getQueueName(), true, false, false, null);
        channel.queueBind(config.getQueueName(), config.getExchangeName(), config.getRoutingKey());

        return SegmentEventClientRabbit.<SegmentLogMessage>builder()
                .channel(channel)
                .exchangeName(config.getExchangeName())
                .routingKey(config.getRoutingKey())
                .queueName(config.getQueueName())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentIO segmentIO(SegmentEventClientProperties properties, SegmentEventReporter segmentEventReporter, SegmentEventClientRabbit eventLogger) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .eventLogger(eventLogger)
                .secret(properties.getSecret())
                .reportApp(properties.getAppId())
                .build();
    }
}
