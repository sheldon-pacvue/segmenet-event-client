package com.pacvue.segment.event.springboot.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.LogStoreProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LogStoreProperties<T> extends AbstractStoreProperties<T> {
    public final static String PROPERTIES_PREFIX = "segment.event.store.log";
}