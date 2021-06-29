package com.nm.commerce.pagedef.evaluator.template;

import javax.servlet.jsp.PageContext;

import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.nucleus.Nucleus;

import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;

public class PartnerPageEvaluator extends SimplePageEvaluator {
  
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    
    String url = null;
    try {
      Repository rep = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
      RepositoryItem item = rep.getItem(getRequest().getParameter("id"), "htmlfragments");
      if (item != null) {
        url = (String) item.getPropertyValue("frag_value");
        if (url.indexOf("?") > -1) {
          url = url + "&PROFILE_ID=" + getProfile().getRepositoryId();
        } else {
          url = url + "?PROFILE_ID=" + getProfile().getRepositoryId();
        }
        getRequest().setAttribute("partnerurl", url);
      }
    } catch (Exception re) {}
    
    return super.evaluate(pageDefinition, pageContext);
  }
  
}
