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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import live.thought.thought4j.ThoughtClientInterface;
import live.thought.thought4j.ThoughtRPCClient;

public class Miner implements Observer
{
  /** Options for the command line parser. */
  protected static final Options           options             = new Options();
  /** The Commons CLI command line parser. */
  protected static final CommandLineParser gnuParser           = new GnuParser();

  private static final String              DEFAULT_HOST        = "localhost";
  private static final int                 DEFAULT_PORT        = 10617;
  private static final String              DEFAULT_USER        = "user";
  private static final String              DEFAULT_PASS        = "pass";
  private static final long                DEFAULT_RETRY_PAUSE = 10000;

  private Worker                           worker;
  private Poller                           poller;
  private long                             lastWorkTime;
  private long                             lastWorkCycles;
  private long                             lastWorkSolutions;
  private long                             lastWorkNonces;
  private long                             lastWorkErrors;

  private static Miner                     instance;
  private static boolean                   debug = false;

  protected String                         coinbaseAddr;

  private static final Logger              LOG;
  private static final Logger              mainLogger;
  static {
    mainLogger = Logger.getLogger("live.thought");
    mainLogger.setUseParentHandlers(false);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(new SimpleFormatter() {
        private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

        @Override
        public synchronized String format(LogRecord lr) {
            return String.format(format,
                    new Date(lr.getMillis()),
                    lr.getLevel().getLocalizedName(),
                    lr.getMessage()
            );
        }
    });
    mainLogger.addHandler(handler);
    mainLogger.setLevel(Level.ALL);
    LOG = Logger.getLogger(Miner.class.getName());
    LOG.setLevel(Level.ALL);

    options.addOption("h", "host", true, "Thought RPC server host (default: localhost)");
    options.addOption("P", "port", true, "Thought RPC server port (default: 10617)");
    options.addOption("u", "user", true, "Thought server RPC user");
    options.addOption("p", "password", true, "Thought server RPC password");
    options.addOption("t", "threads", true, "Number of miner threads to use");
    options.addOption("c", "coinbase-addr", true, "Address to deliver coinbase reward to");
    options.addOption("H", "help", true, "Displays usage information");
    options.addOption("D", "debug", true, "Set debugging output on");
  }

  public Miner(String host, int port, String user, String pass, String coinbase, long retryPause, int nThread)
  {
    Miner.instance = this;
    if (nThread < 1)
      throw new IllegalArgumentException("Invalid number of threads: " + nThread);
    if (retryPause < 0L)
      throw new IllegalArgumentException("Invalid retry pause: " + retryPause);

    this.coinbaseAddr = coinbase;
    URL url = null;
    try
    {
      url = new URL("http://" + user + ':' + pass + "@" + host + ":" + port + "/");
      poller = new Poller(new ThoughtRPCClient(url));
      worker = new Worker(new ThoughtRPCClient(url), retryPause, nThread);
    }
    catch (MalformedURLException e)
    {
      throw new IllegalArgumentException("Invalid URL: " + url);
    }
    worker.addObserver(this);
    Thread t = new Thread(worker);
    t.start();

    t = new Thread(poller);
    poller.addObserver(this);
    poller.addObserver(worker);
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
    
    TimerTask reporter = new TimerTask() {
        public void run() {
            report();
        }
    };
    Timer timer = new Timer("Timer");
     
    long delay  = 30000L;
    long period = 30000L;
    timer.scheduleAtFixedRate(reporter, delay, period);
  }

  public static Miner getInstance()
  {
    return instance;
  }

  public Worker getWorker()
  {
    return worker;
  }

  public Poller getPoller()
  {
    return poller;
  }

  public String getCoinbaseAddress()
  {
    return coinbaseAddr;
  }

