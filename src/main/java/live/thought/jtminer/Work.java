/*
 * jtminer Java mining software for the Thought Network
 * 
 * Copyright (c) 2018 - 2019, Thought Network LLC
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import live.thought.jtminer.algo.SHA256d;
import live.thought.jtminer.data.BlockImpl;
import live.thought.jtminer.data.CoinbaseTransaction;
import live.thought.jtminer.data.DataUtils;
import live.thought.jtminer.data.PaymentObject;
import live.thought.thought4j.ThoughtClientInterface;
import live.thought.thought4j.ThoughtClientInterface.BlockTemplate;
import live.thought.thought4j.ThoughtClientInterface.Masternode;

public class Work
{
  private static final Logger LOG = Logger.getLogger(Work.class.getCanonicalName());

  long                        height;
  private BigInteger          target;

  private BlockImpl           block;
  private CoinbaseTransaction coinbaseTransaction;
  SHA256d                     localHasher    = new SHA256d(32);
  

  public Work(BlockTemplate blt)
  {
    height = blt.height();
    block = new BlockImpl(blt);
    coinbaseTransaction = new CoinbaseTransaction(blt.height(), blt.coinbasevalue(), Miner.getInstance().getCoinbaseAddress());
    
    try 
    {
      if (blt.masternode_payments_started())
      {
        List<Masternode> outputs = blt.masternode();
        for (Masternode m : outputs)
        {
          PaymentObject p = new PaymentObject();
          p.setPayee(m.payee());
          p.setScript(m.script());
          p.setValue(m.amount());
          coinbaseTransaction.addExtraPayment(p);
        }      
      }
    }
    catch (Exception e)
    {
      // Not thought_dash daemon, so ignore this error
    }
    
    block.setCoinbaseTransaction(coinbaseTransaction);

    BigInteger lBits = new BigInteger(DataUtils.hexStringToByteArray(blt.bits()));
    target = DataUtils.decodeCompactBits(lBits.longValue());    
    LOG.setLevel(Level.ALL);
  }

  public boolean submit(ThoughtClientInterface client, int[] solution) throws IOException
  {
    boolean retval = false;

    block.setCuckooSolution(solution);
    String blockStr = DataUtils.byteArrayToHexString(block.getHex());
    try
    {
      localHasher.update(block.getHeader());
      String result = client.submitBlock(blockStr);
      if (null == result)
      {
        retval = true;
      }
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
    }

    hasher.update(DataUtils.hexStringToByteArray(sb.toString()));
    byte[] hash = hasher.doubleDigest();

    BigInteger hashValue = new BigInteger(DataUtils.reverseBytes(hash));
    
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
  
  public BlockImpl getBlock()
  {
    return block;
  }

  public BigInteger getTarget()
  {
    return target;
  }

}
