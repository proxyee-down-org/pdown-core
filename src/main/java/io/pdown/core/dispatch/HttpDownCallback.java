package io.pdown.core.dispatch;

import io.pdown.core.boot.HttpDownBootstrap;
import io.pdown.core.entity.ChunkInfo;

public class HttpDownCallback {

  public void onStart(HttpDownBootstrap httpDownBootstrap) {
  }

  public void onProgress(HttpDownBootstrap httpDownBootstrap) {
  }

  public void onPause(HttpDownBootstrap httpDownBootstrap) {
  }

  public void onContinue(HttpDownBootstrap httpDownBootstrap) {
  }

  public void onChunkError(HttpDownBootstrap httpDownBootstrap, ChunkInfo chunkInfo) {
  }

  public void onError(HttpDownBootstrap httpDownBootstrap) {
  }

  public void onChunkDone(HttpDownBootstrap httpDownBootstrap, ChunkInfo chunkInfo) {
  }

  public void onDone(HttpDownBootstrap httpDownBootstrap) {
  }
}
