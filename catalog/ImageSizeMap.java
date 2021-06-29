package com.nm.commerce.catalog;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps image keys to corresponding sizes.
 */
public class ImageSizeMap {
  
  private static final Map SIZE_MAP = new HashMap();
  
  static {
    SIZE_MAP.put("g", new int[] {75 , 94});
    SIZE_MAP.put("y", new int[] {100 , 125});
    SIZE_MAP.put("h", new int[] {138 , 173});
    SIZE_MAP.put("t", new int[] {173 , 216});
    SIZE_MAP.put("n", new int[] {216 , 270});
    SIZE_MAP.put("j", new int[] {230 , 288});
    SIZE_MAP.put("f", new int[] {274 , 343});
    SIZE_MAP.put("d", new int[] {309 , 387});
    SIZE_MAP.put("x", new int[] {336 , 420});
    SIZE_MAP.put("p", new int[] {451 , 564});
    SIZE_MAP.put("z", new int[] {1200 , 1500});
  }
  
  public static int getWidth(String key) {
    int[] size = getSize(key);
    return (size == null) ? 0 : size[0];
  }
  
  public static int getHeight(String key) {
    int[] size = getSize(key);
    return (size == null) ? 0 : size[1];
  }
  
  private static int[] getSize(String key) {
    if (key.length() > 1) {
      String sizeKey = key.substring(1).toLowerCase();
      return (int[]) SIZE_MAP.get(sizeKey);
    }
    return null;
  }
}
