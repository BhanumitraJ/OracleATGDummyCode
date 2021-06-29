package com.nm.commerce.pagedef.evaluator.service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.droplet.LinkshareCookie;

import com.nm.utils.fiftyone.FiftyOneUtils;

/**
 * Updates variables and sets up any redirects before page execution. Returns true if page content should be output.
 */
public class LinksharePageEvaluator extends SimplePageEvaluator {
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    String redirectString = "/index.jsp";
    DynamoHttpServletRequest request = getRequest();
    DynamoHttpServletResponse response = getResponse();
    LinkshareCookie linkshareCookie = (LinkshareCookie) request.resolveName("/nm/droplet/LinkShareCookie");
    
    FiftyOneUtils fiftyOneUtils = CheckoutComponents.getFiftyOneUtils();
    if (linkshareCookie.getEnableLSTracking()) {
      // get MID and siteId and set in cookie
      String mid = null;
      if (!StringUtils.isBlank(request.getParameter("mid"))) {
        mid = request.getParameter("mid");
      } else {
        mid = fiftyOneUtils.getAffiliateMerchantId();
      }
      String siteID = request.getParameter("siteID");
      
      String muiltipleCookieValues = "";
      if (mid != null & siteID != null) {
        muiltipleCookieValues = mid + "&" + siteID;
      } else if (mid != null) {
        muiltipleCookieValues = mid;
      } else {
        muiltipleCookieValues = siteID;
      }
      
      if (!StringUtils.isBlank(muiltipleCookieValues)) {
        Cookie cookie = linkshareCookie.processCookieParam("setCookie", muiltipleCookieValues, request, response);
        if (cookie != null) {
          String url = request.getParameter("url");
          if (url != null && url.length() > 0) {
            if (siteID.length() > 11) {
              String truncatedString = siteID.substring(0, 11).concat("...");
              redirectString = url.replaceFirst("\\[INSERT_11_DIGIT_SITEID_HERE\\]", truncatedString);
            } else {
              redirectString = url.replaceFirst("\\[INSERT_11_DIGIT_SITEID_HERE\\]", siteID);
            }
          }
        }
      }
      
    }
    response.sendRedirect(redirectString);
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    return false;
  }
}
