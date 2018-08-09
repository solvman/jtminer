package live.thought.jtminer;

import java.security.GeneralSecurityException;
import java.util.Observable;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import live.thought.jtminer.algo.Cuckoo;
import live.thought.jtminer.algo.CuckooSolve;
import live.thought.jtminer.algo.SHA256d;
import live.thought.thought4j.ThoughtClientInterface;

public class WorkChecker extends Observable implements Runnable
{
  private static final Logger LOG = Logger.getLogger(WorkChecker.class.getCanonicalName());
  static
  {
    LOG.setLevel(Level.ALL);
    for (Handler handler : LOG.getParent().getHandlers())
      handler.setLevel(Level.ALL);
  }

  private static final long      THROTTLE_WAIT_TIME = 100L * 1000000L; // ns
  private int                    index;
  private CuckooSolve            solve;
  private boolean                checking;
  private Work                   curWork;
  private ThoughtClientInterface client;
  private SHA256d                hasher = new SHA256d(32);

  public WorkChecker(ThoughtClientInterface client, Work curWork, int index, CuckooSolve solve)
  {
    this.solve = solve;
    this.index = index;
    this.curWork = curWork;
    this.client = client;
  }

  public synchronized void stop()
  {
    LOG.finest("Stopping checker " + index);
    checking = false;
    this.notifyAll();
  }

  public void run()
  {
    LOG.finest("Starting checker " + index);
    checking = true;
    long dt, t0 = System.nanoTime();

    try
    {
      while (checking && solve.getNsols() <= solve.getMaxSols())
      {
        int[] cuckoo = solve.getCuckoo();
        int[] us = new int[CuckooSolve.MAXPATHLEN], vs = new int[CuckooSolve.MAXPATHLEN];
        for (int nonce = index; nonce < Cuckoo.NNODES; nonce += solve.getNthreads())
        {
          int u = cuckoo[us[0] = (int) solve.getGraph().sipnode(nonce, 0)];
          int v = cuckoo[vs[0] = (int) (Cuckoo.NEDGES + solve.getGraph().sipnode(nonce, 1))];
          if (u == vs[0] || v == us[0])
            continue; // ignore duplicate edges
          int nu = 0, nv = 0;
          try
          {
            nu = solve.path(u, us);
            nv = solve.path(v, vs);
          }
          catch (RuntimeException e)
          {
            continue;
          }
          if (us[nu] == vs[nv])
          {
            int min = nu < nv ? nu : nv;
            for (nu -= min, nv -= min; us[nu] != vs[nv]; nu++, nv++)
              ;
            int len = nu + nv + 1;
            Miner.getInstance().getWorker().incrementCycles();
            if (len == Cuckoo.PROOFSIZE && solve.getNsols() < solve.getSols().length)
            {
              solve.solution(us, nu, vs, nv);
              if (solve.getNsols() > 0)
              {
                Miner.getInstance().getWorker().incrementSolutions();
                //LOG.finest("Found a solution in checker " + index);
                if (curWork.meetsTarget(nonce, solve.getSols()[solve.getNsols() - 1], hasher))
                {
                  LOG.finest("Solution meets target in checker " + index);
                  WorkSubmitter ws = new WorkSubmitter(client, curWork, nonce, solve.getSols()[solve.getNsols() - 1]);
                  ws.addObserver(Miner.getInstance());
                  new Thread(ws).start();
                  checking = false;
                  break;
                }
              }
            }
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
    }
    catch (GeneralSecurityException e)
    {
      setChanged();
      notifyObservers(Notification.SYSTEM_ERROR);
      stop();
    }
  }
}