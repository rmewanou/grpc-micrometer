package com.be_hase.grpc.micrometer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.Metadata;
import io.grpc.Status;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

class GrpcMetrics {

    private final MeterRegistry meterRegistry;
    private final String metricsName;
    private final GrpcMetricsConfigure metricsConfigure;
    private final GrpcMethod grpcMethod;
    private final Map<String, String> dynamicTags;

    GrpcMetrics(MeterRegistry meterRegistry,
                String metricsName,
                GrpcMetricsConfigure metricsConfigure,
                GrpcMethod grpcMethod) {
        this(meterRegistry, metricsName, metricsConfigure, grpcMethod, null);
    }
    GrpcMetrics(MeterRegistry meterRegistry,
                String metricsName,
                GrpcMetricsConfigure metricsConfigure,
                GrpcMethod grpcMethod,
                Map<String, String> dynamicTags) {
        this.meterRegistry = meterRegistry;
        this.metricsName = metricsName;
        this.metricsConfigure = metricsConfigure;
        this.grpcMethod = grpcMethod;
        this.dynamicTags = dynamicTags;
    }

    void recordLatency(Status status, long amount, TimeUnit unit) {
        recordLatency(status, null, amount, unit);
    }

    void recordLatency(Status status, Metadata trailers, long amount, TimeUnit unit) {
        latencyTimer(status, trailers).record(amount, unit);
    }

    void incrementStreamMessagesReceived() {
        incrementStreamMessagesReceived(null);
    }

    void incrementStreamMessagesReceived(Metadata metadata) {
        streamMessagesReceivedCounter(metadata).increment();
    }

    void incrementStreamMessagesSent() {
        streamMessagesSentCounter().increment();
    }

    private Timer latencyTimer(Status status, Metadata metadata) {
        final Timer.Builder latencyTimerBuilder =
                Timer.builder(metricsName + ".requests")
                     .description("Response latency(seconds) of gRPC.")
                     .tags(getTags())
                     .tag("statusCode", status.getCode().name());
        metricsConfigure.latencyTimerConfigure.accept(latencyTimerBuilder);
        return latencyTimerBuilder.register(meterRegistry);
    }

    private Counter streamMessagesReceivedCounter(Metadata metadata) {
        final Counter.Builder streamMessagesReceivedCounterBuilder =
                Counter.builder(metricsName + ".stream.messages.received")
                       .description("Total number of stream messages received.")
                       .tags(getTags());
        metricsConfigure.streamMessagesReceivedCounterConfigure.accept(streamMessagesReceivedCounterBuilder);
        return streamMessagesReceivedCounterBuilder.register(meterRegistry);
    }

    private Counter streamMessagesSentCounter() {
        final Counter.Builder streamMessagesSentCounterBuilder =
                Counter.builder(metricsName + ".stream.messages.sent")
                        .description("Total number of stream messages sent.")
                        .tags(getTags());
        metricsConfigure.streamMessagesSentCounterConfigure.accept(streamMessagesSentCounterBuilder);
        return streamMessagesSentCounterBuilder.register(meterRegistry);
    }

    private Iterable<Tag> getTags() {
        List<Tag> tags = new ArrayList<>();
        for (Tag tag : GrpcMethodTagProvider.tags(grpcMethod)) {
            tags.add(tag);
        }

        if (dynamicTags != null) {
            for (String key : dynamicTags.keySet()) {
                tags.add(Tag.of(key, dynamicTags.get(key)));
            }
        }
        return tags;
    }
}
