package io.pdown.core.boot;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
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
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.ReferenceCountUtil;
import io.pdown.core.constant.HttpDownStatus;
import io.pdown.core.dispatch.HttpDownCallback;
import io.pdown.core.entity.ChunkInfo;
import io.pdown.core.entity.ConnectInfo;
import io.pdown.core.entity.HttpDownConfigInfo;
import io.pdown.core.entity.HttpRequestInfo;
import io.pdown.core.entity.TaskInfo;
import io.pdown.core.exception.BootstrapException;
import io.pdown.core.handle.DownTimeoutHandler;
import io.pdown.core.proxy.ProxyConfig;
import io.pdown.core.proxy.ProxyHandleFactory;
import io.pdown.core.util.FileUtil;
import io.pdown.core.util.HttpDownUtil;
import io.pdown.core.util.ProtoUtil.RequestProto;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpDownBootstrap implements Serializable {

  private static final long serialVersionUID = 7265797940598817922L;
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpDownBootstrap.class);

  private static final long SIZE_1MB = 1024 * 1024L;
  private static final double TIME_1S_NANO = (double) TimeUnit.SECONDS.toNanos(1);

  private HttpRequestInfo request;
  private HttpDownConfigInfo downConfig;
  private ProxyConfig proxyConfig;
  private TaskInfo taskInfo;

  protected transient NioEventLoopGroup loopGroup;
  protected transient HttpDownCallback callback;

  private transient ProgressThread progressThread;

  private HttpDownBootstrap() {
  }

  public HttpDownBootstrap(HttpRequestInfo request, HttpDownConfigInfo downConfig, ProxyConfig proxyConfig, TaskInfo taskInfo, NioEventLoopGroup loopGroup,
      HttpDownCallback callback) {
    this.request = request;
    this.downConfig = downConfig;
    this.proxyConfig = proxyConfig;
    this.taskInfo = taskInfo;
    this.loopGroup = loopGroup;
    this.callback = callback;
  }

  protected HttpRequestInfo getRequest() {
    return request;
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

  /**
   * 任务重新开始下载
   */
  public void startDown() {
    taskInfo = new TaskInfo();
    if (downConfig.getFilePath() == null || "".equals(downConfig.getFilePath().trim())) {
      throw new BootstrapException("下载路径不能为空");
    }
    if (!FileUtil.exists(downConfig.getFilePath())) {
      try {
        FileUtil.createDirSmart(downConfig.getFilePath());
      } catch (IOException e) {
        throw new BootstrapException("创建目录失败，请重试", e);
      }
    }
    if (!FileUtil.canWrite(downConfig.getFilePath())) {
      throw new BootstrapException("无权访问下载路径，请修改路径或开放目录写入权限");
    }
    //磁盘空间不足
    if (downConfig.getTotalSize() > FileUtil.getDiskFreeSize(downConfig.getFilePath())) {
      throw new BootstrapException("磁盘空间不足，请修改路径");
    }
    //有文件同名
    if (new File(HttpDownUtil.getTaskFilePath(downConfig)).exists()) {
      if (downConfig.isAutoRename()) {
        downConfig.setFileName(FileUtil.renameIfExists(HttpDownUtil.getTaskFilePath(downConfig)));
      } else {
        throw new BootstrapException("文件名已存在，请修改文件名");
      }
    }
    try {
      //创建文件
      if (downConfig.isSupportRange()) {
        FileUtil.createSparseFile(HttpDownUtil.getTaskFilePath(downConfig), downConfig.getTotalSize());
      } else {
        FileUtil.createFile(HttpDownUtil.getTaskFilePath(downConfig));
      }
    } catch (IOException e) {
      throw new BootstrapException("创建文件失败，请重试", e);
    }
    HttpDownUtil.buildChunkInfoList(downConfig, taskInfo);
    commonStart();
    taskInfo.setStartTime(System.currentTimeMillis());
    //文件下载开始回调
    if (callback != null) {
      request.headers().remove(HttpHeaderNames.RANGE);
      callback.onStart(request, downConfig);
    }
    for (int i = 0; i < taskInfo.getConnectInfoList().size(); i++) {
      ConnectInfo connectInfo = taskInfo.getConnectInfoList().get(i);
      connect(connectInfo);
    }
  }

  private void connect(ConnectInfo connectInfo) {
    RequestProto requestProto = request.requestProto();
    LOGGER.debug("开始下载：" + connectInfo);
    Bootstrap bootstrap = new Bootstrap()
        .channel(NioSocketChannel.class)
        .group(loopGroup)
        .handler(new HttpDownInitializer(requestProto.getSsl(), connectInfo));
    if (proxyConfig != null) {
      //代理服务器解析DNS和连接
      bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
    }
    ChannelFuture cf = bootstrap.connect(requestProto.getHost(), requestProto.getPort());
    //重置最后下载时间
    connectInfo.setConnectChannel(cf.channel());
    cf.addListener((ChannelFutureListener) future -> {
      if (future.isSuccess()) {
        LOGGER.debug("连接成功：" + connectInfo);
        if (downConfig.isSupportRange()) {
          request.headers().set(HttpHeaderNames.RANGE, "bytes=" + connectInfo.getStartPosition() + "-" + connectInfo.getEndPosition());
        } else {
          request.headers().remove(HttpHeaderNames.RANGE);
        }
        future.channel().writeAndFlush(request);
        if (request.content() != null) {
          //请求体写入
          HttpContent content = new DefaultLastHttpContent();
          content.content().writeBytes(request.content());
          future.channel().writeAndFlush(content);
        }
      } else {
        future.channel().close();
      }
    });
  }

  /**
   * 重新发起连接
   */
  private void reConnect(ConnectInfo connectInfo) {
    reConnect(connectInfo, false);
  }

  /**
   * 重新发起连接
   *
   * @param connectInfo 连接相关信息
   * @param isHelp 是否为帮助其他分段下载发起的连接
   */
  private void reConnect(ConnectInfo connectInfo, boolean isHelp) {
    if (!isHelp && downConfig.isSupportRange()) {
      connectInfo.setStartPosition(connectInfo.getStartPosition() + connectInfo.getDownSize());
    }
    connectInfo.setDownSize(0);
    if (connectInfo.getErrorCount() < downConfig.getRetryCount()) {
      connect(connectInfo);
    } else {
      if (callback != null) {
        callback.onChunkError(request, downConfig, taskInfo, taskInfo.getChunkInfoList().get(connectInfo.getChunkIndex()));
      }
      if (taskInfo.getConnectInfoList().stream()
          .filter(connect -> connect.getStatus() != HttpDownStatus.DONE)
          .allMatch(connect -> connect.getErrorCount() >= downConfig.getRetryCount())) {
        taskInfo.setStatus(HttpDownStatus.FAIL);
        taskInfo.getChunkInfoList().forEach(chunkInfo -> chunkInfo.setStatus(HttpDownStatus.FAIL));
        close();
        if (callback != null) {
          callback.onError(request, downConfig, null);
        }
      }
    }
  }

  /**
   * 暂停下载
   */
  public void pauseDown() throws Exception {
    synchronized (taskInfo) {
      if (taskInfo.getStatus() == HttpDownStatus.PAUSE
          || taskInfo.getStatus() == HttpDownStatus.DONE) {
        return;
      }
      taskInfo.setStatus(HttpDownStatus.PAUSE);
      long time = System.currentTimeMillis();
      for (ChunkInfo chunkInfo : taskInfo.getChunkInfoList()) {
        if (chunkInfo.getStatus() != HttpDownStatus.DONE) {
          chunkInfo.setStatus(HttpDownStatus.PAUSE);
          chunkInfo.setLastPauseTime(time);
        }
      }
      close();
    }
    if (callback != null) {
      callback.onPause(request, downConfig, taskInfo);
    }
  }

  /**
   * 继续下载
   */
  public void continueDown()
      throws Exception {
    if (taskInfo == null) {
      startDown();
      return;
    }
    synchronized (taskInfo) {
      if (taskInfo.getStatus() == HttpDownStatus.RUNNING
          || taskInfo.getStatus() == HttpDownStatus.DONE) {
        return;
      }
      if (!FileUtil.exists(HttpDownUtil.getTaskFilePath(downConfig))) {
        close();
        startDown();
      } else {
        commonStart();
        long time = System.currentTimeMillis();
        for (ChunkInfo chunkInfo : taskInfo.getChunkInfoList()) {
          if (chunkInfo.getStatus() == HttpDownStatus.PAUSE) {
            chunkInfo.setStatus(HttpDownStatus.RUNNING);
            chunkInfo.setPauseTime(chunkInfo.getPauseTime() + (time - chunkInfo.getLastPauseTime()));
          }
        }
        for (ConnectInfo connectInfo : taskInfo.getConnectInfoList()) {
          if (connectInfo.getStatus() == HttpDownStatus.RUNNING) {
            reConnect(connectInfo);
          }
        }
      }
    }
    if (callback != null) {
      callback.onContinue(request, downConfig, taskInfo);
    }
  }

  private void done() throws IOException {
    stopThreads();
    if (callback != null) {
      callback.onProgress(request, downConfig, taskInfo);
      callback.onDone(request, downConfig, taskInfo);
    }
  }

  public void close() {
    synchronized (taskInfo) {
      for (ConnectInfo connectInfo : taskInfo.getConnectInfoList()) {
        close(connectInfo);
      }
    }
    stopThreads();
  }

  private void close(ConnectInfo connectInfo) {
    try {
      HttpDownUtil.safeClose(connectInfo.getConnectChannel(), connectInfo.getFileChannel());
    } catch (Exception e) {
      LOGGER.error("closeChunk error", e);
    }
  }

  /*public void delete(boolean delFile) throws Exception {
    TaskInfo taskInfo = downConfig.getTaskInfo();
    //删除任务进度记录文件
    synchronized (taskInfo) {
      close();
      deleteRecord();
      if (delFile) {
        FileUtil.deleteIfExists(HttpDownUtil.getTaskFilePath(taskInfo));
      }
      if (callback != null) {
        callback.onDelete(downConfig);
      }
    }
  }

  private synchronized void saveRecord() {
    TaskInfo taskInfo = downConfig.getTaskInfo();
    try {
      ByteUtil.serialize(downConfig, HttpDownUtil.getTaskRecordFilePath(taskInfo), HttpDownUtil.getTaskRecordBakFilePath(taskInfo), true);
    } catch (IOException e) {
      LOGGER.error("saveRecord error", e);
    }
  }

  private void deleteRecord() {
    TaskInfo taskInfo = downConfig.getTaskInfo();
    try {
      FileUtil.deleteIfExists(HttpDownUtil.getTaskRecordFilePath(taskInfo));
      FileUtil.deleteIfExists(HttpDownUtil.getTaskRecordBakFilePath(taskInfo));
    } catch (IOException e) {
      LOGGER.error("deleteRecord error", e);
    }
  }*/

  private void stopThreads() {
    loopGroup.shutdownGracefully();
    loopGroup = null;
    progressThread.close();
    progressThread = null;
  }

  private void commonStart() {
    taskInfo.setStatus(HttpDownStatus.RUNNING);
    taskInfo.setLastStartTime(0);
    taskInfo.getChunkInfoList().forEach(chunkInfo -> {
      if (chunkInfo.getStatus() != HttpDownStatus.DONE) {
        chunkInfo.setStatus(HttpDownStatus.RUNNING);
      }
    });
    taskInfo.getConnectInfoList().forEach(connectInfo -> {
      connectInfo.setErrorCount(0);
      if (connectInfo.getStatus() != HttpDownStatus.DONE) {
        connectInfo.setStatus(HttpDownStatus.RUNNING);
      }
    });
    if (loopGroup == null) {
      loopGroup = new NioEventLoopGroup(1);
    }
    if (progressThread == null) {
      progressThread = new ProgressThread();
      progressThread.start();
    }
  }

  @Override
  public String toString() {
    return "HttpDownBootstrap{" +
        "request=" + request +
        ", downConfig=" + downConfig +
        ", proxyConfig=" + proxyConfig +
        ", taskInfo=" + taskInfo +
        ", callback=" + callback.getClass() +
        '}';
  }

  public static HttpDownBootstrapBuilder builder() {
    return new HttpDownBootstrapBuilder();
  }

  public static URLHttpDownBootstrapBuilder builder(String url, Map<String, String> heads, String body) {
    return new URLHttpDownBootstrapBuilder(url, heads, body);
  }

  public static URLHttpDownBootstrapBuilder builder(String url, Map<String, String> heads) {
    return builder(url, heads, null);
  }

  public static URLHttpDownBootstrapBuilder builder(String url, String body) {
    return builder(url, null, body);
  }

  public static URLHttpDownBootstrapBuilder builder(String url) {
    return builder(url, null, null);
  }

  class ProgressThread extends Thread {

    private volatile boolean run = true;
    private int period = 1;

    @Override
    public void run() {
      while (run) {
        if (taskInfo.getStatus() != HttpDownStatus.DONE) {
          for (ChunkInfo chunkInfo : taskInfo.getChunkInfoList()) {
            synchronized (chunkInfo) {
              if (chunkInfo.getStatus() != HttpDownStatus.DONE) {
                chunkInfo.setSpeed((chunkInfo.getDownSize() - chunkInfo.getLastCountSize()) / period);
                chunkInfo.setLastCountSize(chunkInfo.getDownSize());
              }
            }
          }
          //计算瞬时速度
          taskInfo.setSpeed(taskInfo.getChunkInfoList()
              .stream()
              .filter(chunkInfo -> chunkInfo.getStatus() != HttpDownStatus.DONE)
              .mapToLong(ChunkInfo::getSpeed)
              .sum());
          callback.onProgress(request, downConfig, taskInfo);
        }
        try {
          TimeUnit.SECONDS.sleep(period);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    public void close() {
      run = false;
    }

  }

  class HttpDownInitializer extends ChannelInitializer {

    private boolean isSsl;
    private ConnectInfo connectInfo;

    public HttpDownInitializer(boolean isSsl, ConnectInfo connectInfo) {
      this.isSsl = isSsl;
      this.connectInfo = connectInfo;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
      if (proxyConfig != null) {
        ch.pipeline().addLast(ProxyHandleFactory.build(proxyConfig));
      }
      if (isSsl) {
        RequestProto requestProto = ((HttpRequestInfo) request).requestProto();
        ch.pipeline().addLast(HttpDownUtil.getSslContext().newHandler(ch.alloc(), requestProto.getHost(), requestProto.getPort()));
      }
      ch.pipeline().addLast("downTimeout", new DownTimeoutHandler(downConfig.getTimeout()));
      ch.pipeline().addLast("httpCodec", new HttpClientCodec());
      ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {

        private ChunkInfo chunkInfo = taskInfo.getChunkInfoList().get(connectInfo.getChunkIndex());
        private SeekableByteChannel fileChannel;
        private boolean normalClose;
        private boolean isSuccess;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
          try {
            if (msg instanceof HttpContent) {
              synchronized (taskInfo) {
                if (!isSuccess || !fileChannel.isOpen() || !ctx.channel().isOpen()) {
                  return;
                }
                HttpContent httpContent = (HttpContent) msg;
                ByteBuf byteBuf = httpContent.content();
                int size = byteBuf.readableBytes();
                //判断是否超出下载字节范围
                if (downConfig.isSupportRange() && connectInfo.getDownSize() + size > connectInfo.getTotalSize()) {
                  size = (int) (connectInfo.getTotalSize() - connectInfo.getDownSize());
                }
                fileChannel.write(byteBuf.nioBuffer());
                synchronized (chunkInfo) {
                  chunkInfo.setDownSize(chunkInfo.getDownSize() + size);
                  connectInfo.setDownSize(connectInfo.getDownSize() + size);
                  taskInfo.setDownSize(taskInfo.getDownSize() + size);
                }
                if (downConfig.getSpeedLimit() > 0) {
                  long time = System.nanoTime();
                  if (taskInfo.getLastStartTime() <= 0) {
                    taskInfo.setLastStartTime(time);
                  } else {
                    //计算平均速度是否超过速度限制
                    long useTime = time - taskInfo.getLastStartTime();
                    double speed = taskInfo.getDownSize() / (double) useTime;
                    double limitSpeed = downConfig.getSpeedLimit() / TIME_1S_NANO;
                    long sleepTime = (long) ((speed - limitSpeed) * useTime);
                    if (sleepTime > 0) {
                      TimeUnit.NANOSECONDS.sleep(sleepTime);
                      //刷新其他连接的lastReadTime
                      for (ConnectInfo ci : taskInfo.getConnectInfoList()) {
                        if (ci.getConnectChannel() != null && ci.getConnectChannel() != ch) {
                          DownTimeoutHandler downTimeout = (DownTimeoutHandler) ci.getConnectChannel().pipeline().get("downTimeout");
                          if (downTimeout != null) {
                            downTimeout.updateLastReadTime(sleepTime);
                          }
                        }
                      }
                    }
                  }
                }
                if (!downConfig.isSupportRange() && !(httpContent instanceof LastHttpContent)) {
                  return;
                }
                long time = System.currentTimeMillis();
                if (connectInfo.getDownSize() >= connectInfo.getTotalSize()) {
                  LOGGER.debug("连接下载完成：" + connectInfo + "\t" + chunkInfo);
                  normalClose = true;
                  close(connectInfo);
                  //判断分段是否下载完成
                  if (isChunkDownDone(httpContent)) {
                    LOGGER.debug("分段下载完成：" + connectInfo + "\t" + chunkInfo);
                    synchronized (chunkInfo) {
                      chunkInfo.setStatus(HttpDownStatus.DONE);
                      //计算最后平均下载速度
                      chunkInfo.setSpeed(calcSpeed(chunkInfo.getTotalSize(), time - taskInfo.getStartTime() - chunkInfo.getPauseTime()));
                    }
                    //分段下载完成回调
                    if (callback != null) {
                      callback.onChunkDone(request, downConfig, taskInfo, chunkInfo);
                    }
                    //所有分段都下载完成
                    if (taskInfo.getChunkInfoList().stream().allMatch((chunk) -> chunk.getStatus() == HttpDownStatus.DONE)) {
                      if (!downConfig.isSupportRange()) {  //chunked编码最后更新文件大小
                        downConfig.setTotalSize(taskInfo.getDownSize());
                        taskInfo.getChunkInfoList().get(0).setTotalSize(taskInfo.getDownSize());
                      }
                      //文件下载完成回调
                      LOGGER.debug("任务下载完成：" + chunkInfo);
                      connectInfo.setStatus(HttpDownStatus.DONE);
                      taskInfo.setStatus(HttpDownStatus.DONE);
                      //计算平均速度
                      taskInfo.setSpeed(calcSpeed(downConfig.getTotalSize(), System.currentTimeMillis() - taskInfo.getStartTime()));
                      done();
                      return;
                    }
                  }
                  //判断是否要去支持其他分段,找一个下载最慢的分段
                  ChunkInfo supportChunk = taskInfo.getChunkInfoList()
                      .stream()
                      .filter(chunk -> chunk.getIndex() != connectInfo.getChunkIndex() && chunk.getStatus() != HttpDownStatus.DONE)
                      .min(Comparator.comparingLong(ChunkInfo::getDownSize))
                      .orElse(null);
                  if (supportChunk == null) {
                    connectInfo.setStatus(HttpDownStatus.DONE);
                    return;
                  }
                  ConnectInfo maxConnect = taskInfo.getConnectInfoList()
                      .stream()
                      .filter(connect -> connect.getChunkIndex() == supportChunk.getIndex() && connect.getTotalSize() - connect.getDownSize() >= SIZE_1MB)
                      .max((c1, c2) -> (int) (c1.getStartPosition() - c2.getStartPosition()))
                      .orElse(null);
                  if (maxConnect == null) {
                    connectInfo.setStatus(HttpDownStatus.DONE);
                    return;
                  }
                  //把这个分段最后一个下载连接分成两个
                  long remainingSize = maxConnect.getTotalSize() - maxConnect.getDownSize();
                  long endTemp = maxConnect.getEndPosition();
                  maxConnect.setEndPosition(maxConnect.getEndPosition() - (remainingSize / 2));
                  //给当前连接重新分配下载区间
                  connectInfo.setStartPosition(maxConnect.getEndPosition() + 1);
                  connectInfo.setEndPosition(endTemp);
                  connectInfo.setChunkIndex(supportChunk.getIndex());
                  LOGGER.debug("支持下载：" + connectInfo + "\t" + chunkInfo);
                  reConnect(connectInfo, true);
                }
              }
            } else {
              HttpResponse httpResponse = (HttpResponse) msg;
              Integer responseCode = httpResponse.status().code();
              if (responseCode < 200 || responseCode >= 300) {
                LOGGER.warn("响应状态码异常：" + responseCode + "\t" + connectInfo);
                if (responseCode >= 500) {
                  connectInfo.setErrorCount(connectInfo.getErrorCount() + 1);
                }
                normalClose = true;
                close(connectInfo);
                ch.eventLoop().schedule(() -> reConnect(connectInfo), 5, TimeUnit.SECONDS);
                return;
              }
              LOGGER.debug("下载响应：" + connectInfo);
              fileChannel = Files.newByteChannel(Paths.get(HttpDownUtil.getTaskFilePath(downConfig)), StandardOpenOption.WRITE);
              if (downConfig.isSupportRange()) {
                fileChannel.position(connectInfo.getStartPosition() + connectInfo.getDownSize());
              }
              connectInfo.setFileChannel(fileChannel);
              isSuccess = true;
            }
          } catch (Exception e) {
            throw e;
          } finally {
            ReferenceCountUtil.release(msg);
          }
        }

        private boolean isChunkDownDone(HttpContent httpContent) {
          if (downConfig.isSupportRange()) {
            if (chunkInfo.getDownSize() >= chunkInfo.getTotalSize()) {
              return true;
            }
          } else if (httpContent instanceof LastHttpContent) {
            return true;
          }
          return false;
        }

        /**
         * 计算下载速度(B/S)
         * @param downSize  下载的字节数
         * @param downTime  下载耗时
         * @return
         */
        private long calcSpeed(long downSize, long downTime) {
          return (long) (downSize / (downTime / 1000D));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
          if (!(cause instanceof IOException) && !(cause instanceof ReadTimeoutException)) {
            LOGGER.error("down onChunkError:", cause);
          }
          normalClose = true;
          close(connectInfo);
          reConnect(connectInfo);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
          super.channelUnregistered(ctx);
          //还未下载完成，继续下载
          if (!normalClose
              && chunkInfo.getStatus() != HttpDownStatus.PAUSE
              && chunkInfo.getStatus() != HttpDownStatus.FAIL) {
            LOGGER.debug("channelUnregistered:" + connectInfo + "\t" + chunkInfo);
            close(connectInfo);
            reConnect(connectInfo);
          }
        }
      });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      super.exceptionCaught(ctx, cause);
      LOGGER.error("down onInit:", cause);
    }
  }
}
