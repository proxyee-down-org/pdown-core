package lee.study.down.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ReferenceCountUtil;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ChunkedDownTestServer {

  private int count;

  public ChunkedDownTestServer(String downFilePath) {
    this.downFilePath = downFilePath;
  }

  private String downFilePath;

  public void start(int port) throws InterruptedException {
    File file = new File(downFilePath);
    CountDownLatch countDownLatch = new CountDownLatch(1);
    new Thread(() -> {
      ServerBootstrap bootstrap = new ServerBootstrap();
      NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
      NioEventLoopGroup workerGroup = new NioEventLoopGroup(32);
      try {
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<Channel>() {

              @Override
              protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast("httpCodec", new HttpServerCodec());
                ch.pipeline().addLast("chunk", new ChunkedWriteHandler());
                ch.pipeline().addLast("serverHandle", new ChannelInboundHandlerAdapter() {

                  @Override
                  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    try {
                      if (msg instanceof LastHttpContent) {
                        try (
                            FileChannel fileChannel = new RandomAccessFile(file, "rw").getChannel()
                        ) {
                          HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                          httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_OCTET_STREAM);
                          httpResponse.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
                          httpResponse.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
                          ctx.channel().writeAndFlush(httpResponse);
                          ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
                          while (fileChannel.read(buffer) != -1) {
                            buffer.flip();
                            ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(8192);
                            byteBuf.writeBytes(buffer);
                            HttpContent httpContent = new DefaultHttpContent(byteBuf);
                            ctx.channel().writeAndFlush(httpContent);
                            buffer.clear();
                            if (count < 2) {
                              count++;
                              TimeUnit.SECONDS.sleep(10);
                            }
                          }
                          ctx.channel().writeAndFlush(new DefaultLastHttpContent());
                        }
                      }
                    } catch (Exception e) {
                      if (!(e instanceof IOException)) {
                        e.printStackTrace();
                      }
                      System.out.println("test server channel close");
                    } finally {
                      ReferenceCountUtil.release(msg);
                    }
                  }

                  @Override
                  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
                    ctx.channel().close();
                  }
                });
              }
            });
        ChannelFuture f = bootstrap
            .bind(port)
            .sync();
        f.addListener(future -> countDownLatch.countDown());
        f.channel().closeFuture().sync();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
      }
    }).start();
    countDownLatch.await();
  }
}
