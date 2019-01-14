package org.mule.transport.jms;

import static junit.framework.TestCase.assertTrue;
import org.mule.api.transport.MessageReceiver;
import org.mule.context.notification.ConnectionNotification;
import org.mule.tck.listener.ConnectionListener;

import java.util.Collection;

import org.junit.Test;

public class JmsReconnectForeverWithCachingConnFactoryTestCase extends JmsReconnectForeverTestCase
{

    private ConnectionListener connectionListener;

    @Override
    protected String getConfigFile()
    {
        return "jms-reconnection-with-caching-conn-factory-config.xml";
    }

    @Test
    public void reconnectAllConsumers() throws Exception
    {
        connector = muleContext.getRegistry().lookupObject("activemqconnector");

        Collection<MessageReceiver> receivers = connector.getReceivers().values();
        assertTrue(receivers != null && receivers.size() == 1);

        assertJmsConnectorIsConnected();
        this.assertMessageRouted("put");

        super.stopBroker();

        connectionListener = new ConnectionListener(muleContext)
                .setExpectedAction(ConnectionNotification.CONNECTION_FAILED)
                .setNumberOfExecutionsRequired(1);
        connectionListener.waitUntilNotificationsAreReceived();

        assertTrue(connector.isStopped());

        super.startBroker();

        connectionListener.setExpectedAction(ConnectionNotification.CONNECTION_CONNECTED)
                .setNumberOfExecutionsRequired(1)
                .waitUntilNotificationsAreReceived();

        assertJmsConnectorIsConnected();
        this.assertMessageRouted("put");
    }

}
