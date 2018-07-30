/*
 * jtminer Java mining software for the Thought Network
 * 
 * Copyright (c) 2018, Thought Network LLC
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
package live.thought.jtminer.runtime;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import live.thought.jtminer.algo.Cuckoo;

public class CuckooTest
{
  public static void main(String argv[])
  {
    String header = "";
    int i, easipct = 100;
    for (i = 0; i < argv.length; i++)
    {
      if (argv[i].equals("-e"))
      {
        easipct = Integer.parseInt(argv[++i]);
      }
      else if (argv[i].equals("-h"))
      {
        header = argv[++i];
      }
    }
    System.out.println(
        "Verifying size " + Cuckoo.PROOFSIZE + " proof for cuckoo" + Cuckoo.NODEBITS + "(\"" + header + "\") with " + easipct + "% edges");
    Scanner sc = new Scanner(System.in);
    sc.next();
    int nonces[] = new int[Cuckoo.PROOFSIZE];
    for (int n = 0; n < Cuckoo.PROOFSIZE; n++)
    {
      nonces[n] = Integer.parseInt(sc.next(), 16);
    }
    sc.close();
    int easiness = (int) (easipct * Cuckoo.NNODES / 100L);
    Cuckoo cuckoo = new Cuckoo(header.getBytes());
    Boolean ok = cuckoo.verify(nonces, easiness);
    if (!ok)
    {
      System.out.println("FAILED");
      System.exit(1);
    }
    System.out.print("Verified with cyclehash ");
    ByteBuffer buf = ByteBuffer.allocate(Cuckoo.PROOFSIZE * 8);
    buf.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(nonces);
    byte[] cyclehash;
    try
    {
      cyclehash = MessageDigest.getInstance("SHA-256").digest(buf.array());
      for (i = 0; i < 32; i++)
        System.out.print(String.format("%02x", ((int) cyclehash[i] & 0xff)));
      System.out.println("");
    }
    catch (NoSuchAlgorithmException e)
    {
      System.out.println(e);
      System.exit(1);
    }
  }
}
