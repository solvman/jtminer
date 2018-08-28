package live.thought.jtminer.data;

import java.math.BigInteger;

public class EncodingTest
{

  public static void main(String[] args)
  {
    //BigInteger bi = BigInteger.valueOf(536870912);
    //System.out.println(bi.toString(16));
    
    //Integer i = Integer.valueOf(536870912);
    //System.out.println(Integer.toHexString(i));
    //System.out.println(Integer.reverseBytes(i));
    //System.out.println(Integer.toHexString(Integer.reverseBytes(i)));

    byte[] buf = new byte[4];
    DataUtils.uint32ToByteArrayBE(536870912, buf, 0);
    System.out.println(DataUtils.byteArrayToHexString(buf));
    DataUtils.uint32ToByteArrayLE(536870912, buf, 0);
    System.out.println(DataUtils.byteArrayToHexString(buf));
  }

}
