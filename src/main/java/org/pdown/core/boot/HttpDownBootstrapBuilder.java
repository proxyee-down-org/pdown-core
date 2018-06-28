package org.pdown.core.boot;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.StringUtil;
import org.pdown.core.dispatch.HttpDownCallback;
import org.pdown.core.entity.HttpDownConfigInfo;
import org.pdown.core.entity.HttpRequestInfo;
import org.pdown.core.entity.HttpResponseInfo;
import org.pdown.core.entity.TaskInfo;
import org.pdown.core.exception.BootstrapBuildException;
import org.pdown.core.proxy.ProxyConfig;

/**
 * 用于创建一个HTTP下载器
 */
public class HttpDownBootstrapBuilder {

  private HttpRequestInfo request;
  private HttpResponseInfo response;
  private HttpDownConfigInfo downConfig;
  private ProxyConfig proxyConfig;
  private TaskInfo taskInfo;
  private NioEventLoopGroup loopGroup;
  private HttpDownCallback callback;

  /**
   * 任务http请求信息 默认值：null
   */
  public HttpDownBootstrapBuilder request(HttpRequestInfo request) {
    this.request = request;
    return this;
  }

  /**
   * 任务http响应信息 默认值：null
   */
  public HttpDownBootstrapBuilder response(HttpResponseInfo response) {
    this.response = response;
    return this;
  }

  /**
   * 任务配置信息 默认:new HttpDownConfigInfo()
   */
  public HttpDownBootstrapBuilder downConfig(HttpDownConfigInfo downConfig) {
    this.downConfig = downConfig;
    return this;
  }

  /**
   * 任务二级代理信息 默认:null
   */
  public HttpDownBootstrapBuilder proxyConfig(ProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
    return this;
  }

  /**
   * 任务下载记录 默认值：null
   */
  public HttpDownBootstrapBuilder taskInfo(TaskInfo taskInfo) {
    this.taskInfo = taskInfo;
    return this;
  }

  /**
   * 线程池设置 默认值：new NioEventLoopGroup(1)
   */
  public HttpDownBootstrapBuilder loopGroup(NioEventLoopGroup loopGroup) {
    this.loopGroup = loopGroup;
    return this;
  }

  /**
   * 下载回调类
   */
  public HttpDownBootstrapBuilder callback(HttpDownCallback callback) {
    this.callback = callback;
    return this;
  }

  protected HttpRequestInfo getRequest() {
    return request;
  }

  protected HttpResponseInfo getResponse() {
    return response;
  }

  protected HttpDownConfigInfo getDownConfig() {
    return downConfig;
  }

  protected ProxyConfig getProxyConfig() {
    return proxyConfig;
  }

  protected TaskInfo getTaskInfo() {
    return taskInfo;
  }

  protected NioEventLoopGroup getLoopGroup() {
    return loopGroup;
  }

  protected HttpDownCallback getCallback() {
    return callback;
  }

  public HttpDownBootstrap build() {
    try {
      if (request == null) {
        throw new BootstrapBuildException("request Can not be empty.");
      }
      if (response == null) {
        throw new BootstrapBuildException("response Can not be empty.");
      }
      if (loopGroup == null) {
        loopGroup = new NioEventLoopGroup(1);
      }
      if (downConfig == null) {
        downConfig = new HttpDownConfigInfo();
      }
      if (StringUtil.isNullOrEmpty(downConfig.getFilePath())) {
        downConfig.setFilePath(System.getProperty("user.dir"));
      }
      if (StringUtil.isNullOrEmpty(response.getFileName())) {
        response.setFileName("Unknown");
      }
      if (downConfig.getTimeout() <= 0) {
        downConfig.setTimeout(30);
      }
      if (downConfig.getRetryCount() <= 0) {
        downConfig.setRetryCount(5);
      }
      if (!response.isSupportRange() || downConfig.getConnections() <= 0) {
        downConfig.setConnections(16);
      }
      return new HttpDownBootstrap(request, response, downConfig, proxyConfig, taskInfo, loopGroup, callback);
    } catch (Exception e) {
      throw new BootstrapBuildException("build HttpDownBootstrap error", e);
    }
  }
}
