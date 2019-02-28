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

public class ByteArray
{
  protected byte[] content;

  public ByteArray()
  {
    content = null;
  }

  public ByteArray(byte[] content)
  {
    if (null == content)
    {
      this.content = null;
    }
    else
    {
      this.content = new byte[content.length];
      System.arraycopy(content, 0, this.content, 0, content.length);
    }
  }

  public ByteArray(int size)
  {
    this.content = new byte[size];
  }

  public ByteArray append(byte b)
  {
    if (null != this.content)
    {
      int newLength = this.content.length + 1;
      byte[] newContent = new byte[newLength];
      System.arraycopy(this.content, 0, newContent, 0, this.content.length);
      newContent[newContent.length - 1] = b;
      this.content = newContent;
    }
    else
    {
      this.content = new byte[1];
      this.content[0] = b;
    }
    return this;
  }

  public ByteArray append(byte[] b)
  {
    if (null != b)
    {
      if (null != this.content)
      {
        int oldLength = this.content.length;
        int newLength = oldLength + b.length;
        byte[] newContent = new byte[newLength];
        System.arraycopy(this.content, 0, newContent, 0, this.content.length);
        System.arraycopy(b, 0, newContent, oldLength, b.length);
        this.content = newContent;
      }
      else
      {
        this.content = new byte[b.length];
        System.arraycopy(b, 0, this.content, 0, b.length);
      }
    }
    return this;
  }

  public ByteArray set(int index, byte b)
  {
    if (null == this.content || index > this.content.length - 1)
    {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    this.content[index] = b;
    return this;
  }

  public byte[] get()
  {
    return this.content;
  }
}
