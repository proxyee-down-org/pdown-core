package io.pdown.core.dispatch;

import io.pdown.core.entity.ChunkInfo;
import io.pdown.core.entity.HttpDownConfigInfo;
import io.pdown.core.entity.HttpRequestInfo;
import io.pdown.core.entity.TaskInfo;

public class HttpDownCallback {

  public void onStart(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig) {
  }

  public void onProgress(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig, TaskInfo taskInfo) {
  }

  public void onPause(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig, TaskInfo taskInfo) {
  }

  public void onContinue(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig, TaskInfo taskInfo) {
  }

  public void onChunkError(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig, TaskInfo taskInfo, ChunkInfo chunkInfo) {
  }

  public void onError(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig, TaskInfo taskInfo) {
  }

  public void onChunkDone(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig, TaskInfo taskInfo, ChunkInfo chunkInfo) {
  }

  public void onDone(HttpRequestInfo requestInfo, HttpDownConfigInfo downConfig, TaskInfo taskInfo) {
  }
}
