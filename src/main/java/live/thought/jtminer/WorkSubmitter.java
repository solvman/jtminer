package live.thought.jtminer;

import java.io.IOException;
import java.util.Observable;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import live.thought.thought4j.ThoughtClientInterface;

public class WorkSubmitter extends Observable implements Runnable
{
  private static final Logger    LOG = Logger.getLogger(WorkChecker.class.getCanonicalName());
  static
  {
    LOG.setLevel(Level.ALL);
    for (Handler handler : LOG.getParent().getHandlers())
      handler.setLevel(Level.ALL);
  }
  
  private ThoughtClientInterface client;
  private Work                   work;
  private int                    nonce;
  private int[]                  solution;

  public WorkSubmitter(ThoughtClientInterface client, Work w, int nonce, int[] solution)
  {
    this.client = client;
    this.work = w;
    this.nonce = nonce;
    this.solution = solution;
  }

  public void run()
  {
    try
    {
      LOG.finest("Submitting solution.");
      boolean result = work.submit(client, nonce, solution);
      setChanged();
      notifyObservers(result ? Notification.POW_TRUE : Notification.POW_FALSE);
    }
    catch (IOException e)
    {
    }
  }
}
