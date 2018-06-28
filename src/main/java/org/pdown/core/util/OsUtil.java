package org.pdown.core.util;

import java.io.IOException;
import java.net.ServerSocket;

public class OsUtil {

  /**
   * 查看指定的端口号是否空闲，若空闲则返回否则返回一个随机的空闲端口号
   */
  public static int getFreePort(int defaultPort) throws IOException {
    try (
        ServerSocket serverSocket = new ServerSocket(defaultPort)
    ) {
      return serverSocket.getLocalPort();
    } catch (IOException e) {
      return getFreePort();
    }
  }

  /**
   * 获取空闲端口号
   */
  public static int getFreePort() throws IOException {
    try (
        ServerSocket serverSocket = new ServerSocket(0)
    ) {
      return serverSocket.getLocalPort();
    }
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
