package org.pdown.core.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.internal.StringUtil;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;
import org.pdown.core.boot.HttpDownBootstrap;
import org.pdown.core.entity.HttpHeadsInfo;
import org.pdown.core.entity.HttpRequestInfo;
import org.pdown.core.entity.HttpRequestInfo.HttpVer;
import org.pdown.core.entity.HttpResponseInfo;
import org.pdown.core.exception.BootstrapResolveException;
import org.pdown.core.proxy.ProxyConfig;
import org.pdown.core.proxy.ProxyHandleFactory;
import org.pdown.core.util.ProtoUtil.RequestProto;

public class HttpDownUtil {

  private static SslContext sslContext;

  public static SslContext getSslContext() throws SSLException {
    if (sslContext == null) {
      synchronized (HttpDownUtil.class) {
        if (sslContext == null) {
          sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        }
      }
    }
    return sslContext;
  }

  /**
   * 检测响应相关信息
   */
  public static HttpResponseInfo getHttpResponseInfo(HttpRequest httpRequest, HttpHeaders resHeaders, ProxyConfig proxyConfig, NioEventLoopGroup loopGroup)
      throws Exception {
    HttpResponse httpResponse = null;
    if (resHeaders == null) {
      httpResponse = getResponse(httpRequest, proxyConfig, loopGroup);
      //处理重定向
      if ((httpResponse.status().code() + "").indexOf("30") == 0) {
        //TODO 302重定向乱码 https://link.gimhoy.com/googledrive/aHR0cHM6Ly9kcml2ZS5nb29nbGUuY29tL29wZW4/aWQ9MThlVmNKeEhwaE40RUpGTUowSk10bWNXOVhCcWJhVE1k.jpg
        String redirectUrl = httpResponse.headers().get(HttpHeaderNames.LOCATION);
        HttpRequestInfo requestInfo = (HttpRequestInfo) httpRequest;
        //重定向cookie设置
        List<String> setCookies = httpResponse.headers().getAll(HttpHeaderNames.SET_COOKIE);
        if (setCookies != null && setCookies.size() > 0) {
          StringBuilder requestCookie = new StringBuilder();
          String oldRequestCookie = requestInfo.headers().get(HttpHeaderNames.COOKIE);
          if (oldRequestCookie != null) {
            requestCookie.append(oldRequestCookie);
          }
          String split = String.valueOf((char) HttpConstants.SEMICOLON) + String.valueOf(HttpConstants.SP_CHAR);
          for (String setCookie : setCookies) {
            String cookieNV = setCookie.split(split)[0];
            if (requestCookie.length() > 0) {
              requestCookie.append(split);
            }
            requestCookie.append(cookieNV);
          }
          requestInfo.headers().set(HttpHeaderNames.COOKIE, requestCookie.toString());
        }
        requestInfo.headers().remove(HttpHeaderNames.HOST);
        requestInfo.setUri(redirectUrl);
        RequestProto requestProto = ProtoUtil.getRequestProto(requestInfo);
        requestInfo.headers().set(HttpHeaderNames.HOST, requestProto.getHost());
        requestInfo.setRequestProto(requestProto);
        return getHttpResponseInfo(httpRequest, null, proxyConfig, loopGroup);
      }
    }
    if (httpResponse == null) {
      httpResponse = getResponse(httpRequest, proxyConfig, loopGroup);
    }
    if (httpResponse.status().code() != HttpResponseStatus.OK.code()
        && httpResponse.status().code() != HttpResponseStatus.PARTIAL_CONTENT.code()) {
      throw new BootstrapResolveException("Status code exception:" + httpResponse.status().code());
    }
    return parseResponse(httpRequest, httpResponse);
  }

