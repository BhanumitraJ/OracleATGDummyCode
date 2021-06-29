package com.nm.commerce.pagedef.evaluator.checkout;

import javax.servlet.jsp.PageContext;

import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.MobileCheckoutPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.GenericLogger;

public class MobileCheckoutPageEvaluator extends SimplePageEvaluator {
  protected GenericLogger log = CommonComponentHelper.getLogger();
  
  @Override
  protected PageModel allocatePageModel() {
    return new MobileCheckoutPageModel();
  }
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    boolean returnValue = super.evaluate(pageDefinition, pageContext);
    return returnValue;
  }
}
