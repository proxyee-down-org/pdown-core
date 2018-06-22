package io.pdown.core.util;

import java.io.IOException;
import java.net.ServerSocket;

public class OsUtil {

  /**
   * 获取空闲端口号
   */
  public static int getFreePort(int defaultPort) throws Exception {
    int port;
    ServerSocket serverSocket1 = null;
    try {
      serverSocket1 = new ServerSocket(defaultPort);
      port = serverSocket1.getLocalPort();
    } catch (Exception e) {
      ServerSocket serverSocket2 = null;
      try {
        serverSocket2 = new ServerSocket(0);
        port = serverSocket2.getLocalPort();
      } catch (IOException e1) {
        throw e1;
      } finally {
        if (serverSocket2 != null) {
          serverSocket2.close();
        }
      }
    } finally {
      if (serverSocket1 != null) {
        serverSocket1.close();
      }
    }
    return port;
  }

  /**
   * 检查端口号是否被占用
   */
  public static boolean isBusyPort(int port) {
    boolean ret = true;
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(port);
      ret = false;
    } catch (Exception e) {
    } finally {
      if (serverSocket != null) {
        try {
          serverSocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return ret;
  }

  private static final String OS = System.getProperty("os.name").toLowerCase();

  public static boolean isWindows() {
    return OS.indexOf("win") >= 0;
  }

  public static boolean isMac() {
    return OS.indexOf("mac") >= 0;
  }

  public static boolean isUnix() {
    return OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") >= 0;
  }

  public static boolean isSolaris() {
    return (OS.indexOf("sunos") >= 0);
  }

  private static final String ARCH = System.getProperty("sun.arch.data.model");

  public static boolean is64() {
    return "64".equals(ARCH);
  }

  public static boolean is32() {
    return "32".equals(ARCH);
  }
}
