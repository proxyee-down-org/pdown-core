package io.pdown.core.entity;

import java.io.Serializable;

public class HttpDownConfigInfo implements Serializable {

  private static final long serialVersionUID = -6077223876890481907L;
  private String id;
  private String filePath;
  private String fileName;
  private long totalSize;
  private boolean supportRange;
  private int connections;
  private int timeout;
  private int retryCount;
  private boolean autoRename;
  private long speedLimit;

  public String getId() {
    return id;
  }

  public HttpDownConfigInfo setId(String id) {
    this.id = id;
    return this;
  }

  public String getFilePath() {
    return filePath;
  }

  public HttpDownConfigInfo setFilePath(String filePath) {
    this.filePath = filePath;
    return this;
  }

  public String getFileName() {
    return fileName;
  }

  public HttpDownConfigInfo setFileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public HttpDownConfigInfo setTotalSize(long totalSize) {
    this.totalSize = totalSize;
    return this;
  }

  public boolean isSupportRange() {
    return supportRange;
  }

  public HttpDownConfigInfo setSupportRange(boolean supportRange) {
    this.supportRange = supportRange;
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
        "id='" + id + '\'' +
        ", filePath='" + filePath + '\'' +
        ", fileName='" + fileName + '\'' +
        ", totalSize=" + totalSize +
        ", supportRange=" + supportRange +
        ", connections=" + connections +
        ", timeout=" + timeout +
        ", retryCount=" + retryCount +
        ", autoRename=" + autoRename +
        ", speedLimit=" + speedLimit +
        '}';
  }
}
