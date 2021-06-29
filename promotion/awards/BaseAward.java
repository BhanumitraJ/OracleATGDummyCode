package com.nm.commerce.promotion.awards;

public abstract class BaseAward implements Award {
  
  protected String id;
  protected String name;
  protected String value;
  protected String valuePrefix;
  protected String displayValue;
  protected String title;
  protected int type;
  
  public int getType() {
    return type;
  }
  
  public void setType(int type) {
    this.type = type;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getValue() {
    return value;
  }
  
  public void setValue(String value) {
    this.value = value;
  }
  
  public String getValuePrefix() {
    return valuePrefix;
  }
  
  public void setValuePrefix(String valuePrefix) {
    this.valuePrefix = valuePrefix;
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  // this is the string representation of the award as a whole (name and value details)
  public abstract String getDisplayValue();
  
  public void setDisplayValue(String displayValue) {
    this.displayValue = displayValue;
  }
  
  public String getTitle() {
    return title;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
}
