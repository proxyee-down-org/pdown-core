package io.pdown.core.dispatch;

import io.pdown.core.entity.HttpDownConfigInfo;
import io.pdown.core.entity.HttpRequestInfo;
import io.pdown.core.entity.TaskInfo;
import io.pdown.core.util.ByteUtil;

public class ConsoleHttpDownCallback extends HttpDownCallback {

  private int progressWidth = 20;

  public ConsoleHttpDownCallback() {
  }

  public ConsoleHttpDownCallback(int progressWidth) {
    this.progressWidth = progressWidth;
  }

  @Override
  public void onStart(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig) {
    System.out.println(requestInfo.toString());
    System.out.println();
  }

  @Override
  public void onProgress(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig, TaskInfo taskInfo) {
    double rate = 0;
    if (downConfig.getTotalSize() > 0) {
      rate = taskInfo.getDownSize() / (double) downConfig.getTotalSize();
    }
    printProgress(rate, taskInfo.getSpeed());
  }

  @Override
  public void onError(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig, TaskInfo taskInfo) {
    System.out.println("\n\nDownload error");
  }

  @Override
  public void onDone(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig, TaskInfo taskInfo) {
    System.out.println("\n\nDownload completed");
  }

  protected void printProgress(double rate, long speed) {
    int downWidth = (int) (progressWidth * rate);
    StringBuilder sb = new StringBuilder();
    sb.append("\r[");
    for (int i = 0; i < progressWidth; i++) {
      if (i < downWidth) {
        sb.append("■");
      } else {
        sb.append("□");
      }
    }
    sb.append("] " + String.format("%.2f", rate * 100) + "%    " + String.format("%8s", ByteUtil.byteFormat(speed)) + "/S");
    System.out.print(sb.toString());
  }
}
