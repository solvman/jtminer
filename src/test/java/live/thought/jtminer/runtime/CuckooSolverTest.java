package live.thought.jtminer.runtime;

import live.thought.jtminer.algo.Cuckoo;
import live.thought.jtminer.algo.CuckooSolve;

public class CuckooSolverTest implements Runnable {
  
  int id;
  CuckooSolve solve;

  public CuckooSolverTest(int i, CuckooSolve cs) {
    id = i;
    solve = cs;
  }

  public void run() {
    int[] cuckoo = solve.getCuckoo();
    int[] us = new int[CuckooSolve.MAXPATHLEN], vs = new int[CuckooSolve.MAXPATHLEN];
    for (int nonce = id; nonce < Cuckoo.NNODES; nonce += solve.getNthreads()) {
      int u = cuckoo[us[0] = (int)solve.getGraph().sipnode(nonce,0)];
      int v = cuckoo[vs[0] = (int)(Cuckoo.NEDGES + solve.getGraph().sipnode(nonce,1))];
      if (u == vs[0] || v == us[0])
        continue; // ignore duplicate edges
      int nu = solve.path(u, us), nv = solve.path(v, vs);
      if (us[nu] == vs[nv]) {
        int min = nu < nv ? nu : nv;
        for (nu -= min, nv -= min; us[nu] != vs[nv]; nu++, nv++) ;
        int len = nu + nv + 1;
        System.out.println(" " + len + "-cycle found at " + id);
        if (len == Cuckoo.PROOFSIZE && solve.getNsols() < solve.getSols().length)
          solve.solution(us, nu, vs, nv);
        continue;
      }
      if (nu < nv) {
        while (nu-- != 0)
          cuckoo[us[nu+1]] = us[nu];
        cuckoo[us[0]] = vs[0];
      } else {
        while (nv-- != 0)
          cuckoo[vs[nv+1]] = vs[nv];
        cuckoo[vs[0]] = us[0];
      }
    }
    Thread.currentThread().interrupt();
  }

  public static void main(String argv[]) {
    assert Cuckoo.NNODES > 0;
    int nthreads = 1;
    int maxsols = 8;
    String header = "";
    int easipct = 50;
    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-e")) {
        easipct = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-h")) {
        header = argv[++i];
      } else if (argv[i].equals("-m")) {
        maxsols = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-t")) {
        nthreads = Integer.parseInt(argv[++i]);
      }
    }
    assert easipct >= 0 && easipct <= 100;
    System.out.println("Looking for " + Cuckoo.PROOFSIZE + "-cycle on cuckoo" + Cuckoo.NODEBITS + "(\"" + header + "\") with " + easipct + "% edges and " + nthreads + " threads");
    CuckooSolve solve = new CuckooSolve(header.getBytes(), easipct * Cuckoo.NNODES / 100, maxsols, nthreads);
  
    Thread[] threads = new Thread[nthreads];
    for (int t = 0; t < nthreads; t++) {
      threads[t] = new Thread(new CuckooSolverTest(t, solve));
      threads[t].start();
    }
    for (int t = 0; t < nthreads; t++) {
      try {
        threads[t].join();
      } catch (InterruptedException e) {
        System.out.println(e);
        System.exit(1);
      }
    }
    for (int s = 0; s < solve.getNsols(); s++) {
      System.out.print("Solution");
      for (int i = 0; i < Cuckoo.PROOFSIZE; i++)
        System.out.print(String.format(" %x", solve.getSols()[s][i]));
      System.out.println("");
    }
  }
}
