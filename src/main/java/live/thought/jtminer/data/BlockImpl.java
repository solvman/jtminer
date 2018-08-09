/*
 * jtminer Java mining software for the Thought Network
 * 
 * Copyright (c) 2018, Thought Network LLC
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

package live.thought.jtminer.data;

import java.util.ArrayList;
import java.util.List;

import live.thought.thought4j.ThoughtClientInterface.Block;
import live.thought.thought4j.ThoughtClientInterface.BlockTemplate;
import live.thought.thought4j.ThoughtClientInterface.Transaction;

public class BlockImpl implements Hexable
{
  private Block block;
  
  protected String hash;
  protected int confirmations;
  protected int size;
  protected long height;
  protected long version;
  protected String merkleRoot;
  protected List<TransactionImpl> transactions;
  protected long time;
  protected long nonce;
  protected String bits;
  protected double difficulty;
  protected String previousHash;
  protected String chainwork;
  
  protected CoinbaseTransaction coinbase;

  public BlockImpl(Block block)
  {
    this.block = block;
    setHash(block.hash());
    setConfirmations(block.confirmations());
    setSize(block.size());
    setHeight(block.height());
    setVersion(block.version());
    setMerkleRoot(block.merkleRoot());
    // TODO: Transactions
    setTime(block.time().getTime());
    setNonce(block.nonce());
    setBits(block.bits());
    setDifficulty(block.difficulty());
    setPreviousHash(block.previousHash());
    setChainwork(block.chainwork());
  }
  
  public BlockImpl(BlockTemplate blt)
  {
    setHeight(blt.height());
    setVersion(blt.version());
    setTime(blt.curtime());
    setBits(blt.bits());
    setPreviousHash(blt.previousblockhash());
    List<Transaction> trans = blt.transactions();
    transactions = new ArrayList<TransactionImpl>(trans.size());
    for (Transaction t: trans)
    {
      transactions.add(new TransactionImpl(t));
    }
  }

  public String getHash()
  {
    return hash;
  }

  public void setHash(String hash)
  {
    this.hash = hash;
  }

  public int getConfirmations()
  {
    return confirmations;
  }

  public void setConfirmations(int confirmations)
  {
    this.confirmations = confirmations;
  }

  public int getSize()
  {
    return size;
  }

  public void setSize(int size)
  {
    this.size = size;
  }

  public long getHeight()
  {
    return height;
  }

  public void setHeight(long height)
  {
    this.height = height;
  }

  public long getVersion()
  {
    return version;
  }

  public void setVersion(long version)
  {
    this.version = version;
  }

  public String getMerkleRoot()
  {
    return merkleRoot;
  }

  public void setMerkleRoot(String merkleRoot)
  {
    this.merkleRoot = merkleRoot;
  }

  public List<TransactionImpl> getTransactions()
  {
    return transactions;
  }

  public void setTransactions(List<TransactionImpl> transactions)
  {
    this.transactions = transactions;
  }

  public long getTime()
  {
    return time;
  }

  public void setTime(long time)
  {
    this.time = time;
  }

  public long getNonce()
  {
    return nonce;
  }

  public void setNonce(long nonce)
  {
    this.nonce = nonce;
  }

  public String getBits()
  {
    return bits;
  }

  public void setBits(String bits)
  {
    this.bits = bits;
  }

  public double getDifficulty()
  {
    return difficulty;
  }

  public void setDifficulty(double difficulty)
  {
    this.difficulty = difficulty;
  }

  public String getPreviousHash()
  {
    return previousHash;
  }

  public void setPreviousHash(String previousHash)
  {
    this.previousHash = previousHash;
  }

  public String getChainwork()
  {
    return chainwork;
  }

  public void setChainwork(String chainwork)
  {
    this.chainwork = chainwork;
  }

  public Block getBlock()
  {
    return block;
  }
  
  public void addCoinbaseTransaction(CoinbaseTransaction trans)
  {
    coinbase = trans;
  }

  @Override
  public byte[] getHex()
  {
    byte[] data = new byte[80];
    
    int offset = 0;
    DataUtils.uint32ToByteArrayLE(version, data, offset);
    offset += 4;
    
    byte[] prev = DataUtils.hexStringToByteArray(previousHash);
    System.arraycopy(prev, 0, data, offset, prev.length);
    
    String merkle_root = MerkleTree.merkle_tree(getTransactions(), coinbase);
    byte[] merkle_bytes = DataUtils.hexStringToByteArray(merkle_root);
    System.arraycopy(merkle_bytes, 0, data, offset, merkle_bytes.length);
    offset += merkle_bytes.length;
    
    DataUtils.uint32ToByteArrayLE(time, data, offset);
    offset += 4;
    byte[] bitsHex = bits.getBytes();
    System.arraycopy(bitsHex, 0, data, offset, bitsHex.length);
    
    return data;
  }
  
}