  public static String getDownFileName(HttpRequest httpRequest, HttpHeaders resHeaders) {
    String fileName = null;
    String disposition = resHeaders.get(HttpHeaderNames.CONTENT_DISPOSITION);
    if (disposition != null) {
      //attachment;filename=1.rar   attachment;filename=*UTF-8''1.rar
      Pattern pattern = Pattern.compile("^.*filename\\*?=\"?(?:.*'')?([^\"]*)\"?$");
      Matcher matcher = pattern.matcher(disposition);
      if (matcher.find()) {
        char[] chs = matcher.group(1).toCharArray();
        byte[] bts = new byte[chs.length];
        //netty将byte转成了char，导致中文乱码 HttpObjectDecoder(:803)
        for (int i = 0; i < chs.length; i++) {
          bts[i] = (byte) chs[i];
        }
        try {
          fileName = new String(bts, "UTF-8");
          fileName = URLDecoder.decode(fileName, "UTF-8");
        } catch (Exception e) {

        }
      }
    }
    if (fileName == null) {
      Pattern pattern = Pattern.compile("^.*/([^/?]*\\.[^./?]+)(\\?[^?]*)?$");
      Matcher matcher = pattern.matcher(httpRequest.uri());
      if (matcher.find()) {
        fileName = matcher.group(1);
      } else {
        fileName = "Unknown";
      }
    }
    return fileName;
  }

  /**
   * 取当前请求的ContentLength
   */
  public static long getDownContentSize(HttpHeaders resHeaders) {
    String contentRange = resHeaders.get(HttpHeaderNames.CONTENT_RANGE);
    if (contentRange != null) {
      Pattern pattern = Pattern.compile("^[^\\d]*(\\d+)-(\\d+)/.*$");
      Matcher matcher = pattern.matcher(contentRange);
      if (matcher.find()) {
        long startSize = Long.parseLong(matcher.group(1));
        long endSize = Long.parseLong(matcher.group(2));
        return endSize - startSize + 1;
      }
    } else {
      String contentLength = resHeaders.get(HttpHeaderNames.CONTENT_LENGTH);
      if (contentLength != null) {
        return Long.valueOf(resHeaders.get(HttpHeaderNames.CONTENT_LENGTH));
      }
    }
    return 0;
  }

  /**
   * 取请求下载文件的总大小
   */
  public static long getDownFileSize(HttpHeaders resHeaders) {
    String contentRange = resHeaders.get(HttpHeaderNames.CONTENT_RANGE);
    if (contentRange != null) {
      Pattern pattern = Pattern.compile("^.*/(\\d+).*$");
      Matcher matcher = pattern.matcher(contentRange);
      if (matcher.find()) {
        return Long.parseLong(matcher.group(1));
      }
    } else {
      String contentLength = resHeaders.get(HttpHeaderNames.CONTENT_LENGTH);
      if (contentLength != null) {
        return Long.valueOf(resHeaders.get(HttpHeaderNames.CONTENT_LENGTH));
      }
    }
    return 0;
  }

  public static HttpResponseInfo parseResponse(HttpRequest httpRequest, HttpResponse httpResponse) {
    HttpResponseInfo responseInfo = new HttpResponseInfo();
    responseInfo.setFileName(getDownFileName(httpRequest, httpResponse.headers()));
    responseInfo.setTotalSize(getDownFileSize(httpResponse.headers()));
    //chunked编码不支持断点下载
    if (httpResponse.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
      //206表示支持断点下载
      if (httpResponse.status().equals(HttpResponseStatus.PARTIAL_CONTENT)) {
        responseInfo.setSupportRange(true);
      }
    }
    return responseInfo;
  }

