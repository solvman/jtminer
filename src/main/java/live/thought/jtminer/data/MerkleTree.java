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

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class MerkleTree
{
  protected static List<String> tempTxList = new ArrayList<String>();

  protected MerkleTree()
  {

  }

  /**
   * execute merkle_tree and set root.
   */
  public static String merkle_tree(List<TransactionImpl> transactions, CoinbaseTransaction cbtx)
  {
    String retval = null;

    tempTxList.clear();
    if (null != cbtx)
    {
      tempTxList.add(cbtx.getHex());
    }
    for (TransactionImpl t : transactions)
    {
      tempTxList.add(t.getHex());
    }

    List<String> newTxList = reduce(tempTxList);
    while (newTxList.size() != 1)
    {
      newTxList = reduce(newTxList);
    }

    retval = newTxList.get(0);
    return retval;
  }

  /**
   * return Node Hash List.
   * 
   * @param tempTxList
   * @return
   */
  protected static List<String> reduce(List<String> tempTxList)
  {

    List<String> newTxList = new ArrayList<String>();
    int index = 0;
    while (index < tempTxList.size())
    {
      // left
      String left = tempTxList.get(index);
      index++;

      // right
      String right = "";
      if (index != tempTxList.size())
      {
        right = tempTxList.get(index);
      }

      // sha2 hex value
      String sha2HexValue = getSHA2HexValue(left + right);
      newTxList.add(sha2HexValue);
      index++;

    }

    return newTxList;
  }

  /**
   * Return hex string
   * 
   * @param str
   * @return
   */
  protected static String getSHA2HexValue(String str)
  {
    String retval = "";
    byte[] cipher_byte;
    try
    {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(str.getBytes());
      cipher_byte = md.digest();
      StringBuilder sb = new StringBuilder(2 * cipher_byte.length);
      for (byte b : cipher_byte)
      {
        sb.append(String.format("%02x", b & 0xff));
      }
      retval = sb.toString();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return retval;
  }
}
