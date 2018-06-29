package org.pdown.core.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class TestUtil {

  public static void buildRandomFile(String path, long size) throws IOException {
    File file = new File(path);
    if (file.exists()) {
      file.delete();
    }
    file.createNewFile();
    try (
        FileOutputStream outputStream = new FileOutputStream(file)
    ) {
      byte[] bts = new byte[8192];
      for (int i = 0; i < bts.length; i++) {
        bts[i] = (byte) (Math.random() * 255);
      }
      for (long i = 0; i < size; i += bts.length) {
        outputStream.write(bts);
      }
    }
  }

  public static String getMd5ByFile(File file) {
    InputStream fis;
    byte[] buffer = new byte[2048];
    int numRead;
    MessageDigest md5;

    try {
      fis = new FileInputStream(file);
      md5 = MessageDigest.getInstance("MD5");
      while ((numRead = fis.read(buffer)) > 0) {
        md5.update(buffer, 0, numRead);
      }
      fis.close();
      return md5ToString(md5.digest());
    } catch (Exception e) {
      return null;
    }
  }

  private static String md5ToString(byte[] md5Bytes) {
    StringBuffer hexValue = new StringBuffer();
    for (int i = 0; i < md5Bytes.length; i++) {
      int val = ((int) md5Bytes[i]) & 0xff;
      if (val < 16) {
        hexValue.append("0");
      }
      hexValue.append(Integer.toHexString(val));
    }
    return hexValue.toString();
  }
}
