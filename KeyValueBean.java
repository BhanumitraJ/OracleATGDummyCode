package com.nm.commerce;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

public class KeyValueBean implements JSONAware {
  
  private Object key;
  private Object value;
  
  public KeyValueBean(Object key, Object value) {
    this.key = key;
    this.value = value;
  }
  
  public Object getKey() {
    return key;
  }
  
  public void setKey(Object key) {
    this.key = key;
  }
  
  public Object getValue() {
    return value;
  }
  
  public void setValue(Object value) {
    this.value = value;
  }
  
  @Override
  public String toString() {
    return key + " + " + value;
  }
  
  @Override
  public String toJSONString() {
    StringBuffer sb = new StringBuffer();
    
    sb.append("{");
    
    sb.append(JSONObject.escape((String) key));
    sb.append(":");
    sb.append(JSONObject.escape((String) value));
    
    sb.append("}");
    
    return sb.toString();
  }
}
