package org.pdown.core.entity;

import java.io.Serializable;

public class HttpResponseInfo implements Serializable {

  private static final long serialVersionUID = 4828023107331722582L;
  private String fileName;
  private long totalSize;
  /**
   * support HTTP 206
   */
  private boolean supportRange;

  public HttpResponseInfo() {
  }

  public HttpResponseInfo(String fileName, long totalSize, boolean supportRange) {
    this.fileName = fileName;
    this.totalSize = totalSize;
    this.supportRange = supportRange;
  }

  public HttpResponseInfo(String fileName) {
    this.fileName = fileName;
  }

  public HttpResponseInfo(long totalSize, boolean supportRange) {
    this.totalSize = totalSize;
    this.supportRange = supportRange;
  }

  public String getFileName() {
    return fileName;
  }

  public HttpResponseInfo setFileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public HttpResponseInfo setTotalSize(long totalSize) {
    this.totalSize = totalSize;
    return this;
  }

  public boolean isSupportRange() {
    return supportRange;
  }

  public HttpResponseInfo setSupportRange(boolean supportRange) {
    this.supportRange = supportRange;
    return this;
  }

  @Override
  public String toString() {
    return "HttpResponseInfo{" +
        "fileName='" + fileName + '\'' +
        ", totalSize=" + totalSize +
        ", supportRange=" + supportRange +
        '}';
  }
}
