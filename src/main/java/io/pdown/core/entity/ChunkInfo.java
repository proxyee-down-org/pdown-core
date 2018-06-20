package io.pdown.core.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
}
