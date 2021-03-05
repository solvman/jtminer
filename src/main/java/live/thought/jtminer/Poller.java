/*
 * jtminer Java mining software for the Thought Network
 * 
 * Copyright (c) 2018 - 2019, Thought Network LLC
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
import java.util.List;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicBoolean;

import live.thought.jtminer.data.BlockImpl;
import live.thought.jtminer.util.Console;
import live.thought.thought4j.ThoughtClientInterface;
import live.thought.thought4j.ThoughtClientInterface.BlockTemplate;

public class Poller extends Observable implements Runnable
{
  protected long                   retryPause      = 10000;
  protected ThoughtClientInterface client;

  protected AtomicBoolean          moreElectricity = new AtomicBoolean(false);
  protected Work                   currentWork     = null;
  protected long                   currentHeight   = 0;
  protected int[]                  workMutex       = new int[0];
  
  protected List<Integer>          voteBits;

  public Poller(ThoughtClientInterface client)
  {
    this.client = client;
  }
  
  public Poller(ThoughtClientInterface client, List<Integer> voteBits)
  {
    this.client = client;
    this.voteBits = voteBits;
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
    Console.debug("Starting poller.", 2);
    moreElectricity.set(true);
    boolean notified = false;
    // Make initial connection for poller
    BlockTemplate bl = null;
    String longpollid = null;

    while (moreElectricity.get())
    {
      try
      {
        bl = client.getBlockTemplate(longpollid);
        longpollid = bl.longpollid();
        if (null != bl.longpollid())
        {
          if (!notified)
          {
            setChanged();
            notifyObservers(Notification.LONG_POLLING_ENABLED);
            notified = true;
          }
          if (bl.height() > currentHeight)
          {
            setChanged();
            notifyObservers(Notification.NEW_BLOCK_DETECTED);
            Console.output(String.format("@|cyan Current block is %d|@", bl.height()));

            Work w = new Work(bl);
            if (null != voteBits)
            {
              BlockImpl b = w.getBlock();
              for (int i : voteBits)
              {
                b.addVoteBit(i);
              }
            }
            synchronized (workMutex)
            {
              currentWork = w;
              currentHeight = bl.height();
            }
            setChanged();
            notifyObservers(Notification.NEW_WORK);
          }
        }
        else
        {
          setChanged();
          notifyObservers(Notification.LONG_POLLING_FAILED);
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
          //e.printStackTrace();
          notifyObservers(Notification.COMMUNICATION_ERROR);
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

}
