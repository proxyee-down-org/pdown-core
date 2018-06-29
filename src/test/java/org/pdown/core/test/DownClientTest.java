package org.pdown.core.test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pdown.core.boot.HttpDownBootstrap;
import org.pdown.core.dispatch.ConsoleHttpDownCallback;
import org.pdown.core.entity.HttpDownConfigInfo;
import org.pdown.core.test.server.ChunkedDownTestServer;
import org.pdown.core.test.server.RangeDownTestServer;
import org.pdown.core.test.util.TestUtil;
import org.pdown.core.util.FileUtil;
import org.pdown.core.util.HttpDownUtil;

public class DownClientTest {

  private static final String TEST_DIR = System.getProperty("user.dir") + "/target/test";
  private static final String TEST_BUILD_FILE = TEST_DIR + "/build.data";

  @Before
  public void httpServerStart() throws InterruptedException, IOException {
    FileUtil.createDirSmart(TEST_DIR);
    //生成下载文件
    TestUtil.buildRandomFile(TEST_BUILD_FILE, 1024 * 1024 * 500L);
    //正常下载
    new RangeDownTestServer(TEST_BUILD_FILE).start(8866);
    //超时下载
    new RangeDownTestServer(TEST_BUILD_FILE) {

      private AtomicInteger count = new AtomicInteger(0);

      @Override
      protected void writeHandle(Map<String, Object> attr) throws InterruptedException {
        if (count.getAndAdd(1) < 2) {
          String key = "flag";
          Boolean flag = (Boolean) attr.get(key);
          if (flag == null) {
            attr.put(key, true);
            TimeUnit.SECONDS.sleep(10);
          }
        }
      }
    }.start(8867);
    //chunk下载
    new ChunkedDownTestServer(TEST_BUILD_FILE).start(8868);
  }

  @Test
  public void down() throws Exception {
    //正常下载
    downTest(8866, 1);
    downTest(8866, 4);
    downTest(8866, 32);
    downTest(8866, 64);
    //超时下载
    downTest(8867, 1);
    downTest(8867, 4);
    downTest(8867, 32);
    downTest(8867, 64);
    //chunked编码下载
    downTest(8868, 2);
    //限速5MB/S
    downTest(8866, 4, 1024 * 1024 * 5L, 0);
    //暂停5S继续
    downTest(8866, 4, 0, 5000L);
  }

  @After
  public void deleteTestFile() throws IOException {
    FileUtil.deleteIfExists(TEST_DIR);
  }

  public static void downTest(int port, int connections, long speedLimit, long pauseTime) throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    AtomicBoolean succ = new AtomicBoolean(false);
    HttpDownBootstrap httpDownBootstrap = HttpDownBootstrap.builder("http://127.0.0.1:" + port)
        .downConfig(new HttpDownConfigInfo().setFilePath(TEST_DIR)
            .setAutoRename(true)
            .setConnections(connections)
            .setTimeout(5)
            .setSpeedLimit(speedLimit))
        .callback(new ConsoleHttpDownCallback() {

          @Override
          public void onDone(HttpDownBootstrap httpDownBootstrap) {
            System.out.println("");
            String sourceMd5 = TestUtil.getMd5ByFile(new File(TEST_BUILD_FILE));
            String downMd5 = TestUtil.getMd5ByFile(new File(HttpDownUtil.getTaskFilePath(httpDownBootstrap)));
            if (sourceMd5.equals(downMd5)) {
              succ.set(true);
            }
            try {
              FileUtil.deleteIfExists(HttpDownUtil.getTaskFilePath(httpDownBootstrap));
            } catch (IOException e) {
              e.printStackTrace();
            }
            countDownLatch.countDown();
          }

          @Override
          public void onError(HttpDownBootstrap httpDownBootstrap) {
            countDownLatch.countDown();
          }

          @Override
          public void onPause(HttpDownBootstrap httpDownBootstrap) {
            System.out.println("\nonPause");
          }

          @Override
          public void onResume(HttpDownBootstrap httpDownBootstrap) {
            System.out.println("\nonResume");
          }
        })
        .build();
    httpDownBootstrap.start();
    if (pauseTime > 0) {
      Thread.sleep(233L);
      httpDownBootstrap.pause();
      Thread.sleep(pauseTime);
      httpDownBootstrap.resume();
    }
    countDownLatch.await();
    assert succ.get();
  }

  private void downTest(int port, int connections) throws Exception {
    downTest(port, connections, 0, 0);
  }
}
