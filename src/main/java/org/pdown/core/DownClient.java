package org.pdown.core;

import io.netty.util.internal.StringUtil;
import org.pdown.core.boot.HttpDownBootstrap;
import org.pdown.core.boot.URLHttpDownBootstrapBuilder;
import org.pdown.core.dispatch.ConsoleHttpDownCallback;
import org.pdown.core.entity.HttpDownConfigInfo;
import org.pdown.core.entity.HttpResponseInfo;
import org.pdown.core.exception.BootstrapBuildException;
import org.pdown.core.proxy.ProxyConfig;
import org.pdown.core.proxy.ProxyType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class DownClient {

  public static void start(String[] args) {
    HelpFormatter formatter = new HelpFormatter();
    Options options = new Options();
    options.addOption(Option.builder("U")
        .longOpt("url")
        .hasArg()
        .desc("Set the request URL.")
        .build());
    options.addOption(Option.builder("H")
        .longOpt("heads")
        .hasArg()
        .desc("Set the HTTP request head<s>.")
        .build());
    options.addOption(Option.builder("B")
        .longOpt("body")
        .hasArg()
        .desc("Set the HTTP request body.")
        .build());
    options.addOption(Option.builder("P")
        .longOpt("path")
        .hasArg()
        .desc("Set download path.")
        .build());
    options.addOption(Option.builder("N")
        .longOpt("name")
        .hasArg()
        .desc("Set the name of the download file.")
        .build());
    options.addOption(Option.builder("C")
        .longOpt("connections")
        .hasArg()
        .desc("Set the number of download connections.")
        .build());
    options.addOption(Option.builder("S")
        .longOpt("speedLimit")
        .hasArg()
        .desc("Set download maximum speed limit(B/S).")
        .build());
    options.addOption(Option.builder("X")
        .longOpt("proxy")
        .hasArg()
        .desc("[protocol://]host:port Set proxy,support HTTP,SOCKS4,SOCKS5.")
        .build());
    options.addOption(Option.builder("h")
        .longOpt("help")
        .desc("See the help.")
        .build());
    if (args == null || args.length == 0) {
      formatter.printHelp("parse error:", options);
      return;
    }
    if (args[0].trim().charAt(0) != '-' && !args[0].equals("-h") && !args[0].equals("--help")) {
      args[0] = "-U=" + args[0];
    }
    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine line = parser.parse(options, args);
      if (line.hasOption("h")) {
        formatter.printHelp("pdDown <url> <options>", options);
        return;
      }
      URLHttpDownBootstrapBuilder builder = HttpDownBootstrap.builder(line.getOptionValue("U"));
      String[] headsStr = line.getOptionValues("H");
      if (headsStr != null && headsStr.length > 0) {
        Map<String, String> heads = new LinkedHashMap<>();
        for (String headStr : headsStr) {
          String[] headArray = headStr.split(":");
          heads.put(headArray[0], headArray[1]);
        }
        builder.heads(heads);
      }
      builder.body(line.getOptionValue("B"));
      if (line.hasOption("N")) {
        builder.response(new HttpResponseInfo(line.getOptionValue("N")));
      }
      builder.downConfig(new HttpDownConfigInfo()
          .setFilePath(line.getOptionValue("P"))
          .setConnections(Integer.parseInt(line.getOptionValue("C", "0")))
          .setSpeedLimit(Integer.parseInt(line.getOptionValue("S", "0"))));
      String proxy = line.getOptionValue("X");
      if (!StringUtil.isNullOrEmpty(proxy)) {
        ProxyType proxyType = ProxyType.HTTP;
        String[] proxyArray;
        int protocolIndex = proxy.indexOf("://");
        if (protocolIndex != -1) {
          proxyType = ProxyType.valueOf(proxy.substring(0, protocolIndex).toUpperCase());
          proxyArray = proxy.substring(protocolIndex + 3).split(":");
        } else {
          proxyArray = proxy.split(":");
        }
        builder.proxyConfig(new ProxyConfig(proxyType, proxyArray[0], Integer.parseInt(proxyArray[1])));
      }
      HttpDownBootstrap bootstrap = builder.callback(new ConsoleHttpDownCallback()).build();
      bootstrap.start();
    } catch (ParseException e) {
      formatter.printHelp("Unrecognized option", options);
    }
  }

  public static void main(String[] args) {
    try {
      start(args);
    } catch (BootstrapBuildException e) {
      if (e.getCause() instanceof TimeoutException) {
        System.out.println("Connection failed, please check the network.");
      } else {
        e.printStackTrace();
      }
    }
  }
}
