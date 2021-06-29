package com.nm.commerce.promotion;

public class PromoException extends Exception {
  public PromoException() {
    super();
  }
  
  public PromoException(final String msg) {
    super(msg);
  }
  
  public PromoException(final Throwable t) {
    super(t);
  }
  
  public PromoException(final String msg, final Throwable t) {
    super(msg, t);
  }
}
