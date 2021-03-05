package live.thought.jtminer.runtime;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import live.thought.jtminer.data.BlockImpl;
import live.thought.jtminer.data.CoinbaseTransaction;
import live.thought.jtminer.data.DataUtils;
import live.thought.thought4j.ThoughtClientInterface;
import live.thought.thought4j.ThoughtClientInterface.BlockTemplate;
import live.thought.thought4j.ThoughtRPCClient;

public class ClientTest
{
  private static final Logger logger = Logger.getLogger(ClientTest.class.getCanonicalName());
  static
  {
    logger.setLevel(Level.ALL);
    for (Handler handler : logger.getParent().getHandlers())
      handler.setLevel(Level.ALL);
  }

  public static void main(String[] args) throws Exception
  {
    ThoughtClientInterface b = new ThoughtRPCClient("http://neo:22Monkeys!@192.168.183.184:11617/");

    System.out.println(b.getBlockChainInfo());
    System.out.println(b.getMiningInfo());
    BlockTemplate bl = b.getBlockTemplate();
    System.out.println(bl);

    BlockImpl bi = new BlockImpl(bl);
    
    CoinbaseTransaction coinbaseTransaction = new CoinbaseTransaction(bl.height(), bl.coinbasevalue(), "kzSJ2PorYyS5zY6VuMygsSiae7wTBRBm5W");
    bi.setCoinbaseTransaction(coinbaseTransaction);

    byte[] data = bi.getHeader();
    System.out.println(DataUtils.byteArrayToHexString(data));
    
  }
}
