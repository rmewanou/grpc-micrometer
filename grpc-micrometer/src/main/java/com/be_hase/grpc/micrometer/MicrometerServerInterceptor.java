package com.be_hase.grpc.micrometer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.micrometer.core.instrument.MeterRegistry;

public class MicrometerServerInterceptor implements ServerInterceptor {
    private static final String DEFAULT_METRICS_NAME = "grpc.server";

    private final MeterRegistry meterRegistry;
    private final String metricsName;
    private final GrpcMetricsConfigure metricsConfigure;
    private final boolean dynamicTags;
    private final List<String> includeTagKeys;
    private final Metadata.AsciiMarshaller<String> keyMarshaller = Metadata.ASCII_STRING_MARSHALLER;


    /**
     * @param meterRegistry
     */
    public MicrometerServerInterceptor(MeterRegistry meterRegistry) {
        this(meterRegistry, DEFAULT_METRICS_NAME, false);
    }

    /**
     * @param meterRegistry
     * @param dynamicTags
     */
    public MicrometerServerInterceptor(MeterRegistry meterRegistry, Boolean dynamicTags, List<String> includeTagKeys) {
        this(meterRegistry, DEFAULT_METRICS_NAME, GrpcMetricsConfigure.create(), dynamicTags != null && dynamicTags, includeTagKeys);
    }

    /**
     * @param meterRegistry
     * @param metricsName
     */
    public MicrometerServerInterceptor(MeterRegistry meterRegistry, String metricsName, Boolean dynamicTags) {
        this(meterRegistry, metricsName, GrpcMetricsConfigure.create(), dynamicTags != null && dynamicTags);
    }

    /**
     * @param meterRegistry
     * @param metricsConfigure
     */
    public MicrometerServerInterceptor(MeterRegistry meterRegistry, GrpcMetricsConfigure metricsConfigure, Boolean dynamicTags) {
        this(meterRegistry, DEFAULT_METRICS_NAME, metricsConfigure, dynamicTags != null && dynamicTags);
    }

    /**
     * @param meterRegistry
     * @param metricsName
     * @param metricsConfigure
     */
    public MicrometerServerInterceptor(MeterRegistry meterRegistry,
                                       String metricsName,
                                       GrpcMetricsConfigure metricsConfigure) {
        this(meterRegistry, metricsName, metricsConfigure, false);
    }

    /**
     * @param meterRegistry
     * @param metricsName
     * @param metricsConfigure
     */
    public MicrometerServerInterceptor(MeterRegistry meterRegistry,
                                       String metricsName,
                                       GrpcMetricsConfigure metricsConfigure,
                                       Boolean dynamicTags) {
        this(meterRegistry, metricsName, metricsConfigure, dynamicTags, null);
    }

    public MicrometerServerInterceptor(MeterRegistry meterRegistry,
                                       String metricsName,
                                       GrpcMetricsConfigure metricsConfigure,
                                       Boolean dynamicTags,
                                       List<String> includeTagKeys) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.metricsName = Objects.requireNonNull(metricsName, "metricsName");
        this.metricsConfigure = Objects.requireNonNull(metricsConfigure, "metricsConfigure");
        this.dynamicTags = dynamicTags != null && dynamicTags;
        this.includeTagKeys = includeTagKeys;
    }

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                      ServerCallHandler<ReqT, RespT> next) {
        final GrpcMethod grpcMethod = GrpcMethod.of(call.getMethodDescriptor());
        GrpcMetrics grpcMetrics;
        if (dynamicTags && includeTagKeys != null) {
            Map<String, String> tagMap = new HashMap<>();
            for (String keyString: includeTagKeys) {
                Metadata.Key<String> key = Metadata.Key.of(keyString, keyMarshaller);
                if (headers.containsKey(key)) {
                    tagMap.put(keyString, headers.get(key));
                }
            }
            grpcMetrics = new GrpcMetrics(meterRegistry, metricsName, metricsConfigure, grpcMethod, tagMap);
        } else {
            grpcMetrics = new GrpcMetrics(meterRegistry, metricsName, metricsConfigure, grpcMethod);
        }
        final MicrometerServerCall<ReqT, RespT> micrometerCall =
                new MicrometerServerCall<>(call, grpcMetrics, grpcMethod);
        return new MicrometerServerCallListener<>(
                next.startCall(micrometerCall, headers), grpcMetrics, grpcMethod);
    }
}
