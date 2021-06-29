// Created by vsrb1 6/17/2003
// Source File Name: NMCookieManager.java

package com.nm.commerce;

import atg.userprofiling.CookieManager;
import atg.userprofiling.Profile;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import java.lang.NullPointerException;
import javax.servlet.http.Cookie;
import atg.userprofiling.ProfileRequestServlet;

public class NMCookieManager extends CookieManager {
  
  // -------------------------------------
  public NMCookieManager() {
    super();
  } // end no-argument constructor
  
  // -------------------------------------
  /**
   * Override super method by testing the boolean profileRequestServlet.isSendCookie(profile) before calling the super method. Catch NullPointerException and record in log.
   */
  public void forceProfileCookies(Profile profile, DynamoHttpServletRequest request, DynamoHttpServletResponse response) throws ServletException, IOException {
    
    try {
      ProfileRequestServlet profileRequestServlet = (ProfileRequestServlet) request.resolveName("/atg/userprofiling/ProfileRequestServlet");
      
      if (profileRequestServlet.isSendCookie(profile)) {
        super.forceProfileCookies(profile, request, response);
      } // end if
    } catch (NullPointerException npe) {
      npe.printStackTrace();
    } finally {
      return;
    } // end try-catch
  } // end forceProfileCookies() method
  // -------------------------------------
  
} // end class
