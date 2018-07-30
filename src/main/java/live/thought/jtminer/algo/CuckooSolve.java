
//Cuckoo Cycle, a memory-hard proof-of-work
//Copyright (c) 2013-2016 John Tromp
package live.thought.jtminer.algo;

import java.util.Set;
import java.util.HashSet;

public class CuckooSolve
{
  public static final int MAXPATHLEN = 4096;
  Cuckoo           graph;
  int              easiness;
  int[]            cuckoo;
  int[][]          sols;
  int              nsols;
  int              maxsols;
  int              nthreads;

  public CuckooSolve(byte[] hdr, int en, int ms, int nt)
  {
    graph = new Cuckoo(hdr);
    easiness = en;
    sols = new int[ms][Cuckoo.PROOFSIZE];
    cuckoo = new int[1 + (int) Cuckoo.NNODES];
    assert cuckoo != null;
    nsols = 0;
    nthreads = nt;
    maxsols = ms;
  }

  public int path(int u, int[] us)
  {
    int nu;
    for (nu = 0; u != 0; u = cuckoo[u])
    {
      if (++nu >= MAXPATHLEN)
      {
        while (nu-- != 0 && us[nu] != u)
          ;
        if (nu < 0)
          System.out.println("maximum path length exceeded");
        else
          System.out.println("illegal " + (MAXPATHLEN - nu) + "-cycle");
        Thread.currentThread().interrupt();
      }
      us[nu] = u;
    }
    return nu;
  }

  public synchronized void solution(int[] us, int nu, int[] vs, int nv)
  {
    Set<Edge> cycle = new HashSet<Edge>();
    int n;
    cycle.add(new Edge(us[0], vs[0] - Cuckoo.NEDGES));
    while (nu-- != 0) // u's in even position; v's in odd
      cycle.add(new Edge(us[(nu + 1) & ~1], us[nu | 1] - Cuckoo.NEDGES));
    while (nv-- != 0) // u's in odd position; v's in even
      cycle.add(new Edge(vs[nv | 1], vs[(nv + 1) & ~1] - Cuckoo.NEDGES));
    for (int nonce = n = 0; nonce < easiness; nonce++)
    {
      Edge e = graph.sipedge(nonce);
      if (cycle.contains(e))
      {
        sols[nsols][n++] = nonce;
        cycle.remove(e);
      }
    }
    if (n == Cuckoo.PROOFSIZE)
      nsols++;
    //else
    //  System.out.println("Only recovered " + n + " nonces");
  }

  public int getEasiness()
  {
    return easiness;
  }

  public void setEasiness(int easiness)
  {
    this.easiness = easiness;
  }

  public int[] getCuckoo()
  {
    return cuckoo;
  }

  public int getNthreads()
  {
    return nthreads;
  }

  public Cuckoo getGraph()
  {
    return graph;
  }

  public int[][] getSols()
  {
    return sols;
  }

  public int getNsols()
  {
    return nsols;
  }

  public int getMaxSols()
  {
    return maxsols;
  }
  
  
}