package live.thought.jtminer.runtime;

import java.math.BigInteger;

import live.thought.jtminer.data.DataUtils;

public class EncodingTest
{

  public static void main(String[] args)
  {
    byte[] ver = new byte[4];
    DataUtils.uint32ToByteArrayLE(1610612736, ver, 0);
    System.out.println(DataUtils.byteArrayToHexString(ver));
    
    
    String hash = "00000000eaba8e4869a63ebe476d0fcd36b418c401bc50621661ce563b1794c8";
    byte[] hashbytes = DataUtils.hexStringToByteArray(hash);
    byte[] reversebytes = DataUtils.reverseBytes(hashbytes);
    String reversehash = DataUtils.byteArrayToHexString(reversebytes);
    System.out.println("Hash: " + hash);
    System.out.println("Reverse: " + reversehash);

   System.out.println(Long.toHexString(Long.reverseBytes(31400000000L)));
   
   byte[] t1 = DataUtils.encodeCompact(126L);
   BigInteger t2 = new BigInteger(t1);
   BigInteger t3 = DataUtils.decodeCompactBits(t2.longValue());
   System.out.println("Value: " + t3.intValue());
    
  }

}
