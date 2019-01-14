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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.TopicConnection;

import org.springframework.jms.connection.CachingConnectionFactory;

public class ReconnectInterceptorCachingConnectionFactory extends CachingConnectionFactory
{

    private final ConnectionFactory realTargetConnectionFactory;
    private AtomicBoolean isReconnecting = new AtomicBoolean();

    public ReconnectInterceptorCachingConnectionFactory(ConnectionFactory targetConnectionFactory)
    {
        super(targetConnectionFactory);

        // Since the decorate applyTo methods verifies that the targetConnectionFactory is a CachingConnectionFactory, this
        // methods does not need to be in a try-catch idiom.
        realTargetConnectionFactory = ((CachingConnectionFactory) targetConnectionFactory).getTargetConnectionFactory();

        isReconnecting.set(false);
    }

    @Override
    public ConnectionFactory getTargetConnectionFactory()
    {
        return realTargetConnectionFactory;
    }

    @Override
    public Connection createConnection() throws JMSException
    {
        isReconnecting.set(false);
        return getConnectionProxy(super.createConnection());
    }

    private Connection getConnectionProxy(Connection target)
    {
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
                new SharedConnectionInvocationHandler(target));
    }

    @Override
    public void resetConnection()
    {
        isReconnecting.set(true);
        super.resetConnection();
    }

    class SharedConnectionInvocationHandler implements InvocationHandler
    {

        private Connection proxiedConnection;

        public SharedConnectionInvocationHandler(Connection target)
        {
            proxiedConnection = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            if ((method.getName().equals("getConnection") || method.getName().equals("toString") || method.getName().equals("createSession"))
                && isReconnecting.get())
            {
                throw new JMSException("Cannot get connection while reconnecting.");
            }

            try {
                return method.invoke(proxiedConnection, args);
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }
}
