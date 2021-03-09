package com.be_hase.grpc.micrometer;

import java.util.Objects;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import io.micrometer.core.instrument.MeterRegistry;

public class MicrometerClientInterceptor implements ClientInterceptor {
    private static final String DEFAULT_METRICS_NAME = "grpc.client";

    private final MeterRegistry meterRegistry;
    private final String metricsName;
    private final GrpcMetricsConfigure metricsConfigure;
    private final boolean dynamicTags;

    /**
     * @param meterRegistry
     */
    public MicrometerClientInterceptor(MeterRegistry meterRegistry) {
        this(meterRegistry, DEFAULT_METRICS_NAME, GrpcMetricsConfigure.create(), null);
    }

    /**
     * @param meterRegistry
     */
    public MicrometerClientInterceptor(MeterRegistry meterRegistry, Boolean dynamicTags) {
        this(meterRegistry, DEFAULT_METRICS_NAME, dynamicTags != null && dynamicTags);
    }

    /**
     * @param meterRegistry
     * @param metricsName
     */
    public MicrometerClientInterceptor(MeterRegistry meterRegistry, String metricsName, Boolean dynamicTags) {
        this(meterRegistry, metricsName, GrpcMetricsConfigure.create(), dynamicTags != null && dynamicTags);
    }

    /**
     * @param meterRegistry
     * @param metricsConfigure
     */
    public MicrometerClientInterceptor(MeterRegistry meterRegistry, GrpcMetricsConfigure metricsConfigure, Boolean dynamicTags) {
        this(meterRegistry, DEFAULT_METRICS_NAME, metricsConfigure, dynamicTags != null && dynamicTags);
    }

    /**
     * @param meterRegistry
     * @param metricsName
     * @param metricsConfigure
     */
    public MicrometerClientInterceptor(MeterRegistry meterRegistry,
                                       String metricsName,
                                       GrpcMetricsConfigure metricsConfigure) {
        this(meterRegistry, metricsName, metricsConfigure, null);
    }
    /**
     * @param meterRegistry
     * @param metricsName
     * @param metricsConfigure
     */
    public MicrometerClientInterceptor(MeterRegistry meterRegistry,
                                       String metricsName,
                                       GrpcMetricsConfigure metricsConfigure,
                                       Boolean dynamicTags) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.metricsName = Objects.requireNonNull(metricsName, "metricsName");
        this.metricsConfigure = Objects.requireNonNull(metricsConfigure, "metricsConfigure");
        this.dynamicTags = dynamicTags != null && dynamicTags;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        final GrpcMethod grpcMethod = GrpcMethod.of(method);
        final GrpcMetrics grpcMetrics = new GrpcMetrics(meterRegistry, metricsName, metricsConfigure, grpcMethod);
//        final GrpcMetrics grpcMetrics = new GrpcMetrics(meterRegistry, metricsName, metricsConfigure, grpcMethod, dynamicTags);
        return new MicrometerClientCall<>(next.newCall(method, callOptions), grpcMetrics, grpcMethod, dynamicTags);
    }
}
