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
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import live.thought.jtminer.algo.Cuckoo;
import live.thought.jtminer.algo.CuckooSolve;
import live.thought.jtminer.data.BlockImpl;
import live.thought.jtminer.util.Console;
import live.thought.thought4j.ThoughtRPCClient;

/**
 * @author phil_000
 *
 */
public class Miner implements Observer
{
  /** Options for the command line parser. */
  protected static final Options           options      = new Options();
  /** The Commons CLI command line parser. */
  protected static final CommandLineParser gnuParser    = new GnuParser();
  /** Default values for connection. */
  private static final String              DEFAULT_HOST = "localhost";
  private static final int                 DEFAULT_PORT = 10617;
  private static final String              DEFAULT_USER = "user";
  private static final String              DEFAULT_PASS = "pass";

  /** Longpoll client */
  private Poller                           poller;
  /** Performance metrics */
  private long                             lastWorkTime;
  private long                             lastWorkCycles;
  private long                             lastWorkSolutions;
  private long                             lastWorkErrors;
  private AtomicLong                       cycles       = new AtomicLong(0L);
  private AtomicLong                       errors       = new AtomicLong(0L);
  private AtomicLong                       solutions    = new AtomicLong(0L);

  /** Work in progress */
  private volatile Work                    curWork      = null;
  private AtomicInteger                    cycleIndex   = new AtomicInteger(0);

  /** Single instance */
  private static Miner                     instance;
  /** Control printing of debug messages */
  private static int                       debugLevel   = 1;

  /** Runtime parameters */
  protected String                         coinbaseAddr;
  protected boolean                        moreElectricity;
  protected int                            nThreads;

  /** Connection for solvers */
  private ThoughtRPCClient                 client;

  /** Set up command line options. */
  static
  {
    options.addOption("h", "host", true, "Thought RPC server host (default: localhost)");
    options.addOption("P", "port", true, "Thought RPC server port (default: 10617)");
    options.addOption("u", "user", true, "Thought server RPC user");
    options.addOption("p", "password", true, "Thought server RPC password");
    options.addOption("t", "threads", true, "Number of miner threads to use");
    options.addOption("c", "coinbase-addr", true, "Address to deliver coinbase reward to");
    options.addOption("H", "help", true, "Displays usage information");
    options.addOption("D", "debug", true, "Set debugging output on");

    Console.setLevel(debugLevel);
  }

  /**
   * Constructs an instance of Miner. Protected for Singleton.
   * 
   * @param host
   *          The thoughtd RPC host
   * @param port
   *          The thoughtd RPC port
   * @param user
   *          The thoughtd RPC username
   * @param pass
   *          The thoughtd RPC password
   * @param coinbase
   *          The address to assign coinbase transactions to
   * @param nThread
   *          The number of solver threads to use. Will default to the number of
   *          available CPUs.
   */
  protected Miner(String host, int port, String user, String pass, String coinbase, int nThread)
  {
    Miner.instance = this;
    if (nThread < 1)
    {
      throw new IllegalArgumentException("Invalid number of threads: " + nThread);
    }
    else
    {
      this.nThreads = nThread;
    }
    this.coinbaseAddr = coinbase;
    URL url = null;
    try
    {
      url = new URL("http://" + user + ':' + pass + "@" + host + ":" + port + "/");
      client = new ThoughtRPCClient(url);

      // Poller thread gets its own connection
      poller = new Poller(new ThoughtRPCClient(url));
      Thread t = new Thread(poller);
      poller.addObserver(this);
      t.setPriority(Thread.MIN_PRIORITY);
      t.start();
    }
    catch (MalformedURLException e)
    {
      throw new IllegalArgumentException("Invalid URL: " + url);
    }

    // Set up timer for performance metric reporting
    TimerTask reporter = new TimerTask()
    {
      public void run()
      {
        report();
      }
    };
    Timer timer = new Timer("Timer");

    long delay = 15000L;
    long period = 15000L;
    timer.scheduleAtFixedRate(reporter, delay, period);
  }

  public static Miner getInstance()
  {
    return instance;
  }

  public int getDebugLevel()
  {
    return debugLevel;
  }

  public Poller getPoller()
  {
    return poller;
  }

  public String getCoinbaseAddress()
  {
    return coinbaseAddr;
  }

  public void incrementCycles()
  {
    cycles.incrementAndGet();
  }