  public void update(Observable o, Object arg)
  {
    Notification n = (Notification) arg;
    if (n == Notification.SYSTEM_ERROR)
    {
      LOG.severe("System error");
      System.exit(1);
    }
    else if (n == Notification.PERMISSION_ERROR)
    {
      LOG.severe("Permission error");
      System.exit(1);
    }
    else if (n == Notification.AUTHENTICATION_ERROR)
    {
      LOG.severe("Invalid worker username or password");
      System.exit(1);
    }
    else if (n == Notification.CONNECTION_ERROR)
    {
      LOG.warning("Connection error, retrying in " + poller.getRetryPause() / 1000L + " seconds");
    }
    else if (n == Notification.COMMUNICATION_ERROR)
    {
      LOG.warning("Communication error");
    }
    else if (n == Notification.LONG_POLLING_FAILED)
    {
      LOG.warning("Long polling failed");
    }
    else if (n == Notification.LONG_POLLING_ENABLED)
    {
      LOG.info("Long polling activated");
    }
    else if (n == Notification.NEW_BLOCK_DETECTED)
    {
      LOG.info("LONGPOLL detected new block");
    }
    else if (n == Notification.POW_TRUE)
    {
      LOG.info("PROOF OF WORK RESULT: true (yay!!!)");
    }
    else if (n == Notification.POW_FALSE)
    {
      LOG.info("PROOF OF WORK RESULT: false (booooo)");
    }
    else if (n == Notification.NEW_WORK)
    {
      report();
    }
  }

  protected void report()
  {
    if (lastWorkTime > 0L)
    {
      long cycles = worker.getCycles() - lastWorkCycles;
      long nonces = worker.getNonces() - lastWorkNonces;
      long errors = worker.getErrors() - lastWorkErrors;
      long solutions = worker.getSolutions() - lastWorkSolutions;
      float speed = (float) cycles / Math.max(1, System.currentTimeMillis() - lastWorkTime);
      LOG.info(String.format("%d nonces, %d solutions, %d cycles, %.2f kilocycles/sec, %d errors", nonces, solutions, cycles, speed, errors));
      // Check for a failed worker and restart if needed.
      if (cycles == 0)
      {
        if (worker.isWarning())
        {
          // This is the second update at 0, create a new worker.
          LOG.warning("Restarting stalled worker.");
          worker.stop();
          worker.deleteObservers();
          poller.deleteObservers();
          poller.addObserver(this);
          Worker tmp = new Worker(worker.getClient(), worker.getPauseMillis(), worker.getnThreads());
          worker = tmp;
          System.gc();
          worker.addObserver(this);
          poller.addObserver(worker);
          Thread t = new Thread(worker);
          t.start();
          LOG.info("Worker restarted.");
        }
        else
        {
          // This is the first time, so warn.
          LOG.finest("Worker may be stalled.");
          worker.setWarning(true);
        }
      }
      else
      {
        // If we've had a warning but now we're working, clear it.
        worker.setWarning(false);
      }
    }
    lastWorkTime = System.currentTimeMillis();
    lastWorkCycles = worker.getCycles();
    lastWorkNonces = worker.getNonces();
    lastWorkErrors = worker.getErrors();
    lastWorkSolutions = worker.getSolutions();
    
    
  }
  
  public static boolean getDebug()
  {
    return debug;
  }

  
  protected static void usage()
  {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Miner", options);
  }

  public static void main(String[] args)
  {
    String host = DEFAULT_HOST;
    int port = DEFAULT_PORT;
    String user = DEFAULT_USER;
    String pass = DEFAULT_PASS;
    String coinbase = null;
    int nThread = Runtime.getRuntime().availableProcessors();
    long retryPause = DEFAULT_RETRY_PAUSE;
    CommandLine commandLine = null;

    try
    {
      commandLine = gnuParser.parse(options, args);
      if (commandLine.hasOption("host"))
      {
        host = commandLine.getOptionValue("host");
      }
      if (commandLine.hasOption("port"))
      {
        port = Integer.parseInt(commandLine.getOptionValue("port"));
      }
      if (commandLine.hasOption("user"))
      {
        user = commandLine.getOptionValue("user");
      }
      if (commandLine.hasOption("password"))
      {
        pass = commandLine.getOptionValue("password");
      }
      if (commandLine.hasOption("threads"))
      {
        nThread = Integer.parseInt(commandLine.getOptionValue("threads"));
      }
      if (commandLine.hasOption("coinbase-addr"))
      {
        coinbase = commandLine.getOptionValue("coinbase-addr");
      }
      else
      {
        System.out.println("No coinbase address specified.");
        usage();
        System.exit(1);
      }
      if (commandLine.hasOption("help"))
      {
        usage();
        System.exit(1);
      }
      if (commandLine.hasOption("debug"))
      {
        Miner.mainLogger.setLevel(Level.ALL);;
        Miner.debug = true;
      }
      new Miner(host, port, user, pass, coinbase, retryPause, nThread);
    }
    catch (ParseException pe)
    {
      System.err.println(pe.getLocalizedMessage());
      usage();
    }
    catch (Exception e)
    {
      System.err.println(e.getLocalizedMessage());
    }
  }

}
