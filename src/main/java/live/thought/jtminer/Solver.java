/*
 * jtminer Java mining software for the Thought Network
 * 
 * Copyright (c) 2018 - 2019, Thought Network LLC
 *
 * Based on code from Cuckoo Cycle, a memory-hard proof-of-work
 * Copyright (c) 2013-2016 John Tromp
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
import java.security.GeneralSecurityException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

import live.thought.jtminer.algo.Cuckoo;
import live.thought.jtminer.algo.CuckooSolve;
import live.thought.jtminer.algo.SHA256d;
import live.thought.jtminer.util.Console;
import live.thought.thought4j.ThoughtClientInterface;

public class Solver extends Observable implements Observer, Runnable
{
  private int                    index;
  private CuckooSolve            solve;
  private Work                   curWork;
  private ThoughtClientInterface client;
  private SHA256d                hasher             = new SHA256d(32);
  private AtomicBoolean          stop               = new AtomicBoolean();

  public Solver(ThoughtClientInterface client, Work curWork, int index, CuckooSolve solve)
  {
    this.solve = solve;
    this.index = index;
    this.curWork = curWork;
    this.client = client;
    stop.set(false);
  }

  public synchronized void stop()
  {
    Console.debug("Stopping solver " + index, 2);
    this.stop.set(true); 
  }

  public void cleanup()
  {
    Miner.getInstance().getPoller().deleteObserver(this);
    deleteObserver(Miner.getInstance());
  }
  
  public void run()
  {
    Console.debug("Starting solver " + index, 2);
    int[] cuckoo = solve.getCuckoo();
    int[] us = new int[CuckooSolve.MAXPATHLEN], vs = new int[CuckooSolve.MAXPATHLEN];
    try
    {
      for (int nonce = index; nonce < solve.getEasiness(); nonce += solve.getNthreads())
      {
        if (stop.get())
        {
          Thread.currentThread().interrupt();
          break;
        }
        int u = cuckoo[us[0] = (int) solve.getGraph().sipnode(nonce, 0)];
        int v = cuckoo[vs[0] = (int) (Cuckoo.NEDGES + solve.getGraph().sipnode(nonce, 1))];
        if (u == vs[0] || v == us[0])
          continue; // ignore duplicate edges
        int nu = solve.path(u, us), nv = solve.path(v, vs);
        if (us[nu] == vs[nv])
        {
          int min = nu < nv ? nu : nv;
          for (nu -= min, nv -= min; us[nu] != vs[nv]; nu++, nv++)
            ;
          int len = nu + nv + 1;
          Miner.getInstance().incrementCycles();
          if (len == Cuckoo.PROOFSIZE)
          {
            int[] soln = solve.solution(us, nu, vs, nv);
            if (null != soln)
            {
              Miner.getInstance().incrementSolutions();
              try
              {
                if (solve.getGraph().verify(soln, Cuckoo.NNODES))
                {
                  if (curWork.meetsTarget(index, soln, hasher))
                  {
                    Console.debug("Trying to submit.", 2);
                    boolean success = curWork.submit(client, soln);
                    setChanged();
                    notifyObservers(success ? Notification.POW_TRUE : Notification.POW_FALSE);
                    stop();
                    break;
                  }
                }
                else
                {
                  Miner.getInstance().incrementErrors();
                }
              }
              catch (GeneralSecurityException e)
              {
                Console.output(e.toString());
              }
              catch (IOException e)
              {
                Console.output(e.toString());
              }
            }
          }
          continue;
        }
        if (nu < nv)
        {
          while (nu-- != 0)
            cuckoo[us[nu + 1]] = us[nu];
          cuckoo[us[0]] = vs[0];
        }
        else
        {
          while (nv-- != 0)
            cuckoo[vs[nv + 1]] = vs[nv];
          cuckoo[vs[0]] = us[0];
        }
      }
    }
    catch (RuntimeException re)
    {
      Console.debug("Illegal cycle.", 2);
    }
    cleanup();
    Console.debug("Exiting solver " + index, 2);
  }

  @Override
  public void update(Observable o, Object arg)
  {
    Notification n = (Notification) arg;
    if (n == Notification.NEW_WORK)
    {
      stop();
    }
  }
}