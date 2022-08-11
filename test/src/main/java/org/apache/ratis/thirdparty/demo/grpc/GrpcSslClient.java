/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ozone.thirdparty.demo.grpc;

import org.apache.ozone.thirdparty.demo.netty.NettyUtils;
import org.apache.ozone.thirdparty.demo.proto.GreeterGrpc;
import org.apache.ozone.thirdparty.demo.proto.HelloReply;
import org.apache.ozone.thirdparty.demo.proto.HelloRequest;
import org.apache.ozone.thirdparty.demo.common.SslClientConfig;
import org.apache.ozone.thirdparty.io.grpc.ManagedChannel;
import org.apache.ozone.thirdparty.io.grpc.StatusRuntimeException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.ozone.thirdparty.io.grpc.netty.NettyChannelBuilder;
import org.apache.ozone.thirdparty.io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC Demo SSL client with shaded ozone thirdparty jar.
 */
public class GrpcSslClient {
  private static final Logger LOG = LoggerFactory.getLogger(GrpcSslClient.class);

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  public GrpcSslClient(String host, int port, SslClientConfig conf) throws IOException {
    channel = initChannel(host, port, conf);
    blockingStub = GreeterGrpc.newBlockingStub(channel);
  }

  private ManagedChannel initChannel(String host, int port, SslClientConfig conf) throws IOException {
    NettyChannelBuilder channelBuilder =
        NettyChannelBuilder.forAddress(host, port);
    // Hacky way to work around hostname verify of the certificate
    //channelBuilder.overrideAuthority(
    //    InetAddress.getLocalHost().getHostName()) ;
    SslContextBuilder sslContextBuilder = NettyUtils.newClientSslContextBuilder(conf);
    return channelBuilder.useTransportSecurity()
        .sslContext(sslContextBuilder.build()).build();
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public String greet(String name) {
    HelloRequest request = HelloRequest.newBuilder().setName(name).build();
    HelloReply response;
    try {
      response = blockingStub.hello(request);
      LOG.trace("Greeting: " + response.getMessage());
      return response.getMessage();
    } catch (StatusRuntimeException e) {
      LOG.warn("RPC failed: " + e.getStatus(), e);
      return "";
    }
  }
}
