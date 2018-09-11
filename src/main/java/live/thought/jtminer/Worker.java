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

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import live.thought.jtminer.algo.Cuckoo;
import live.thought.jtminer.algo.CuckooSolve;
import live.thought.jtminer.data.BlockImpl;
import live.thought.thought4j.ThoughtClientInterface;

public class Worker extends Observable implements Observer, Runnable
{
  private static final Logger    LOG        = Logger.getLogger(Worker.class.getCanonicalName());

  private ThoughtClientInterface client;
  private int                    nThreads;

  private volatile Work          curWork    = null;
  private AtomicLong             cycles     = new AtomicLong(0L);
  private AtomicLong             nonces     = new AtomicLong(0L);
  private AtomicLong             errors     = new AtomicLong(0L);
  private AtomicLong             solutions  = new AtomicLong(0L);
  private AtomicInteger          cycleIndex = new AtomicInteger(0);

  public Worker(ThoughtClientInterface client, long pauseMillis)
  {
    this(client, pauseMillis, Runtime.getRuntime().availableProcessors());
  }

  public Worker(ThoughtClientInterface client, long pauseMillis, int nThreads)
  {
    this.client = client;
    if (nThreads < 0)
      throw new IllegalArgumentException();
    this.nThreads = nThreads;
  }

  public long getCycles()
  {
    return cycles.get();
  }

  public void incrementCycles()
  {
    cycles.incrementAndGet();
  }

  public long getNonces()
  {
    return nonces.get();
  }

  public void incrementNonces()
  {
    nonces.incrementAndGet();
  }

  public long getErrors()
  {
    return errors.get();
  }

  public void incrementErrors()
  {
    errors.incrementAndGet();
  }

  public long getSolutions()
  {
    return solutions.get();
  }

  public void incrementSolutions()
  {
    solutions.incrementAndGet();
  }

  private volatile boolean running = false;

  public synchronized void stop()
  {
    LOG.finest("Stopping worker.");
    running = false;
    this.notifyAll();
  }

  @Override
  public void run()
  {
    running = true;
    LOG.finest("Starting worker.");

    while (running)
    {
      ArrayList<Thread> threads = new ArrayList<Thread>(nThreads);
      if (null != curWork)
      {
        LOG.finest("Target: " + curWork.getTarget().toString(16));
        LOG.info("Starting " + nThreads + " solvers.");

        BlockImpl block = curWork.getBlock();
        int blockNonce = cycleIndex.getAndIncrement();
        block.setNonce(blockNonce);
        CuckooSolve solve = new CuckooSolve(block.getHeader(), Cuckoo.NNODES, nThreads);
        for (int n = 0; n < nThreads; n++)
        {
          WorkChecker checker = new WorkChecker(client, curWork, cycleIndex.getAndIncrement(), solve);
          checker.addObserver(Miner.getInstance());
          Miner.getInstance().getPoller().addObserver(checker);

          Thread t = new Thread(checker);
          threads.add(t);
          t.start();
        }

        for (Thread t : threads)
        {
          try
          {
            t.join();
          }
          catch (InterruptedException e)
          {
            // Swallow
          }
        }
      }
      // Wait a bit for some work
      try
      {
        Thread.sleep(5000);
      }
      catch (InterruptedException e)
      {
        // quiet
      }
    }
  }

  @Override
  public void update(Observable o, Object arg)
  {
    Notification n = (Notification) arg;
    if (n == Notification.NEW_WORK)
    {
      LOG.finest("Getting new work.");
      curWork = Miner.getInstance().getPoller().getWork();
      if (null != curWork)
      {
        LOG.finest("New work retrieved.");
      }
      cycleIndex.set(0);
    }
  }

}
