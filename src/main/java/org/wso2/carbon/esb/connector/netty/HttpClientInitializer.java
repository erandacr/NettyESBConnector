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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import org.wso2.carbon.esb.connector.ProxyServerConfig;

public class HttpClientInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private org.apache.axis2.context.MessageContext messageContext;
    private ProxyServerConfig proxyServerConfig;

    public HttpClientInitializer(SslContext sslCtx, org.apache.axis2.context.MessageContext messageContext,
            ProxyServerConfig proxyServerConfig) {
        this.sslCtx = sslCtx;
        this.messageContext = messageContext;
        this.proxyServerConfig = proxyServerConfig;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();

        if (proxyServerConfig != null) {
            p.addLast(
                    new HttpProxyHandler(proxyServerConfig.getInetSocketAddress(), proxyServerConfig.getProxyUsername(),
                            proxyServerConfig.getProxyPassword()));
        }
        // Enable HTTPS if necessary.
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }

        p.addLast(new HttpClientCodec());

        // Remove the following line if you don't want automatic content decompression.
        p.addLast(new HttpContentDecompressor());

        // Uncomment the following line if you don't want to handle HttpContents.
        p.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));

        p.addLast(new HttpClientHandler(messageContext));
    }
}