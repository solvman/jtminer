/*
 * jtminer Java mining software for the Thought Network
 * 
 * Copyright (c) 2018 - 2019, Thought Network LLC
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

import live.thought.thought4j.ThoughtClientInterface.BlockTemplateTransaction;

public class TransactionImpl implements Hexable
{
  String data;
  String hash;

  public TransactionImpl(BlockTemplateTransaction trans)
  {
    data = trans.data();
    hash = trans.hash();
  }
  
  public TransactionImpl()
  {
    
  }

  public byte[] getHex()
  {
    return DataUtils.hexStringToByteArray(data);
  }

  public void setHex(String hex)
  {
    this.data = hex;
  }
  
  public byte[] getHash()
  {
    return DataUtils.hexStringToByteArray(hash);
  }

  public void setHash(String hash)
  {
    this.hash = hash;
  }
}