  public void incrementSolutions()
  {
    solutions.incrementAndGet();
  }

  public void incrementErrors()
  {
    errors.incrementAndGet();
  }

  public void update(Observable o, Object arg)
  {
    Notification n = (Notification) arg;
    if (n == Notification.SYSTEM_ERROR)
    {
      Console.output("@|red System error|@");
      moreElectricity = false;
    }
    else if (n == Notification.PERMISSION_ERROR)
    {
      Console.output("@|red Permission Error|@");
      moreElectricity = false;
    }
    else if (n == Notification.AUTHENTICATION_ERROR)
    {
      Console.output("@|red Invalid worker username or password|@");
      moreElectricity = false;
    }
    else if (n == Notification.CONNECTION_ERROR)
    {
      Console.output("@|yellow Connection error, retrying in " + poller.getRetryPause() / 1000L + " seconds|@");
    }
    else if (n == Notification.COMMUNICATION_ERROR)
    {
      Console.output("@|red Communication error|@");
    }
    else if (n == Notification.LONG_POLLING_FAILED)
    {
      Console.output("@|red Long polling failed|@");
    }
    else if (n == Notification.LONG_POLLING_ENABLED)
    {
      Console.output("@|bold,white Long polling activated|@");
    }
    else if (n == Notification.NEW_BLOCK_DETECTED)
    {
      Console.output("@|bold,white LONGPOLL detected new block|@");
    }
    else if (n == Notification.POW_TRUE)
    {
      Console.output("@|white PROOF OF WORK RESULT:|@ @|bold,white true|@ @|bold,green (yay!!!)|@");
    }
    else if (n == Notification.POW_FALSE)
    {
      Console.output("@|white PROOF OF WORK RESULT:|@ @|bold,white false @|bold,red (boo...)|@");
    }
    else if (n == Notification.NEW_WORK)
    {
      Console.debug("Getting new work.", 2);
      curWork = getPoller().getWork();
      if (null != curWork)
      {
        Console.debug("New work retrieved.", 2);
      }
      cycleIndex.set(0);
    }
  }

  protected void report()
  {
    if (lastWorkTime > 0L)
    {
      long currentCycles = cycles.get() - lastWorkCycles;
      long currentErrors = errors.get() - lastWorkErrors;
      long currentSolutions = solutions.get() - lastWorkSolutions;
      float speed = (float) currentCycles / Math.max(1, System.currentTimeMillis() - lastWorkTime);
      Console.output(String.format("%d cycles, %d solutions, %d errors, %.2f kilocycles/sec", currentCycles, currentSolutions,
          currentErrors, speed));     
    }
    lastWorkTime = System.currentTimeMillis();
    lastWorkCycles = cycles.get();
    lastWorkErrors = errors.get();
    lastWorkSolutions = solutions.get();
  }

  protected static void usage()
  {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Miner", options);
  }

  public void run()
  {
    moreElectricity = true;
    Console.output("Using " + nThreads + " threads.");

    while (moreElectricity)
    {
      ArrayList<Thread> threads = new ArrayList<Thread>(nThreads);
      if (null != curWork)
      {
        Console.debug(String.format("Target: %064x", curWork.getTarget()), 2);
        Console.debug("Starting " + nThreads + " solvers.", 2);

        BlockImpl block = curWork.getBlock();
        int blockNonce = cycleIndex.getAndIncrement();
        block.setNonce(blockNonce);
        CuckooSolve solve = new CuckooSolve(block.getHeader(), Cuckoo.NNODES, nThreads);
        for (int n = 0; n < nThreads; n++)
        {
          Solver solver = new Solver(client, curWork, cycleIndex.getAndIncrement(), solve);
          solver.addObserver(Miner.getInstance());
          Miner.getInstance().getPoller().addObserver(solver);

          Thread t = new Thread(solver);
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
        Thread.sleep(1000);
      }
      catch (InterruptedException e)
      {
        // quiet
      }
    }
  }

  public static void main(String[] args)
  {
    String host = DEFAULT_HOST;
    int port = DEFAULT_PORT;
    String user = DEFAULT_USER;
    String pass = DEFAULT_PASS;
    String coinbase = null;
    int nThread = Runtime.getRuntime().availableProcessors();
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
        Console.setLevel(2);
      }
      Miner miner = new Miner(host, port, user, pass, coinbase, nThread);
      miner.run();
      Console.end();
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
