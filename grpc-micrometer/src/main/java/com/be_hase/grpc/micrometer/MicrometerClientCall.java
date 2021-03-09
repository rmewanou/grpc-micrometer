package com.be_hase.grpc.micrometer;

import io.grpc.ClientCall;
import io.grpc.Context;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;

class MicrometerClientCall<ReqT, RespT>
        extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {
    private final GrpcMetrics grpcMetrics;
    private final GrpcMethod grpcMethod;
    private final boolean dynamicTags;

    MicrometerClientCall(ClientCall<ReqT, RespT> delegate,
                         GrpcMetrics grpcMetrics,
                         GrpcMethod grpcMethod) {
        this(delegate, grpcMetrics, grpcMethod, null);
    }

    MicrometerClientCall(ClientCall<ReqT, RespT> delegate,
                         GrpcMetrics grpcMetrics,
                         GrpcMethod grpcMethod,
                         Boolean dynamicTags) {
        super(delegate);

        this.grpcMetrics = grpcMetrics;
        this.grpcMethod = grpcMethod;
        this.dynamicTags = dynamicTags != null && dynamicTags;
    }

    @Override
    public void start(Listener<RespT> responseListener, Metadata headers) {
        if (dynamicTags) {
            super.start(new MicrometerClientCallListener<>(responseListener, grpcMetrics, grpcMethod, headers, true), headers);
        } else {
            super.start(new MicrometerClientCallListener<>(responseListener, grpcMetrics, grpcMethod), headers);
        }
    }

    @Override
    public void sendMessage(ReqT message) {
        super.sendMessage(message);
        if (grpcMethod.isStreamsRequests()) {
            grpcMetrics.incrementStreamMessagesSent();
        }
    }
}
