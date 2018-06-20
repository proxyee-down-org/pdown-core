package io.pdown.core.entity;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TaskInfo implements Serializable {

  private static final long serialVersionUID = 4813413517396555930L;
  private long downSize;
  private long startTime = 0;
  private long lastStartTime = 0;
  private int status;
  private long speed;
  private List<ChunkInfo> chunkInfoList;
  private List<ConnectInfo> connectInfoList;

  public long getDownSize() {
    return chunkInfoList.stream()
        .mapToLong(ChunkInfo::getDownSize)
        .sum();
  }
}
