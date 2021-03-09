package com.be_hase.grpc.micrometer;

import java.util.concurrent.TimeUnit;

import io.grpc.ClientCall.Listener;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.Status;

class MicrometerClientCallListener<RespT>
        extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {
    private final GrpcMetrics grpcMetrics;
    private final GrpcMethod grpcMethod;
    private final long start;
    private final Metadata headers;
    private final boolean dynamicTags;

    MicrometerClientCallListener(Listener<RespT> delegate,
                                 GrpcMetrics grpcMetrics,
                                 GrpcMethod grpcMethod) {
        this(delegate, grpcMetrics, grpcMethod, null, null);
    }
    MicrometerClientCallListener(Listener<RespT> delegate,
                                 GrpcMetrics grpcMetrics,
                                 GrpcMethod grpcMethod,
                                 Metadata headers) {
        this(delegate, grpcMetrics, grpcMethod, headers, null);
    }

    MicrometerClientCallListener(Listener<RespT> delegate,
                                 GrpcMetrics grpcMetrics,
                                 GrpcMethod grpcMethod,
                                 Metadata headers,
                                 Boolean dynamicTags) {
        super(delegate);

        this.grpcMetrics = grpcMetrics;
        this.grpcMethod = grpcMethod;
        this.headers = headers;
        this.dynamicTags = dynamicTags != null && dynamicTags;
        start = System.nanoTime();
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
        super.onClose(status, trailers);
        grpcMetrics.recordLatency(status, trailers,System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }

    @Override
    public void onMessage(RespT message) {
        super.onMessage(message);
        if (grpcMethod.isStreamsResponses()) {
            grpcMetrics.incrementStreamMessagesReceived(headers);
        }
    }
}
