package io.pdown.core.entity;

import io.netty.channel.Channel;
import java.io.Serializable;
import java.nio.channels.SeekableByteChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

  public long getTotalSize() {
    return endPosition - startPosition + 1;
  }
}
