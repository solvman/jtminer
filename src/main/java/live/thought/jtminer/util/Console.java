package live.thought.jtminer.util;

import org.fusesource.jansi.AnsiConsole;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.fusesource.jansi.Ansi;

public abstract class Console
{
  protected static int debugLevel;
  protected static final SimpleDateFormat sdf = new SimpleDateFormat("[YYYY-MM-dd HH:mm:ss.SSS] ");
  
  static {
    AnsiConsole.systemInstall();   
  }
  
  public static int getLevel()
  {
    return debugLevel;
  }
  
  public static void setLevel(int level)
  {
    debugLevel = level;
  }
  
  public static void print(Object content)
  {
    System.out.print( Ansi.ansi().render(content.toString()));
  }
  
  public static void println(Object content)
  {
    System.out.println( Ansi.ansi().render(content.toString()));
  }
  
  public static void output(Object content)
  {
    Console.println(sdf.format(new Date()) + content.toString());
  }
  
  public static void debug(Object content, int level)
  {
    if (level <= debugLevel)
    {
      Console.output("@|faint,white " + content.toString() + "|@");
    }
  }
  
  public static void end()
  {
    AnsiConsole.systemUninstall();
  }
  
}
