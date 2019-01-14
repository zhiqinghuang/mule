/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.mule.api.MuleContext;

import javax.jms.ConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jms.connection.CachingConnectionFactory;

public class ReconnectInterceptorCachingConnectionFactoryDecorator implements ConnectionFactoryDecorator
{

    private final Log logger = LogFactory.getLog(getClass());

    private ReconnectInterceptorCachingConnectionFactory reconnectInterceptorCachingConnectionFactory;

    @Override
    public ConnectionFactory decorate(ConnectionFactory connectionFactory, JmsConnector jmsConnector, MuleContext mulecontext)
    {
        logger.info("Decorating JMS connection factory with " + getClass().getName());
        reconnectInterceptorCachingConnectionFactory = new ReconnectInterceptorCachingConnectionFactory(connectionFactory);
        return reconnectInterceptorCachingConnectionFactory;
    }

    @Override
    public boolean appliesTo(ConnectionFactory connectionFactory, MuleContext muleContext)
    {
        return connectionFactory instanceof CachingConnectionFactory;
    }
}
