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

import java.util.Arrays;

import live.thought.jtminer.Miner;
import live.thought.thought4j.ThoughtClientInterface.BlockTemplate;

public class CoinbaseTransaction implements Hexable
{
  protected int version = 1;
  protected int inCounter = 1;
  protected long height;
  protected int outCounter = 1;
  protected long value;
  protected String address;
  protected int lockTime = 0;
   
  public CoinbaseTransaction(BlockTemplate blt)
  {
    height = blt.height();
    value = blt.coinbasevalue();
    address = Miner.getInstance().getCoinbaseAddress();
  }
   
  public int getVersion()
  {
    return version;
  }

  public void setVersion(int version)
  {
    this.version = version;
  }

  public int getInCounter()
  {
    return inCounter;
  }

  public void setInCounter(int inCounter)
  {
    this.inCounter = inCounter;
  }

  public long getHeight()
  {
    return height;
  }

  public void setHeight(long height)
  {
    this.height = height;
  }

  public int getOutCounter()
  {
    return outCounter;
  }

  public void setOutCounter(int outCounter)
  {
    this.outCounter = outCounter;
  }

  public long getValue()
  {
    return value;
  }

  public void setValue(long value)
  {
    this.value = value;
  }

  public String getAddress()
  {
    return address;
  }

  public void setAddress(String address)
  {
    this.address = address;
  }

  public int getLockTime()
  {
    return lockTime;
  }

  public void setLockTime(int lockTime)
  {
    this.lockTime = lockTime;
  }

  @Override
  public String getHex()
  {
    byte[] cbtx = new byte[256];
    int cbtx_size = 0;
    // Encode the version
    byte[] ver = Integer.toHexString(Integer.reverseBytes(version)).getBytes();
    System.arraycopy(ver, 0, cbtx, cbtx_size, 4);
    cbtx_size += 4;
    // tx_in counter
    cbtx[cbtx_size] = (byte)0x01;
    cbtx_size += 1;
    // previous tx hash
    Arrays.fill(cbtx, cbtx_size, cbtx_size + 32, (byte)0x00);
    // BIP-34 height
    cbtx_size = 43;
    for (long n = height; n != 0; n >>= 8)
      cbtx[cbtx_size++] = (byte) (n & 0xff);
    /* If the last byte pushed is >= 0x80, then we need to add
       another zero byte to signal that the block height is a
       positive number.  */
    if ((cbtx[cbtx_size - 1] & 0x80) != 0)
      cbtx[cbtx_size++] = 0;
    // Scriptsig length
    cbtx[42] = (byte) (cbtx_size - 43);
    cbtx[41] = (byte) (cbtx_size - 42);
    // Sequence
    byte[] seq = Integer.toHexString(Integer.reverseBytes(0xffffffff)).getBytes();
    System.arraycopy(seq, 0, cbtx, cbtx_size, 4);
    cbtx_size += 4;
    // tx_out counter
    cbtx[cbtx_size] = (byte)0x01;
    cbtx_size += 1;
    // value
    byte[] val = Integer.toHexString(Integer.reverseBytes((int)value)).getBytes();
    System.arraycopy(val, 0, cbtx, cbtx_size, 4);
    cbtx_size += 4;
    val = Integer.toHexString(Integer.reverseBytes((int)value >> 32)).getBytes();
    System.arraycopy(val, 0, cbtx, cbtx_size, 4);
    cbtx_size += 4;
    // Address size
    byte[] len = Integer.toHexString(Integer.reverseBytes(address.length())).getBytes();
    System.arraycopy(len, 0, cbtx, cbtx_size, 4);
    cbtx_size += 4;
    // Payment Address
    System.arraycopy(address.getBytes(), 0, cbtx, cbtx_size, address.length());
    cbtx_size += address.length();
    // Lock time
    byte[] loc = {0x00, 0x00, 0x00, 0x00};
    
    System.arraycopy(loc, 0, cbtx, cbtx_size, 4);
    cbtx_size += 4;
    
    // Not supporting coinbase signature yet
    
    // Thought doesn't have any coinbaseaux flags at the moment
    
    return DataUtils.byteArrayToHexString(cbtx);
  }
  
  
  
}
