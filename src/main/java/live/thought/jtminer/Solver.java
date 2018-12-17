package live.thought.jtminer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import live.thought.jtminer.algo.Cuckoo;
import live.thought.jtminer.algo.CuckooSolve;
import live.thought.jtminer.algo.SHA256d;
import live.thought.thought4j.ThoughtClientInterface;

public class Solver extends Observable implements Observer, Runnable
{
  private static final Logger LOG = Logger.getLogger(Solver.class.getCanonicalName());

  //private static final long      THROTTLE_WAIT_TIME = 100L * 1000000L;    // ns
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
    LOG.finest("Stopping solver " + index);
    this.stop.set(true);
    this.setChanged();
    this.notifyObservers();
    Miner.getInstance().getPoller().deleteObserver(this);
    deleteObserver(Miner.getInstance());
  }

  public void run()
  {
    LOG.finest("Starting solver " + index);
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
        Miner.getInstance().getWorker().incrementNonces();
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
          Miner.getInstance().getWorker().incrementCycles();
          if (len == Cuckoo.PROOFSIZE)
          {
            int[] soln = solve.solution(us, nu, vs, nv);
            if (null != soln)
            {
              Miner.getInstance().getWorker().incrementSolutions();
              try
              {
                if (solve.getGraph().verify(soln, Cuckoo.NNODES))
                {
                  if (curWork.meetsTarget(index, soln, hasher))
                  {
                    boolean success = curWork.submit(client, soln);
                    setChanged();
                    notifyObservers(success ? Notification.POW_TRUE : Notification.POW_FALSE);
                    stop();
                    break;
                  }
                }
                else
                {
                  Miner.getInstance().getWorker().incrementErrors();
                }
              }
              catch (GeneralSecurityException e)
              {
                LOG.warning(e.toString());
              }
              catch (IOException e)
              {
                LOG.warning(e.toString());
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
      LOG.finest("Illegal cycle.");
    }
    LOG.finest("Exiting solver " + index);
    Thread.currentThread().interrupt();
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