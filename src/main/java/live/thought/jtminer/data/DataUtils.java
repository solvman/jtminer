/*
 * jtminer Java mining software for the Thought Network
 * 
 * Copyright (c) 2018, Thought Network LLC
 * 
 * Based on code from Litecoin JMiner
 * Copyright 2011  LitecoinPool.org
 * 
 * Contains code from bitcoinj-minimal
 * Copyright 2011 Google Inc.
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

import java.math.BigInteger;

public class DataUtils
{
  public static String byteArrayToHexString(byte[] b)
  {
    StringBuilder sb = new StringBuilder(80);
    for (int i = 0; i < b.length; i++)
      sb.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
    return sb.toString();
  }

  public static byte[] hexStringToByteArray(String s)
  {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2)
    {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  private final static char[] BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
  private static int[]        toInt           = new int[128];

  static
  {
    for (int i = 0; i < BASE64_ALPHABET.length; i++)
    {
      toInt[BASE64_ALPHABET[i]] = i;
    }
  }

  public static String stringToBase64(String str)
  {
    byte[] buf = str.getBytes();
    int size = buf.length;
    char[] ar = new char[((size + 2) / 3) * 4];
    int a = 0;
    int i = 0;
    while (i < size)
    {
      byte b0 = buf[i++];
      byte b1 = (i < size) ? buf[i++] : 0;
      byte b2 = (i < size) ? buf[i++] : 0;
      ar[a++] = BASE64_ALPHABET[(b0 >> 2) & 0x3f];
      ar[a++] = BASE64_ALPHABET[((b0 << 4) | ((b1 & 0xFF) >> 4)) & 0x3f];
      ar[a++] = BASE64_ALPHABET[((b1 << 2) | ((b2 & 0xFF) >> 6)) & 0x3f];
      ar[a++] = BASE64_ALPHABET[b2 & 0x3f];
    }
    switch (size % 3)
    {
      case 1:
        ar[--a] = '=';
      case 2:
        ar[--a] = '=';
    }
    return new String(ar);
  }

  public static void uint32ToByteArrayBE(long val, byte[] out, int offset)
  {
    out[offset + 0] = (byte) (0xFF & (val >> 24));
    out[offset + 1] = (byte) (0xFF & (val >> 16));
    out[offset + 2] = (byte) (0xFF & (val >> 8));
    out[offset + 3] = (byte) (0xFF & (val >> 0));
  }

  public static void uint32ToByteArrayLE(long val, byte[] out, int offset)
  {
    out[offset + 0] = (byte) (0xFF & (val >> 0));
    out[offset + 1] = (byte) (0xFF & (val >> 8));
    out[offset + 2] = (byte) (0xFF & (val >> 16));
    out[offset + 3] = (byte) (0xFF & (val >> 24));
  }

  public static long readUint32(byte[] bytes, int offset)
  {
    return ((bytes[offset++] & 0xFFL) << 0) | ((bytes[offset++] & 0xFFL) << 8) | ((bytes[offset++] & 0xFFL) << 16)
        | ((bytes[offset] & 0xFFL) << 24);
  }

  public static long readUint32BE(byte[] bytes, int offset)
  {
    return ((bytes[offset + 0] & 0xFFL) << 24) | ((bytes[offset + 1] & 0xFFL) << 16) | ((bytes[offset + 2] & 0xFFL) << 8)
        | ((bytes[offset + 3] & 0xFFL) << 0);
  }

  /**
   * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They
   * consist of a 4 byte big endian length field, followed by the stated number of
   * bytes representing the number in big endian format.
   */
  public static BigInteger decodeMPI(byte[] mpi)
  {
    int length = (int) readUint32BE(mpi, 0);
    byte[] buf = new byte[length];
    System.arraycopy(mpi, 4, buf, 0, length);
    return new BigInteger(buf);
  }

  // The representation of nBits uses another home-brew encoding, as a way to
  // represent a large
  // hash value in only 32 bits.
  public static BigInteger decodeCompactBits(long compact)
  {
    int size = ((int) (compact >> 24)) & 0xFF;
    byte[] bytes = new byte[4 + size];
    bytes[3] = (byte) size;
    if (size >= 1)
      bytes[4] = (byte) ((compact >> 16) & 0xFF);
    if (size >= 2)
      bytes[5] = (byte) ((compact >> 8) & 0xFF);
    if (size >= 3)
      bytes[6] = (byte) ((compact >> 0) & 0xFF);
    return decodeMPI(bytes);
  }
  
  public static byte[] reverseBytes(byte[] bytes) {
    byte[] buf = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++)
        buf[i] = bytes[bytes.length - 1 - i];
    return buf;
}
}
