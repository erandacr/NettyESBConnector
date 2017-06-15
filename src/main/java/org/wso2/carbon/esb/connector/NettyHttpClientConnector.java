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
package org.wso2.carbon.esb.connector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.esb.connector.netty.HttpClient;

import java.io.OutputStream;
import java.net.URI;
import java.util.TreeMap;

/**
 * Sample method implementation.
 */
public class NettyHttpClientConnector extends AbstractConnector {

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        log.debug("Invoking the Netty Client connector");
        Object address = getParameter(messageContext, "address");

        //proxy server
        ProxyServerConfig proxyServerConfig = null;
        Object proxyHost = getParameter(messageContext, "proxyServer.host");
        Object proxyPort = getParameter(messageContext, "proxyServer.port");


        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();

        try {

            // Creating the proxy server
            if (proxyHost != null && proxyPort != null) {
                Object proxyUsername = getParameter(messageContext, "proxyServer.username");
                Object proxyPassword = getParameter(messageContext, "proxyServer.password");
                int port = Integer.parseInt((String)proxyPort);
                proxyServerConfig = new ProxyServerConfig((String)proxyHost, port);
                proxyServerConfig.setProxyUsername((String)proxyUsername);
                proxyServerConfig.setProxyPassword((String)proxyPassword);
            }

            if (axis2MessageContext.getProperty("HTTP_METHOD") != null && "DELETE"
                    .equalsIgnoreCase((String) axis2MessageContext.getProperty("HTTP_METHOD"))) {
                // Preparing the message to be sent
                MessageFormatter messageFormatter;
                messageFormatter = MessageProcessorSelector.getMessageFormatter(axis2MessageContext);
                OMOutputFormat format = BaseUtils.getOMOutputFormat(axis2MessageContext);

                // Netty Processing
                ByteBuf byteBuf = Unpooled.buffer();
                OutputStream out = new ByteBufOutputStream(byteBuf);
                messageFormatter.writeTo(axis2MessageContext, format, out, true);
                URI uri = new URI((String) address);
                HttpClient.sendReceive(uri, HttpClient.createRequest(uri, HttpMethod.DELETE, byteBuf,
                        (TreeMap) axis2MessageContext.getProperty("TRANSPORT_HEADERS")), axis2MessageContext, proxyServerConfig);

                log.debug(
                        "Netty Connector processed the request " + messageContext.getMessageID() + ", To: " + address);
            } else {
                log.error("HTTP_METHOD is not set to DELETE");
            }

        } catch (Exception e) {
            throw new ConnectException(e);
        }
    }
}