package io.pdown.core.entity;

import io.netty.channel.Channel;
import java.io.Serializable;
import java.nio.channels.SeekableByteChannel;

public class ConnectInfo implements Serializable {

  private static final long serialVersionUID = 231649750985691346L;
  private long startPosition;
  private long endPosition;
  private long downSize;
  private int errorCount;
  private int chunkIndex;
  private int status;


  private transient Channel connectChannel;
  private transient SeekableByteChannel fileChannel;

  public long getStartPosition() {
    return startPosition;
  }

  public void setStartPosition(long startPosition) {
    this.startPosition = startPosition;
  }

  public long getEndPosition() {
    return endPosition;
  }

  public void setEndPosition(long endPosition) {
    this.endPosition = endPosition;
  }

  public long getDownSize() {
    return downSize;
  }

  public void setDownSize(long downSize) {
    this.downSize = downSize;
  }

  public int getErrorCount() {
    return errorCount;
  }

  public void setErrorCount(int errorCount) {
    this.errorCount = errorCount;
  }

  public int getChunkIndex() {
    return chunkIndex;
  }

  public void setChunkIndex(int chunkIndex) {
    this.chunkIndex = chunkIndex;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public Channel getConnectChannel() {
    return connectChannel;
  }

  public void setConnectChannel(Channel connectChannel) {
    this.connectChannel = connectChannel;
  }

  public SeekableByteChannel getFileChannel() {
    return fileChannel;
  }

  public void setFileChannel(SeekableByteChannel fileChannel) {
    this.fileChannel = fileChannel;
  }

  public long getTotalSize() {
    return endPosition - startPosition + 1;
  }

  @Override
  public String toString() {
    return "ConnectInfo{" +
        "startPosition=" + startPosition +
        ", endPosition=" + endPosition +
        ", downSize=" + downSize +
        ", errorCount=" + errorCount +
        ", chunkIndex=" + chunkIndex +
        ", status=" + status +
        ", connectChannel=" + connectChannel +
        ", fileChannel=" + fileChannel +
        '}';
  }
}
