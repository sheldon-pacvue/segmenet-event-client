package com.pacvue.segment.event.springboot.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.pacvue.segment.event.springboot.properties.SegmentEventClientFileProperties.PROPERTIES_PREFIX;

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class SegmentEventClientFileProperties {
    public final static String PROPERTIES_PREFIX = "segment.event.client.file";

    private String path;
    private String fileName;
    // 默认文件大小为100Mb
    private long maxFileSizeMb = 100;
}
