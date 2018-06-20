package io.pdown.core.entity;

import java.io.Serializable;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
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
}
