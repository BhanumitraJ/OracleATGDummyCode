package com.nm.commerce.pagedef.model.omniture;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.nm.utils.StringUtilities;

/**
 * Map class for storing omniture properties. String properties are wrapped in double quotes before being added to the map. The wrapping in quotes is intended to help the process that generates the
 * inline javascript so that it doesn't have to worry about what does and doesn't need to be enclosed in quotes (strings vs numbers).
 * 
 * @author nmwps
 * 
 */
public class OmniturePropertyMap extends HashMap<String, Object> {
  
  /**
   * Gets object from map for given key. Remove wrapped double quotes from strings.
   */
  public Object getValue(String key) {
    return removeOuterQuotes(super.get(key));
  }
  
  /**
   * Adds object to map for given key. String values will be wrapped in double quotes before being added to the map.
   */
  public Object put(String key, Object value) {
    return super.put(key, scrub(value));
  }
  
  /**
   * Copies keys and value from existing map to this map. String values will be wrapped in double quotes before being added to the map. *
   */
  public void putAll(Map map) {
    Set<String> keys = map.keySet();
    for (String key : keys) {
      Object value = map.get(key);
      if (value instanceof String && !((String) value).startsWith("\"") && !((String) value).endsWith("\"")) {
        value = "\"" + value + "\"";
      }
      super.put(key, value);
    }
  }
  
  public void append(String key, Object value, String delim) {
    Object objValue = this.get(key);
    if (objValue instanceof String && StringUtilities.isNotEmpty((String) objValue)) {
      objValue = removeOuterQuotes(objValue) + delim + (String) value;
    }
    put(key, objValue);
  }
  
  /*
   * Added a simple null check , found few scenarios if the var's in the map are set to null then Omniture is not getting fired Assumed that we dont have to send null values to Omniture
   */
  private Object scrub(Object value) {
    if (value == null) {
      value = "";
    }
    if (value instanceof String && !((String) value).startsWith("\"") && !((String) value).endsWith("\"")) {
      value = "\"" + value + "\"";
    } else if (value instanceof String && ((String) value).endsWith("\\\"")) {
      value = "\"" + value + "\"";
    }
    return value;
  }
  
  private Object removeOuterQuotes(Object value) {
    if (value == null) {
      value = "";
    }
    if (value instanceof String && ((String) value).startsWith("\"") && ((String) value).endsWith("\"")) {
      int valueLength = ((String) value).length();
      if (valueLength > 2) {
        value = ((String) value).substring(1, valueLength - 1);
      } else {
        value = "";
      }
    }
    return value;
  }
}
