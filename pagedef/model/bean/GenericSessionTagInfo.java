package com.nm.commerce.pagedef.model.bean;

import atg.servlet.DynamoHttpServletRequest;

/*
 * This object is used for tracking marketing tag information and will be attached to the request as a session attribute.
 */
public class GenericSessionTagInfo {
  private boolean omnitureLoginSent = false;
  
  private static final String GENERIC_SESSION_TAG_INFO = "GenericSessionTagInfo";
  
  public GenericSessionTagInfo() {}
  
  public static GenericSessionTagInfo getInstance(DynamoHttpServletRequest dRequest) {
    GenericSessionTagInfo tagInfo = (GenericSessionTagInfo) dRequest.getSession().getAttribute(GENERIC_SESSION_TAG_INFO);
    if (tagInfo == null) {
      tagInfo = new GenericSessionTagInfo();
      dRequest.getSession().setAttribute(GENERIC_SESSION_TAG_INFO, tagInfo);
    }
    return tagInfo;
  }
  
  public boolean isOmnitureLoginSent() {
    return omnitureLoginSent;
  }
  
  public void setOmnitureLoginSent(boolean omnitureLoginSent) {
    this.omnitureLoginSent = omnitureLoginSent;
  }
  
}
