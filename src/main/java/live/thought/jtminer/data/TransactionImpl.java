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

import java.util.Date;

import live.thought.thought4j.ThoughtClientInterface.Transaction;

public class TransactionImpl implements Hexable
{
  String account;
  String address;
  String category;
  double amount;
  double fee;
  int    confirmations;
  String blockHash;
  int    blockIndex;
  Date   blockTime;
  String txId;
  Date   time;
  Date   timeReceived;
  String comment;
  String commentTo;
  String hex;

  public TransactionImpl(Transaction trans)
  {
    account = trans.account();
    address = trans.address();
    category = trans.category();
    amount = trans.amount();
    fee = trans.fee();
    confirmations = trans.confirmations();
    blockHash = trans.blockHash();
    blockIndex = trans.blockIndex();
    blockTime = trans.blockTime();
    txId = trans.txId();
    time = trans.time();
    timeReceived = trans.timeReceived();
    comment = trans.comment();
    commentTo = trans.commentTo();
    hex = trans.raw().hex();
  }
  
  public TransactionImpl()
  {
    
  }

  public String getAccount()
  {
    return account;
  }

  public void setAccount(String account)
  {
    this.account = account;
  }

  public String getAddress()
  {
    return address;
  }

  public void setAddress(String address)
  {
    this.address = address;
  }

  public String getCategory()
  {
    return category;
  }

  public void setCategory(String category)
  {
    this.category = category;
  }

  public double getAmount()
  {
    return amount;
  }

  public void setAmount(double amount)
  {
    this.amount = amount;
  }

  public double getFee()
  {
    return fee;
  }

  public void setFee(double fee)
  {
    this.fee = fee;
  }

  public int getConfirmations()
  {
    return confirmations;
  }

  public void setConfirmations(int confirmations)
  {
    this.confirmations = confirmations;
  }

  public String getBlockHash()
  {
    return blockHash;
  }

  public void setBlockHash(String blockHash)
  {
    this.blockHash = blockHash;
  }

  public int getBlockIndex()
  {
    return blockIndex;
  }

  public void setBlockIndex(int blockIndex)
  {
    this.blockIndex = blockIndex;
  }

  public Date getBlockTime()
  {
    return blockTime;
  }

  public void setBlockTime(Date blockTime)
  {
    this.blockTime = blockTime;
  }

  public String getTxId()
  {
    return txId;
  }

  public void setTxId(String txId)
  {
    this.txId = txId;
  }

  public Date getTime()
  {
    return time;
  }

  public void setTime(Date time)
  {
    this.time = time;
  }

  public Date getTimeReceived()
  {
    return timeReceived;
  }

  public void setTimeReceived(Date timeReceived)
  {
    this.timeReceived = timeReceived;
  }

  public String getComment()
  {
    return comment;
  }

  public void setComment(String comment)
  {
    this.comment = comment;
  }

  public String getCommentTo()
  {
    return commentTo;
  }

  public void setCommentTo(String commentTo)
  {
    this.commentTo = commentTo;
  }

  public byte[] getHex()
  {
    return DataUtils.hexStringToByteArray(hex);
  }

  public void setHex(String hex)
  {
    this.hex = hex;
  }
}
