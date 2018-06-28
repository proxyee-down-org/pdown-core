package org.pdown.core.entity;

import java.io.Serializable;

public class ChunkInfo implements Serializable {

  private static final long serialVersionUID = 231649750985696846L;
  private int index;
  private long downSize = 0;
  private long totalSize;
  private long lastCountSize = 0;
  private long lastPauseTime = 0;
  private long pauseTime = 0;
  private int status = 0;
  private long speed;

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public long getDownSize() {
    return downSize;
  }

  public void setDownSize(long downSize) {
    this.downSize = downSize;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(long totalSize) {
    this.totalSize = totalSize;
  }

  public long getLastCountSize() {
    return lastCountSize;
  }

  public void setLastCountSize(long lastCountSize) {
    this.lastCountSize = lastCountSize;
  }

  public long getLastPauseTime() {
    return lastPauseTime;
  }

  public void setLastPauseTime(long lastPauseTime) {
    this.lastPauseTime = lastPauseTime;
  }

  public long getPauseTime() {
    return pauseTime;
  }

  public void setPauseTime(long pauseTime) {
    this.pauseTime = pauseTime;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public long getSpeed() {
    return speed;
  }

  public void setSpeed(long speed) {
    this.speed = speed;
  }

  @Override
  public String toString() {
    return "ChunkInfo{" +
        "index=" + index +
        ", downSize=" + downSize +
        ", totalSize=" + totalSize +
        ", lastCountSize=" + lastCountSize +
        ", lastPauseTime=" + lastPauseTime +
        ", pauseTime=" + pauseTime +
        ", status=" + status +
        ", speed=" + speed +
        '}';
  }
}
