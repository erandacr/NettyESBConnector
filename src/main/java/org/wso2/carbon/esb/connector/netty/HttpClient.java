/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.esb.connector.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.proxy.ProxyConnectException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.esb.connector.ProxyServerConfig;

import java.net.URI;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Sample Netty Client to handle DELETE request and response
 */
public final class HttpClient {

    private static final Log log = LogFactory.getLog(HttpClient.class);

    public static void sendReceive(URI uri, HttpRequest request, org.apache.axis2.context.MessageContext messageContext,
            ProxyServerConfig proxyServerConfig) throws Exception {

        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            log.error("Invalided port");
            return;
        }

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            log.error("Protocol not supported");
            return;
        }

        // Configure SSL context if necessary.
        final boolean ssl = "https".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        // Configure the client.
        // TODO: make calls blocking instead of non blocking
        EventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new HttpClientInitializer(sslCtx, messageContext, proxyServerConfig));

            // Make the connection attempt.
            Channel ch = b.connect(host, port).sync().channel();

            ch.writeAndFlush(request);

            // Wait for the server to close the connection.
            ch.closeFuture().sync();
        } finally {
            // Shut down executor threads to exit.
            group.shutdownGracefully();
        }
    }

    /**
     * Create the Netty Request using axis2MessageContext
     *
     * @param uri
     * @param httpMethod
     * @param bbuf
     * @param headers
     * @return
     */
    public static HttpRequest createRequest(URI uri, HttpMethod httpMethod, ByteBuf bbuf, TreeMap headers) {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, httpMethod, uri.getRawPath());
        request.headers().set("host", uri.getHost());
        Iterator<Object> iterator = headers.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            request.headers().add(key, headers.get(key));
        }
        request.headers().set("content-length", bbuf.readableBytes());
        request.content().clear().writeBytes(bbuf);

        return request;
    }

}