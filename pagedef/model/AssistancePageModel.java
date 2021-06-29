package com.nm.commerce.pagedef.model;

import com.nm.catalog.navigation.NMCategory;

public class AssistancePageModel extends PageModel {
  private NMCategory assistanceCategory;
  private NMCategory currentAssistanceCategory;
  
  /** boolean to hold charge for returns active. */
  private boolean chargeForReturnsActive;
  
  public AssistancePageModel() {}
  
  public void setAssistanceCategory(final NMCategory assistanceCategory) {
    this.assistanceCategory = assistanceCategory;
  }
  
  public NMCategory getAssistanceCategory() {
    return assistanceCategory;
  }
  
  public void setCurrentAssistanceCategory(final NMCategory currentAssistanceCategory) {
    this.currentAssistanceCategory = currentAssistanceCategory;
  }
  
  public NMCategory getCurrentAssistanceCategory() {
    return currentAssistanceCategory;
  }
  
  /**
   * Checks if is charge for returns active.
   * 
   * @return true, if is charge for returns active
   */
  public boolean isChargeForReturnsActive() {
    return chargeForReturnsActive;
  }
  
  /**
   * Sets the charge for returns active.
   * 
   * @param chargeForReturnsActive
   *          the new charge for returns active
   */
  public void setChargeForReturnsActive(final boolean chargeForReturnsActive) {
    this.chargeForReturnsActive = chargeForReturnsActive;
  }
  
}
