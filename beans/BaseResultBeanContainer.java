package com.nm.commerce.beans;

public class BaseResultBeanContainer {
  
  private BaseResultBean baseResultBean;
  
  public BaseResultBean getResultBean() {
    return baseResultBean;
  }
  
  public void setResultBean(BaseResultBean baseResultBean) {
    this.baseResultBean = baseResultBean;
  }
  
  public static final String CONFIG_PATH = "/nm/commerce/beans/BaseResultBeanContainer";
  
}
