package com.nm.commerce.pagedef.evaluator.template;

import com.nm.catalog.navigation.NMCategory;

/**
 * Updates variables and sets up any redirects before page execution. Returns false because it always does redirect of some sort instead of outputting regular template page content.
 */
public class RedirectAltPageEvaluator extends RedirectPageEvaluator {
  
  @Override
  protected String getRedirectSource(NMCategory category) {
    return category.getDescription();
  }
  
}
