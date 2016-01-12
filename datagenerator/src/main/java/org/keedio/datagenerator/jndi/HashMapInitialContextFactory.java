package org.keedio.datagenerator.jndi;

import javax.jms.ConnectionFactory;
import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;
import org.apache.qpid.amqp_1_0.jms.impl.DestinationImpl;
import org.apache.qpid.amqp_1_0.jms.impl.QueueImpl;
import org.apache.qpid.amqp_1_0.jms.impl.TopicImpl;
import org.apache.qpid.amqp_1_0.jms.jndi.ReadOnlyContext;

import javax.naming.Context;
import javax.naming.NamingException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 12/1/16.
 */
public class HashMapInitialContextFactory {
  public static final String CONNECTION_FACTORY_PREFIX = "event.hubs.connectionfactory.url.";
  public static final String DESTINATION_PREFIX = "event.hubs.destination.";
  public static final String QUEUE_PREFIX = "queue.";
  public static final String TOPIC_PREFIX = "topic.";


  public Context getInitialContext(Hashtable environment) throws NamingException
  {
    Map data = new ConcurrentHashMap();

    try
    {
      createConnectionFactories(data, environment);
    }
    catch (MalformedURLException e)
    {
      NamingException ne = new NamingException();
      ne.setRootCause(e);
      throw ne;
    }

    createDestinations(data, environment);

    createQueues(data, environment);

    createTopics(data, environment);

    return createContext(data, environment);
  }

  protected ReadOnlyContext createContext(Map data, Hashtable environment)
  {
    return new ReadOnlyContext(environment, data);
  }

  protected void createConnectionFactories(Map data, Hashtable environment) throws MalformedURLException
  {
    for (Iterator iter = environment.entrySet().iterator(); iter.hasNext();)
    {
      Map.Entry entry = (Map.Entry) iter.next();
      String key = entry.getKey().toString();
      if (key.startsWith(CONNECTION_FACTORY_PREFIX))
      {
        String jndiName = key.substring(CONNECTION_FACTORY_PREFIX.length());
        ConnectionFactory cf = createFactory(entry.getValue().toString().trim());
        if (cf != null)
        {
          data.put(jndiName, cf);
        }
      }
    }
  }

  protected void createDestinations(Map data, Hashtable environment)
  {
    for (Iterator iter = environment.entrySet().iterator(); iter.hasNext();)
    {
      Map.Entry entry = (Map.Entry) iter.next();
      String key = entry.getKey().toString();
      if (key.startsWith(DESTINATION_PREFIX))
      {
        String jndiName = key.substring(DESTINATION_PREFIX.length());
        Destination dest = createDestination(entry.getValue().toString().trim());
        if (dest != null)
        {
          data.put(jndiName, dest);
        }
      }
    }
  }

  protected void createQueues(Map data, Hashtable environment)
  {
    for (Iterator iter = environment.entrySet().iterator(); iter.hasNext();)
    {
      Map.Entry entry = (Map.Entry) iter.next();
      String key = entry.getKey().toString();
      if (key.startsWith(QUEUE_PREFIX))
      {
        String jndiName = key.substring(QUEUE_PREFIX.length());
        Queue q = createQueue(entry.getValue().toString().trim());
        if (q != null)
        {
          data.put(jndiName, q);
        }
      }
    }
  }

  protected void createTopics(Map data, Hashtable environment)
  {
    for (Iterator iter = environment.entrySet().iterator(); iter.hasNext();)
    {
      Map.Entry entry = (Map.Entry) iter.next();
      String key = entry.getKey().toString();
      if (key.startsWith(TOPIC_PREFIX))
      {
        String jndiName = key.substring(TOPIC_PREFIX.length());
        Topic t = createTopic(entry.getValue().toString().trim());
        if (t != null)
        {
          data.put(jndiName, t);
        }
      }
    }
  }


  private ConnectionFactory createFactory(String url) throws MalformedURLException
  {
    return ConnectionFactoryImpl.createFromURL(url);
  }

  private DestinationImpl createDestination(String str)
  {
    return DestinationImpl.createDestination(str);
  }

  private QueueImpl createQueue(String address)
  {
    return QueueImpl.createQueue(address);
  }

  private TopicImpl createTopic(String address)
  {
    return TopicImpl.createTopic(address);
  }
}
