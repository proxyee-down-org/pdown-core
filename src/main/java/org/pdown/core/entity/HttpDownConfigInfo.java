package org.pdown.core.entity;

import java.io.Serializable;

public class HttpDownConfigInfo implements Serializable {

  private static final long serialVersionUID = -69564667309038578L;
  private String filePath;
  private int connections;
  private int timeout;
  private int retryCount;
  private boolean autoRename;
  private long speedLimit;

  public String getFilePath() {
    return filePath;
  }

  public HttpDownConfigInfo setFilePath(String filePath) {
    this.filePath = filePath;
    return this;
  }

  public int getConnections() {
    return connections;
  }

  public HttpDownConfigInfo setConnections(int connections) {
    this.connections = connections;
    return this;
  }

  public int getTimeout() {
    return timeout;
  }

  public HttpDownConfigInfo setTimeout(int timeout) {
    this.timeout = timeout;
    return this;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public HttpDownConfigInfo setRetryCount(int retryCount) {
    this.retryCount = retryCount;
    return this;
  }

  public boolean isAutoRename() {
    return autoRename;
  }

  public HttpDownConfigInfo setAutoRename(boolean autoRename) {
    this.autoRename = autoRename;
    return this;
  }

  public long getSpeedLimit() {
    return speedLimit;
  }

  public HttpDownConfigInfo setSpeedLimit(long speedLimit) {
    this.speedLimit = speedLimit;
    return this;
  }

  @Override
  public String toString() {
    return "HttpDownConfigInfo{" +
        "filePath='" + filePath + '\'' +
        ", connections=" + connections +
        ", timeout=" + timeout +
        ", retryCount=" + retryCount +
        ", autoRename=" + autoRename +
        ", speedLimit=" + speedLimit +
        '}';
  }
}
