package io.pdown.core.boot;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.StringUtil;
import io.pdown.core.entity.HttpDownConfigInfo;
import io.pdown.core.entity.HttpRequestInfo;
import io.pdown.core.exception.BootstrapBuildException;
import io.pdown.core.util.HttpDownUtil;
import java.util.Map;

/**
 * 通过URL创建一个下载任务
 */
public class URLHttpDownBootstrapBuilder extends HttpDownBootstrapBuilder {

  private String url;
  private Map<String, String> heads;
  private String body;


  public URLHttpDownBootstrapBuilder(String url, Map<String, String> heads, String body) {
    this.url = url;
    this.heads = heads;
    this.body = body;
  }

  public URLHttpDownBootstrapBuilder url(String url) {
    this.url = url;
    return this;
  }

  public URLHttpDownBootstrapBuilder heads(Map<String, String> heads) {
    this.heads = heads;
    return this;
  }

  public URLHttpDownBootstrapBuilder body(String body) {
    this.body = body;
    return this;
  }

  @Override
  public HttpDownBootstrap build() {
    try {
      HttpRequestInfo request = HttpDownUtil.buildGetRequest(url, heads, body);
      request(request);
      if (getLoopGroup() == null) {
        loopGroup(new NioEventLoopGroup(1));
      }
      HttpDownConfigInfo parseDownConfig = HttpDownUtil.getHttpDownConfigInfo(request, null, getProxyConfig(), getLoopGroup());
      if (getDownConfig() == null) {
        downConfig(parseDownConfig);
      } else {
        if (StringUtil.isNullOrEmpty(getDownConfig().getFileName())) {
          getDownConfig().setFileName(parseDownConfig.getFileName());
        }
        getDownConfig().setSupportRange(parseDownConfig.isSupportRange());
        getDownConfig().setTotalSize(parseDownConfig.getTotalSize());
      }
    } catch (Exception e) {
      getLoopGroup().shutdownGracefully();
      throw new BootstrapBuildException("build URLHttpDownBootstrap error", e);
    }
    return super.build();
  }
}
