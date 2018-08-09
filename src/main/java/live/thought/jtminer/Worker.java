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
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import live.thought.jtminer.algo.Cuckoo;
import live.thought.jtminer.algo.CuckooSolve;
import live.thought.thought4j.ThoughtClientInterface;

public class Worker extends Observable implements Observer, Runnable
{
  private static final Logger LOG = Logger.getLogger(Worker.class.getCanonicalName());
  static
  {
    LOG.setLevel(Level.ALL);
    for (Handler handler : LOG.getParent().getHandlers())
      handler.setLevel(Level.ALL);
  }

  private ThoughtClientInterface client;
  private int                    nThreads;

  private volatile Work          curWork      = null;
  private AtomicLong             cycles       = new AtomicLong(0L);
  private AtomicLong             solutions    = new AtomicLong(0L);
  private AtomicInteger          cycleIndex   = new AtomicInteger(0);

  private ArrayList<WorkChecker> checkers     = new ArrayList<WorkChecker>();
  private int[]                  checkerMutex = new int[0];

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
    
    long maxMem = Runtime.getRuntime().maxMemory();
    long useful = (long)(0.1 * maxMem);
    long perThread = useful / nThreads;
    long need = 4 * Cuckoo.PROOFSIZE;
    long maxSols = Math.min(perThread / need, 65535);
    LOG.finest("Using " + maxSols + " solutions per thread instance.");

    while (running)
    {
      ArrayList<Thread> threads = new ArrayList<Thread>(nThreads);
      if (null != curWork)
      {
        // Start work checkers 
        synchronized (checkerMutex)
        {
          LOG.finest("New target: " + curWork.getTarget().toString(16));
          LOG.finest("Starting work checkers.");
          for (int n = 0; n < nThreads; n++)
          {
            CuckooSolve solve = new CuckooSolve(curWork.getData(), Cuckoo.NNODES, (int) maxSols, nThreads);
            WorkChecker checker = new WorkChecker(client, curWork, cycleIndex.getAndIncrement(), solve);
            checker.addObserver(Miner.getInstance());
            checkers.add(checker);

            Thread t = new Thread(checker);
            threads.add(t);
            t.start();
          }
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
        Thread.sleep(500);
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
      synchronized (checkerMutex)
      {
        LOG.finest("Getting new work.");
        curWork = Miner.getInstance().getPoller().getWork();
        if (null != curWork)
        {
          LOG.finest("New work retrieved.");
        }
        LOG.finest("Stopping checkers on new work notification.");
        for (Iterator<WorkChecker> it = checkers.iterator(); it.hasNext();)
        {
          WorkChecker w = it.next();
          w.stop();
          it.remove();
        }
        cycleIndex.set(0);
      }

    }
  }

}
