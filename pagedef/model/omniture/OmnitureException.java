package com.nm.commerce.pagedef.model.omniture;

public class OmnitureException extends Exception {
  private static final long serialVersionUID = 1L;
  
  public OmnitureException() {
    super();
  }
  
  public OmnitureException(final String msg) { 
    super(msg);
  }
  
  public OmnitureException(final Throwable e) {
    super(e);
  }
  
  public OmnitureException(final String msg, final Throwable e) {
    super(msg, e);
  }
}