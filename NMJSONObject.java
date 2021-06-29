package com.nm.commerce;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.ItemList;

public class NMJSONObject extends LinkedHashMap {
  
  private static final long serialVersionUID = 8881249060554751494L;
  
  public String toString() {
    ItemList list = new ItemList();
    Iterator iter = entrySet().iterator();
    
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      list.add(toString(entry.getKey().toString(), entry.getValue()));
    }
    return "{" + list.toString() + "}";
  }
  
  public static String toString(String key, Object value) {
    StringBuffer sb = new StringBuffer();
    
    sb.append("\"");
    sb.append(escape(key));
    sb.append("\":");
    if (value == null) {
      sb.append("null");
      return sb.toString();
    }
    
    if (value instanceof String) {
      sb.append("\"");
      sb.append(escape((String) value));
      sb.append("\"");
    } else
      sb.append(value);
    return sb.toString();
  }
  
  /**
   * " => \" , \ => \\
   * 
   * @param s
   * @return
   */
  public static String escape(String s) {
    if (s == null) return null;
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      switch (ch) {
        case '"':
          sb.append("\\\"");
        break;
        case '\\':
          sb.append("\\\\");
        break;
        case '\b':
          sb.append("\\b");
        break;
        case '\f':
          sb.append("\\f");
        break;
        case '\n':
          sb.append("\\n");
        break;
        case '\r':
          sb.append("\\r");
        break;
        case '\t':
          sb.append("\\t");
        break;
        case '/':
          sb.append("\\/");
        break;
        default:
          if (ch >= '\u0000' && ch <= '\u001F') {
            String ss = Integer.toHexString(ch);
            sb.append("\\u");
            for (int k = 0; k < 4 - ss.length(); k++) {
              sb.append('0');
            }
            sb.append(ss.toUpperCase());
          } else {
            sb.append(ch);
          }
      }
    }// for
    return sb.toString();
  }
}
