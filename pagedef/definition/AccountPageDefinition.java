package com.nm.commerce.pagedef.definition;

/**
 * Basic definition for a template page. Configuration for main content area, layout, nav, etc.
 */
public class AccountPageDefinition extends PageDefinition {
  private String siloId;
  
  public void setSiloId(String value) {
    siloId = value;
  }
  
  public String getSiloId() {
    return getValue(siloId, basis, "getSiloId");
  }
}