  /**
   * 取请求响应
   */
  public static HttpResponse getResponse(HttpRequest httpRequest, ProxyConfig proxyConfig, NioEventLoopGroup loopGroup) throws Exception {
    final HttpResponse[] httpResponses = new HttpResponse[1];
    CountDownLatch cdl = new CountDownLatch(1);
    HttpRequestInfo requestInfo = (HttpRequestInfo) httpRequest;
    RequestProto requestProto = requestInfo.requestProto();
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(loopGroup) // 注册线程池
        .channel(NioSocketChannel.class) // 使用NioSocketChannel来作为连接用的channel类
        .handler(new ChannelInitializer() {

          @Override
          protected void initChannel(Channel ch) throws Exception {
            if (proxyConfig != null) {
              ch.pipeline().addLast(ProxyHandleFactory.build(proxyConfig));
            }
            if (requestProto.getSsl()) {
              ch.pipeline().addLast(getSslContext().newHandler(ch.alloc(), requestProto.getHost(), requestProto.getPort()));
            }
            ch.pipeline().addLast("httpCodec", new HttpClientCodec());
            ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {

              @Override
              public void channelRead(ChannelHandlerContext ctx0, Object msg0)
                  throws Exception {
                if (msg0 instanceof HttpResponse) {
                  HttpResponse httpResponse = (HttpResponse) msg0;
                  httpResponses[0] = httpResponse;
                  ctx0.channel().close();
                  cdl.countDown();
                }
              }


            });
          }

        });
    if (proxyConfig != null) {
      //代理服务器解析DNS和连接
      bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
    }
    ChannelFuture cf = bootstrap.connect(requestProto.getHost(), requestProto.getPort());
    cf.addListener((ChannelFutureListener) future -> {
      if (future.isSuccess()) {
        //请求下载一个字节测试是否支持断点下载
        httpRequest.headers().set(HttpHeaderNames.RANGE, "bytes=0-0");
        cf.channel().writeAndFlush(httpRequest);
        if (requestInfo.content() != null) {
          //请求体写入
          HttpContent content = new DefaultLastHttpContent();
          content.content().writeBytes(requestInfo.content());
          cf.channel().writeAndFlush(content);
        }
      } else {
        cdl.countDown();
      }
    });
    cdl.await(60, TimeUnit.SECONDS);
    if (httpResponses[0] == null) {
      throw new TimeoutException("getResponse timeout");
    }
    httpRequest.headers().remove(HttpHeaderNames.RANGE);
    return httpResponses[0];
  }

  /**
   * 关闭tcp连接和文件描述符
   */
  public static void safeClose(Channel channel, Closeable... fileChannels) throws IOException {
    if (channel != null && channel.isOpen()) {
      //关闭旧的下载连接
      channel.close();
    }
    if (fileChannels != null && fileChannels.length > 0) {
      for (Closeable closeable : fileChannels) {
        if (closeable != null) {
          //关闭旧的下载文件连接
          closeable.close();
        }
      }
    }
  }

  public static RequestProto parseRequestProto(String url) throws MalformedURLException {
    return parseRequestProto(new URL(url));
  }

  public static RequestProto parseRequestProto(URL url) {
    int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
    return new RequestProto(url.getHost(), port, url.getProtocol().equalsIgnoreCase("https"));
  }

  public static HttpRequestInfo buildRequest(String method, String url, Map<String, String> heads, String body)
      throws MalformedURLException {
    URL u = new URL(url);
    HttpHeadsInfo headsInfo = new HttpHeadsInfo();
    headsInfo.add("Host", u.getHost());
    headsInfo.add("Connection", "keep-alive");
    headsInfo.add("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.75 Safari/537.36");
    headsInfo.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headsInfo.add("Referer", u.getHost());
    if (heads != null) {
      for (Entry<String, String> entry : heads.entrySet()) {
        headsInfo.set(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
      }
    }
    byte[] content = null;
    if (body != null && body.length() > 0) {
      content = body.getBytes();
      headsInfo.add("Content-Length", content.length);
    }
    HttpMethod httpMethod = StringUtil.isNullOrEmpty(method) ? HttpMethod.GET : HttpMethod.valueOf(method.toUpperCase());
    HttpRequestInfo requestInfo = new HttpRequestInfo(HttpVer.HTTP_1_1, httpMethod, u.getFile(), headsInfo, content);
    requestInfo.setRequestProto(parseRequestProto(u));
    return requestInfo;
  }

  public static HttpRequestInfo buildRequest(String url, Map<String, String> heads, String body)
      throws MalformedURLException {
    return buildRequest(null, url, heads, body);
  }

  public static HttpRequestInfo buildRequest(String url, Map<String, String> heads)
      throws MalformedURLException {
    return buildRequest(url, heads, null);
  }

  public static HttpRequestInfo buildRequest(String url)
      throws MalformedURLException {
    return buildRequest(url, null);
  }

  public static String getUrl(HttpRequest request) {
    String host = request.headers().get(HttpHeaderNames.HOST);
    String url;
    if (request.uri().indexOf("/") == 0) {
      if (request.uri().length() > 1) {
        url = host + request.uri();
      } else {
        url = host;
      }
    } else {
      url = request.uri();
    }
    return url;
  }

  /**
   * 取下载文件绝对路径
   */
  public static String getTaskFilePath(HttpDownBootstrap httpDownBootstrap) {
    return httpDownBootstrap.getDownConfig().getFilePath() + File.separator + httpDownBootstrap.getResponse().getFileName();
  }
}
