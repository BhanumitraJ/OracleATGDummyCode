package com.nm.commerce.pagedef.evaluator;

import javax.servlet.jsp.PageContext;

import com.nm.commerce.pagedef.definition.PageDefinition;

public interface PageEvaluator {
  
  /**
   * Establishes attributes used by page, and sets up any redirects. Returns true if page content should be output.
   */
  public boolean evaluate(PageDefinition pageDefinition, PageContext context) throws Exception;
  
  /**
   * Perform business logic processing specific to the cached part of the content area. This can be used to avoid time-consuming operations used by the cached content area.
   */
  public void evaluateContent(PageContext context) throws Exception;
}
