package com.nm.commerce.pagedef.evaluator.template;

import javax.servlet.jsp.PageContext;

import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.TemplatePageEvaluator;
import com.nm.commerce.pagedef.model.TemplatePageModel;
import com.nm.utils.NMFormHelper;
import com.nm.utils.SeoUrlUtil;

/**
 * Updates variables and sets up any redirects before page execution. Returns false because it always does redirect of some sort instead of outputting regular template page content.
 */
public class RedirectPageEvaluator extends TemplatePageEvaluator {
  
  // ICID constant - to check icid parameter in request URL
  private static final String ICID = "icid";
  
  // constants
  private static final String EQUALS = "=";
  private static final String QUESTION_MARK = "?";
  
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    TemplatePageModel pageModel = (TemplatePageModel) getPageModel();
    String redirectSource = getRedirectSource(pageModel.getCategory());
    
    if (redirectSource.indexOf("category.jsp") != -1 && SeoUrlUtil.isCategorySeoEnabled()) {
      
      String semanticUrl = SeoUrlUtil.buildSemanticPath(redirectSource);
      if (!isEmpty(semanticUrl)) {
        redirectSource = semanticUrl;
      }
    }
    
    if (!isEmpty(redirectSource) && !isRedirect(redirectSource)) {
      getResponse().sendRedirect(redirectSource);
    } else {
      getResponse().sendRedirect(getBrandSpecs().getCategoryNotFoundPath());
    }
    
    return false;
  }
  
  // returns redirecturl after appending icid tag
  
  protected String getRedirectSource(NMCategory category) {
    
    StringBuffer redirectURL = null;
    String icid = getRequest().getParameter(ICID);
    String redirect = null;
    
    if (category.getRedirectURL() != null) {
      redirectURL = new StringBuffer(category.getRedirectURL());
    } else if (category.getLongDescription() != null) {
      redirectURL = new StringBuffer(category.getLongDescription());
    }
    
    // getting ICID parameter value from the request which is set in
    // subCategoryThumbnail
    
    if (redirectURL != null && !(redirectURL.indexOf(ICID) != -1) && icid != null) {
      if (redirectURL.indexOf(QUESTION_MARK) != -1) {
        redirectURL.append("&").append(ICID).append(EQUALS).append(icid);
      } else {
        redirectURL.append(QUESTION_MARK).append(ICID).append(EQUALS).append(icid);
      }
    }
    
    if (redirectURL != null) {
      redirect = NMFormHelper.reviseUrl(redirectURL.toString());
    }
    
    return redirect;
  }
  
  protected boolean isRedirect(String redirectSource) {
    if (redirectSource.indexOf("X0.jsp") != -1) {
      return true;
    }
    
    if (redirectSource.indexOf("X1.jsp") != -1) {
      return true;
    }
    
    // references to jhtml files will indirectly cause infinite redirect
    if (redirectSource.indexOf(".jhtml") != -1) {
      return true;
    }
    
    return false;
  }
}
