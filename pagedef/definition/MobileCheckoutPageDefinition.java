package com.nm.commerce.pagedef.definition;

public class MobileCheckoutPageDefinition extends PageDefinition {
  public static final String TYPE = "mobilecheckout";
  
  private String initialJSP;
  
  public void setInitialJSP(String initialJSP) {
    this.initialJSP = initialJSP;
  }
  
  public String getInitialJSP() {
    return getValue(initialJSP, basis, "getInitialJSP");
  }
  
}
