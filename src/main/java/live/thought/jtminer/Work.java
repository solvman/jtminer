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
    coinbaseTransaction = new CoinbaseTransaction(blt.height(), blt.coinbasevalue(), Miner.getInstance().getCoinbaseAddress());
    block.setCoinbaseTransaction(coinbaseTransaction);

    data = block.getHeader();
    BigInteger lBits = new BigInteger(DataUtils.hexStringToByteArray(blt.bits()));
    target = DataUtils.decodeCompactBits(lBits.longValue());    
  }

  public boolean submit(ThoughtClientInterface client, int nonce, int[] solution) throws IOException
  {
    boolean retval = false;
   
    block.setNonce(nonce);
    block.setCuckooSolution(solution);
    String blockStr = DataUtils.byteArrayToHexString(block.getHex());
    try
    {
      LOG.finest("Submitting: " + blockStr);
      retval = client.submitBlock(blockStr);
    }
    catch (Exception e)
    {
      LOG.severe(e.getMessage());
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
      //sb.append(String.format("%08X", solution[n]));  
    }
    hasher.update(DataUtils.hexStringToByteArray(sb.toString()));
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
  
  public BlockImpl getBlock()
  {
    return block;
  }

  public BigInteger getTarget()
  {
    return target;
  }

}
