package com.be_hase.grpc.micrometer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.grpc.Metadata;
import org.junit.Before;
import org.junit.Test;

import io.grpc.MethodDescriptor.MethodType;
import io.grpc.Status;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class GrpcMetricsTest {
    private GrpcMetrics target;
    private GrpcMetrics targetWithDynamicTags;

    private MeterRegistry meterRegistry;
    private GrpcMethod grpcMethod;
    private final Metadata.AsciiMarshaller<String> keyMarshaller = Metadata.ASCII_STRING_MARSHALLER;

    @Before
    public void before() {
        meterRegistry = new SimpleMeterRegistry();
        grpcMethod = mock(GrpcMethod.class);
        doReturn("serviceName").when(grpcMethod).getServiceName();
        doReturn("methodName").when(grpcMethod).getMethodName();
        doReturn(MethodType.UNARY).when(grpcMethod).getMethodType();

        target = new GrpcMetrics(meterRegistry, "test", GrpcMetricsConfigure.create(), grpcMethod);
        targetWithDynamicTags = new GrpcMetrics(meterRegistry, "test", GrpcMetricsConfigure.create(), grpcMethod, true);
    }

    @Test
    public void recordLatency() {
        // when
        target.recordLatency(Status.OK, 100, TimeUnit.MILLISECONDS);
        target.recordLatency(Status.OK, 200, TimeUnit.MILLISECONDS);

        // then
        final Timer timer = meterRegistry.timer("test.requests", Arrays.asList(
                Tag.of("service", "serviceName"),
                Tag.of("method", "methodName"),
                Tag.of("methodType", "UNARY"),
                Tag.of("statusCode", "OK")
        ));
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.max(TimeUnit.SECONDS)).isEqualTo(0.2);
        assertThat(timer.mean(TimeUnit.SECONDS)).isEqualTo(0.15);
    }

    @Test
    public void recordLatencyWithMetadata() {
        Metadata testMetadata = new Metadata();
        testMetadata.put(Metadata.Key.of("testTagKey", keyMarshaller), "tagValue");

        // when
        targetWithDynamicTags.recordLatency(Status.OK, testMetadata, 100, TimeUnit.MILLISECONDS);
        targetWithDynamicTags.recordLatency(Status.OK, testMetadata, 400, TimeUnit.MILLISECONDS);

        // then
        final Timer timer = meterRegistry.timer("test.requests", Arrays.asList(
                Tag.of("testTagKey".toLowerCase(), "tagValue"),
                Tag.of("service", "serviceName"),
                Tag.of("method", "methodName"),
                Tag.of("methodType", "UNARY"),
                Tag.of("statusCode", "OK")
        ));
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.max(TimeUnit.SECONDS)).isEqualTo(0.4);
        assertThat(timer.mean(TimeUnit.SECONDS)).isEqualTo(0.25);
    }

    @Test
    public void incrementStreamMessagesReceived() {
        // when
        target.incrementStreamMessagesReceived();
        target.incrementStreamMessagesReceived();

        // then
        final Counter counter = meterRegistry.counter("test.stream.messages.received", Arrays.asList(
                Tag.of("service", "serviceName"),
                Tag.of("method", "methodName"),
                Tag.of("methodType", "UNARY")
        ));
        assertThat(counter.count()).isEqualTo(2);
    }

    @Test
    public void incrementStreamMessagesSent() {
        // when
        target.incrementStreamMessagesSent();
        target.incrementStreamMessagesSent();

        // then
        final Counter counter = meterRegistry.counter("test.stream.messages.sent", Arrays.asList(
                Tag.of("service", "serviceName"),
                Tag.of("method", "methodName"),
                Tag.of("methodType", "UNARY")
        ));
        assertThat(counter.count()).isEqualTo(2);
    }
}
