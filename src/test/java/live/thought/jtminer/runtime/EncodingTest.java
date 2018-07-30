package live.thought.jtminer.runtime;

import live.thought.jtminer.data.DataUtils;

public class EncodingTest
{

  public static void main(String[] args)
  {
    System.out.println(Integer.toHexString(Integer.reverseBytes(2)));
    byte[] ver = new byte[4];
    DataUtils.uint32ToByteArrayLE(536870912L, ver, 0);
    System.out.println(new String(ver));
    

  }

}
