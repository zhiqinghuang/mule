/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.TopicConnection;

import org.springframework.jms.connection.CachingConnectionFactory;

public class ReconnectInterceptorCachingConnectionFactory extends CachingConnectionFactory
{

    private boolean isReconnecting;

    public ReconnectInterceptorCachingConnectionFactory(ConnectionFactory targetConnectionFactory)
    {
        super(targetConnectionFactory);
    }

    @Override
    public Connection createConnection() throws JMSException
    {
        Connection target = super.createConnection();
        List<Class<?>> classes = new ArrayList<Class<?>>(3);
        classes.add(Connection.class);
        if (target instanceof QueueConnection) {
            classes.add(QueueConnection.class);
        }
        if (target instanceof TopicConnection) {
            classes.add(TopicConnection.class);
        }
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                classes.toArray(new Class<?>[classes.size()]),
                new SharedConnectionInvocationHandler());
    }

    @Override
    public void resetConnection()
    {
        isReconnecting = true;
        try
        {
            super.resetConnection();
        }
        finally
        {
            isReconnecting = false;
        }
    }

    class SharedConnectionInvocationHandler implements InvocationHandler
    {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            if (method.getName().equals("getConnection") && isReconnecting)
            {
                throw new JMSException("Cannot get connection while reconnecting.");
            }

            try {
                return method.invoke(getConnection(), args);
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }
}
