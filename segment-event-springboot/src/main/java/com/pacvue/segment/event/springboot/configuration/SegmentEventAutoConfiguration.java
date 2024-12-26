package com.pacvue.segment.event.springboot.configuration;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientHttp;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.spring.client.SpringSegmentEventClientRegistry;
import com.pacvue.segment.event.springboot.properties.RabbitMQRemoteStoreProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientFileProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientHttpProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientSocketProperties;
import com.pacvue.segment.event.store.RabbitMQDistributedStore;
import com.pacvue.segment.event.store.Store;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import com.rabbitmq.client.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Configuration
@ConfigurationPropertiesScan(basePackageClasses = {
        SegmentEventClientHttpProperties.class,
        SegmentEventClientSocketProperties.class,
        SegmentEventClientFileProperties.class,
        RabbitMQRemoteStoreProperties.class,
})
public class SegmentEventAutoConfiguration {
    @Bean
    public HttpClient httpClient(SegmentEventClientHttpProperties properties) {
        // 设置 ConnectionProvider 配置
        ConnectionProvider provider = ConnectionProvider.builder("segment-event-client")
                .maxConnections(properties.getMaxConnections())  // 最大连接数
                .maxIdleTime(Duration.ofSeconds(properties.getMaxIdleTime()))  // 最大空闲时间
                .maxLifeTime(Duration.ofSeconds(properties.getMaxLifeTime()))  // 最大生命周期
                .pendingAcquireMaxCount(properties.getPendingAcquireMaxCount())  // 最大并发请求数
                .pendingAcquireTimeout(Duration.ofSeconds(properties.getPendingAcquireTimeout()))  // 获取连接的最大等待时间
                .build();

        // 创建 HttpClient 实例
        return HttpClient.create(provider)
                .baseUrl(properties.getBaseUrl())  // 设置基础 URL
                .responseTimeout(Duration.ofSeconds(properties.getResponseTimeout()))  // 设置响应超时
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectionTimeout())  // 设置连接超时
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeout(), TimeUnit.SECONDS))  // 设置读取超时
                        .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeout(), TimeUnit.SECONDS)));  // 设置写入超时
    }

    @Bean
    public SegmentEventClientHttp segmentEventClientHttp(HttpClient httpClient, SegmentEventClientHttpProperties properties) {
        return SegmentEventClientHttp.builder()
                .httpClient(httpClient)
                .uri(properties.getUri())
                .method(properties.getMethod())
                .retry(properties.getRetry())
                .secret(properties.getSecret()).build();
    }

    @Bean
    public SegmentEventClientRegistry segmentEventClientRegistry(List<? extends SegmentEventClient> clients) {
        return new SpringSegmentEventClientRegistry(clients);
    }

    @Bean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientRegistry segmentEventClientRegistry) {
        return SegmentEventReporter.builder().registry(segmentEventClientRegistry).build();
    }

    @Bean
    @ConditionalOnProperty(value = RabbitMQRemoteStoreProperties.PROPERTIES_PREFIX + ".enabled", havingValue = "true", matchIfMissing = true)
    public RabbitMQDistributedStore rabbitMQRemoteStore(RabbitMQRemoteStoreProperties properties) {
        ConnectionFactory factory = new ConnectionFactory();

        return new RabbitMQDistributedStore(factory, properties.getExchangeName(), properties.getRoutingKey(), properties.getQueueName());
    }

    @Bean
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter, Store remoteStore) {
        return SegmentIO.builder().reporter(segmentEventReporter).distributedStore(remoteStore).build();
    }
}
