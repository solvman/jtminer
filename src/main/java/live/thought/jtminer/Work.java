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

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import live.thought.jtminer.algo.SHA256d;
import live.thought.jtminer.data.BlockImpl;
import live.thought.jtminer.data.CoinbaseTransaction;
import live.thought.jtminer.data.DataUtils;
import live.thought.thought4j.ThoughtClientInterface.BlockTemplate;
import live.thought.thought4j.ThoughtClientInterface;

public class Work
{
  private static final Logger LOG = Logger.getLogger(Work.class.getCanonicalName());
  static
  {
    LOG.setLevel(Level.ALL);
    for (Handler handler : LOG.getParent().getHandlers())
      handler.setLevel(Level.ALL);
  }
  long                        height;
  private byte[]              data;                    // little-endian
  private BigInteger          target;

  private BlockImpl           block;
  private CoinbaseTransaction coinbaseTransaction;

  

  public Work(BlockTemplate blt)
  {
    height = blt.height();
    block = new BlockImpl(blt);
    coinbaseTransaction = new CoinbaseTransaction(blt);
    block.addCoinbaseTransaction(coinbaseTransaction);

    data = block.getHex();
    BigInteger lBits = new BigInteger(DataUtils.hexStringToByteArray(blt.bits()));
    target = DataUtils.decodeCompactBits(lBits.longValue());    
  }

  public boolean submit(ThoughtClientInterface client, int nonce, int[] solution) throws IOException
  {
    boolean retval = false;
    byte[] d = data.clone();
    d[79] = (byte) (nonce >> 0);
    d[78] = (byte) (nonce >> 8);
    d[77] = (byte) (nonce >> 16);
    d[76] = (byte) (nonce >> 24);
    String sData = DataUtils.byteArrayToHexString(d);
    StringBuilder sb = new StringBuilder(sData);
    for (int n = 0; n < solution.length; n++)
    {
      sb.append(Integer.toHexString(Integer.reverseBytes(solution[n])));
    }
    try
    {
      LOG.finest("Submitting: " + sb.toString());
      client.submitBlock(sb.toString());
      retval = true;
    }
    catch (Exception e)
    {
    	e.printStackTrace(System.err);
      
    }
    return retval;
  }

  public boolean meetsTarget(int nonce, int[] solution, SHA256d hasher) throws GeneralSecurityException
  {
    boolean retval = false;
    StringBuilder sb = new StringBuilder();
    for (int n = 0; n < solution.length; n++)
    {
      sb.append(String.format("%08X", Integer.reverseBytes(solution[n])));  
    }

    byte[] hash = hasher.doubleDigest();
    BigInteger hashValue = new BigInteger(hash);
    
    if (hashValue.compareTo(BigInteger.ZERO) == -1)
    {
      retval = false;
    }
    else
    {
      retval = (hashValue.compareTo(target) == 1) ? false : true; 
    }
    
    return retval;  
  }

  public byte[] getData()
  {
    return data;
  }

  public BigInteger getTarget()
  {
    return target;
  }

}
