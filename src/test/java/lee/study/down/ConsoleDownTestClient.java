package lee.study.down;

import org.pdown.core.boot.HttpDownBootstrap;
import org.pdown.core.dispatch.ConsoleHttpDownCallback;
import org.pdown.core.entity.HttpDownConfigInfo;
import org.pdown.core.util.FileUtil;
import lee.study.down.server.RangeDownTestServer;

public class ConsoleDownTestClient {

  private static final String TEST_DIR = System.getProperty("user.dir")+"/target";
  private static final String TEST_FILE = TEST_DIR + "/" + "test.data";

  public static void main(String[] args) throws Exception {
    FileUtil.createDir(TEST_DIR);
    //正常下载
    new Thread(() -> {
      try {
        new RangeDownTestServer("f:/test/test.data").start(8866);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }).start();
    //生成下载文件
    DownClientTest.buildRandomFile(TEST_FILE, 1024 * 1024 * 100L);
    HttpDownBootstrap.builder("http://127.0.0.1:8866")
        .downConfig(new HttpDownConfigInfo().setFilePath(TEST_DIR)
            .setAutoRename(true)
            .setConnections(5)
            .setSpeedLimit(5 * 1024 * 1024))
        .callback(new ConsoleHttpDownCallback() {
          @Override
          public void onError(HttpDownBootstrap httpDownBootstrap) {
            super.onError(httpDownBootstrap);
            System.exit(0);
          }

          @Override
          public void onDone(HttpDownBootstrap httpDownBootstrap) {
            super.onDone(httpDownBootstrap);
            System.exit(0);
          }
        }).build().startDown();
  }
}
