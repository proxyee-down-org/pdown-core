package lee.study.down;

import io.pdown.core.boot.HttpDownBootstrap;
import io.pdown.core.dispatch.HttpDownCallback;
import io.pdown.core.entity.HttpDownConfigInfo;
import io.pdown.core.entity.HttpRequestInfo;
import io.pdown.core.entity.TaskInfo;

public class BdyTestClient {

  public static void main(String[] args) throws Exception {
    HttpDownBootstrap bootstrap = HttpDownBootstrap.builder(
        "https://www.baidupcs.com/rest/2.0/pcs/file?method=batchdownload&app_id=250528&zipcontent=%7B%22fs_id%22%3A%5B%2296066052180415%22%2C%22458455020118303%22%2C%221007892194709021%22%5D%7D&sign=DCb740ccc5511e5e8fedcff06b081203:iPVTqH8VdH7wPZmMLQQpHK1YQJU%3D&uid=1444206073&time=1528531740&dp-logid=221308402506765327&dp-callid=0&from_uk=1444206073")
        .downConfig(new HttpDownConfigInfo().setConnections(1)
            .setFilePath("f:/test"))
        .callback(new HttpDownCallback() {

          @Override
          public void onProgress(HttpRequestInfo requestInfo,HttpDownConfigInfo downConfig, TaskInfo taskInfo) {
            System.out.println("speed:" + taskInfo.getSpeed());
          }

          @Override
          public void onDone(HttpRequestInfo requestInfo,HttpDownConfigInfo downConfig, TaskInfo taskInfo) {
            System.out.println("final speed:" + taskInfo.getSpeed());
          }

        })
        .build();
    bootstrap.startDown();
  }
}
