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

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.transport.TransportUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.TreeMap;

public class HttpClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Log log = LogFactory.getLog(HttpClientHandler.class);

    private org.apache.axis2.context.MessageContext messageContext;

    public HttpClientHandler(org.apache.axis2.context.MessageContext messageContext) {
        this.messageContext = messageContext;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof FullHttpResponse) {
            ((TreeMap) (messageContext.getProperty("TRANSPORT_HEADERS"))).clear();
            messageContext.setProperty("HTTP_SC", ((FullHttpResponse) msg).getStatus().code());

            HttpHeaders headers = ((FullHttpResponse) msg).headers();

            if (!headers.isEmpty()) {
                for (CharSequence name : headers.names()) {
                    for (CharSequence value : headers.getAll(name)) {
                        ((TreeMap) (messageContext.getProperty("TRANSPORT_HEADERS")))
                                .put(name.toString(), value.toString());
                    }
                }
            }
            buildMessage((FullHttpResponse) msg);
            log.debug("Response received at the Netty connector");
            ctx.close();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error on Netty Connector", cause);
        ctx.close();
    }

    private void buildMessage(FullHttpResponse httpResponse) {
        String contentType = (String) ((TreeMap) messageContext.getProperty("TRANSPORT_HEADERS")).get("Content-Type");
        //Default content-type
        if (contentType == null) contentType = "application/json";
        int index = contentType.indexOf(';');
        String type = index > 0 ? contentType.substring(0, index) : contentType;
        Builder builder;
        try {
            builder = BuilderUtil.getBuilderFromSelector(type, messageContext);
            OMElement documentElement = builder
                    .processDocument(new ByteBufInputStream(httpResponse.content()), contentType, messageContext);
            messageContext.setEnvelope(TransportUtils.createSOAPEnvelope(documentElement));
        } catch (AxisFault axisFault) {
            log.error("Message builder can not be found for " + contentType);
        }

    }
}