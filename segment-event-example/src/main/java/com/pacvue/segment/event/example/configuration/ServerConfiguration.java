package com.pacvue.segment.event.example.configuration;

import com.alibaba.druid.pool.DruidDataSource;
import com.pacvue.segment.event.client.SegmentEventClientFile;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.client.SegmentEventClientSocket;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentPersistingMessage;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.spring.filter.ReactorRequestHolderFilter;
import com.pacvue.segment.event.spring.metrics.SpringPrometheusMetricsCounter;
import com.pacvue.segment.event.springboot.configuration.SegmentEventAutoConfiguration;
import com.pacvue.segment.event.springboot.properties.*;
import com.pacvue.segment.event.store.ClickHouseStore;
import com.pacvue.segment.event.store.RabbitMQDistributedStore;
import com.pacvue.segment.event.store.Store;
import com.pacvue.segment.event.store.ZookeeperMasterElection;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.segment.analytics.messages.Message;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;


@Configuration
@ImportAutoConfiguration({
        SegmentEventAutoConfiguration.class,
})
public class ServerConfiguration {

    @Bean
    public ReactorRequestHolderFilter requestHolderFilter() {
        return new ReactorRequestHolderFilter();
    }

    @Bean
    public SegmentEventClientFile segmentEventClientFile(SegmentEventClientFileProperties properties) {
        return new SegmentEventClientFile(properties.getPath(), properties.getFileName(), properties.getMaxFileSizeMb());
    }

//    @Bean
//    public SegmentEventClientSocket segmentEventClientSocket(SegmentEventClientSocketProperties properties) {
//        return new SegmentEventClientSocket(properties.getHost(), properties.getPort(), properties.getSecret(), properties.getEndPoint());
//    }
//
//    @Bean
//    @ConditionalOnMissingBean
//    public MetricsCounter metricsCounter(MeterRegistry meterRegistry, SegmentEventPrometheusMetricsProperties properties) {
//        return SpringPrometheusMetricsCounter.builder(meterRegistry, properties.getName())
//                .tags(properties.getTags())
//                .build();
//    }
//
//    @Bean
//    public SegmentEventReporter segmentEventReporter(SegmentEventClientRegistry segmentEventClientRegistry, MetricsCounter metricsCounter) {
//        return SegmentEventReporter.builder().registry(segmentEventClientRegistry).metricsCounter(metricsCounter).defaultClientClass(SegmentEventClientSocket.class).build();
//    }
//
//    @Bean
//    @ConditionalOnMissingBean
//    @Qualifier("persistingStore")
//    public Store<SegmentPersistingMessage> persistingStore(ClickHouseStoreProperties properties) throws IOException {
//        DruidDataSource dataSource = new DruidDataSource();
//        dataSource.configFromPropeties(properties.getDataSourceProperties());
//        ClickHouseStore clickHouseStore = new ClickHouseStore(dataSource, properties.getTableName(), properties.getLoopIntervalMinutes())
//                .setMasterElection(new ZookeeperMasterElection("localhost:12181", "/segment/example"));
//        clickHouseStore.createTableIfNotExists();
//        return clickHouseStore;
//    }
//
//
//    @Bean
//    @ConditionalOnMissingBean(name = "distributedStore")
//    @ConditionalOnProperty(value = RabbitMQRemoteStoreProperties.PROPERTIES_PREFIX + ".enabled", havingValue = "true")
//    public Store<Message> distributedStore(RabbitMQRemoteStoreProperties properties) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setUri(properties.getUri());
//        Connection connection = factory.newConnection();
//        Channel channel = connection.createChannel();
//
//        channel.exchangeDeclare(properties.getExchangeName(), BuiltinExchangeType.DIRECT, true, false, null);
//        channel.queueDeclare(properties.getQueueName(), true, false, false, null);
//        channel.queueBind(properties.getQueueName(), properties.getExchangeName(), properties.getRoutingKey());
//
//        return RabbitMQDistributedStore.builder()
//                .connection(connection)
//                .channel(channel)
//                .exchangeName(properties.getExchangeName())
//                .routingKey(properties.getRoutingKey())
//                .queueName(properties.getQueueName())
//                .build();
//    }
//
//    @Bean
//    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter,
//                               @Qualifier("distributedStore") Store<Message> distributedStore,
//                               @Qualifier("persistingStore") Store<SegmentPersistingMessage> persistingStore) {
//        return SegmentIO.builder()
//                .reporter(segmentEventReporter)
//                .distributedStore(distributedStore)
//                .persistingStore(persistingStore)
//                .build().start();
//    }
}
