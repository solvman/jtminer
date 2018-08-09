/*
 * jtminer Java mining software for the Thought Network
 * 
 * Copyright (c) 2018, Thought Network LLC
 * 
 * Based on code from Litecoin JMiner
 * Copyright 2011  LitecoinPool.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as 
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package live.thought.jtminer;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import live.thought.thought4j.ThoughtClientInterface;
import live.thought.thought4j.ThoughtClientInterface.BlockTemplate;

public class Poller extends Observable implements Runnable
{
  private static final Logger      LOG       = Logger.getLogger(Poller.class.getCanonicalName());
  static
  {
    LOG.setLevel(Level.ALL);
    for (Handler handler : LOG.getParent().getHandlers())
      handler.setLevel(Level.ALL);
  }
  
  
  protected long                   retryPause = 10000;

  protected ThoughtClientInterface client;

  protected AtomicBoolean          moreElectricity = new AtomicBoolean(false);
  protected Work                   currentWork;
  protected long                   currentHeight;
  protected int[]                  workMutex = new int[0];
  

  public Poller(ThoughtClientInterface client)
  {
    this.client = client;
  }

  public synchronized void shutdown()
  {
    moreElectricity.set(false);
    this.notifyAll();
  }

  public synchronized Work getWork()
  {
    Work retval = null;

      retval = currentWork;

    return retval;
  }
  
  public long getRetryPause()
  {
    return retryPause;
  }

  @Override
  public void run()
  {
	  LOG.finest("Starting poller.");
    BlockTemplate bl = client.getBlockTemplate();
    String longpollid = bl.longpollid();
    if (null != longpollid)
    {
      moreElectricity.set(true);
      setChanged();
      notifyObservers(Notification.LONG_POLLING_ENABLED);
      Work w = new Work(bl);
      synchronized (workMutex)
      {
        currentWork = w;
        currentHeight = bl.height();
      }
      setChanged();
      notifyObservers(Notification.NEW_WORK);
      
      while (moreElectricity.get())
      {
        try
        {
          bl = client.getBlockTemplate(longpollid);
          if (bl.height() > currentHeight)
          {
            setChanged();
            notifyObservers(Notification.NEW_BLOCK_DETECTED);
            LOG.info(String.format("Current block is %d", bl.height()));

            w = new Work(bl);
            synchronized (workMutex)
            {
              currentWork = w;
              currentHeight = bl.height();
            }
            setChanged();
            notifyObservers(Notification.NEW_WORK);
          }
        }
        catch (Exception e)
        {
          setChanged();
          if (e instanceof IllegalArgumentException)
          {
            notifyObservers(Notification.AUTHENTICATION_ERROR);
            shutdown();
            break;
          }
          else if (e instanceof AccessControlException)
          {
            notifyObservers(Notification.PERMISSION_ERROR);
            shutdown();
            break;
          }
          else if (e instanceof IOException)
          {
            notifyObservers(Notification.CONNECTION_ERROR);
          }
          else
          {
            notifyObservers(Notification.COMMUNICATION_ERROR);
            e.printStackTrace();
          }
          try
          {
            currentWork = null;
            Thread.sleep(retryPause);
          }
          catch (InterruptedException ie)
          {
          }
        }
      }
    }
    else
    {
      setChanged();
      notifyObservers(Notification.LONG_POLLING_FAILED);
    }
    currentWork = null;
    setChanged();
    notifyObservers(Notification.TERMINATED);
  }

}
