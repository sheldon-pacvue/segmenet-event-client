package com.pacvue.segment.event.client;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.socket.SocketUtil;
import com.pacvue.segment.event.core.SegmentEvent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import lombok.NonNull;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class SegmentEventClientSocket implements SegmentEventClient {
    // 常量定义
    private static final String DEFAULT_LIB_NAME = "defaultLibrary";
    private static final String DEFAULT_LIB_VERSION = "0.0.0";

    private final Socket socket;
    @NonNull
    private final String secret;

    private final String endPoint;


    public static Socket createSocket(String host, int port, int connectionTimeout) {
        return SocketUtil.connect(host, port, connectionTimeout);
    }

    public SegmentEventClientSocket(Socket socket, String secret, String endPoint) {
        this.socket = socket;
        this.secret = secret;
        this.endPoint = endPoint;
    }


    @Override
    public Mono<Boolean> send(List<SegmentEvent> events) {
        // 向服务端发送消息
        return Mono.fromCallable(() -> {
                    // 在阻塞调用中发送数据
                    IoUtil.writeUtf8(socket.getOutputStream(), false, createMessage(events));
                    return true;
                })
                // 将阻塞调用交由线程池执行
                .subscribeOn(Schedulers.boundedElastic());
    }

    protected String createMessage(List<SegmentEvent> events) {
        // 验证事件列表和内容
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list is empty or null");
        }

        String content = JSONUtil.toJsonStr(events);
        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("Content is empty or null");
        }

        // 获取 library 信息
        String[] libraryInfo = extractLibraryInfo(events);
        String libName = libraryInfo[0];
        String libVersion = libraryInfo[1];

        // 构建请求头
        String host = socket.getInetAddress().getHostAddress();
        String authorization = Base64.getEncoder().encodeToString((this.secret + ":").getBytes(StandardCharsets.UTF_8));

        return HttpMethod.POST + StrUtil.SPACE + endPoint + StrUtil.SPACE + HttpVersion.HTTP_1_1 + "\r\n" +
                HttpHeaderNames.HOST + ": " + host + "\r\n" +
                HttpHeaderNames.CONTENT_TYPE + ": " + HttpHeaderValues.APPLICATION_JSON + "\r\n" +
                HttpHeaderNames.AUTHORIZATION + ": Basic " + authorization + "\r\n" +
                HttpHeaderNames.ACCEPT + ": " + HttpHeaderValues.APPLICATION_JSON + "\r\n" +
                HttpHeaderNames.USER_AGENT + ": " + libName + "/" + libVersion + "\r\n" +
                HttpHeaderNames.CONTENT_LENGTH + ": " + content.length() + "\r\n" +
                "\r\n" +
                content;
    }

    /**
     * 从事件列表中提取 library 的名称和版本信息。
     */
    private String[] extractLibraryInfo(List<SegmentEvent> events) {
        String libName = DEFAULT_LIB_NAME;
        String libVersion = DEFAULT_LIB_VERSION;

        try {
            Map<String, Object> context = events.get(0).getContext();
            if (context != null) {
                Map<String, Object> library = (Map<String, Object>) context.get("library");
                if (library != null) {
                    libName = (String) library.getOrDefault("name", DEFAULT_LIB_NAME);
                    libVersion = (String) library.getOrDefault("version", DEFAULT_LIB_VERSION);
                }
            }
        } catch (Exception ignored) {
        }

        return new String[]{libName, libVersion};
    }
}
