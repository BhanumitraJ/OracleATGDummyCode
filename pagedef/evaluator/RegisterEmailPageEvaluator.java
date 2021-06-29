package com.nm.commerce.pagedef.evaluator;

import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.servlet.ServletUtil;

import com.nm.commerce.NMProfile;
import com.nm.commerce.pagedef.definition.IframePageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.RegisterEmailPageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;

public class RegisterEmailPageEvaluator extends SimplePageEvaluator {
  private final String ROW_KEY = "ROW-EN";
  private final String PROFILE_ID_PLACE_HOLDER = "<PROFILE_ID>";
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    boolean returnValue = false;
    returnValue = super.evaluate(pageDefinition, pageContext);
    IframePageDefinition definition = (IframePageDefinition) pageDefinition;
    NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    String userCountry = nmProfile.getCountryPreference();
    String userLanguage = nmProfile.getLanguagePreference();
    String mapKey = userCountry.toUpperCase() + "-" + userLanguage.toUpperCase();
    Map<String, String> pathMap = definition.getInnerContentPathMap();
    // default innerPath to domestic
    String innerPath = definition.getInnerContentPath();
    if (pathMap != null) {
      Set<String> countryCodes = pathMap.keySet();
      if (countryCodes.contains(mapKey)) {
        innerPath = pathMap.get(mapKey);
      } else if (countryCodes.contains(ROW_KEY)) {
        innerPath = pathMap.get(ROW_KEY);
      }
      if (!StringUtils.isBlank(innerPath) && innerPath.contains(PROFILE_ID_PLACE_HOLDER)) {
        innerPath = innerPath.replace(PROFILE_ID_PLACE_HOLDER, nmProfile.getWebId());
      }
    }
    RegisterEmailPageModel model = (RegisterEmailPageModel) getPageModel();
    model.setInnerPath(innerPath);
    getPageModel().setPageType(TMSDataDictionaryConstants.SWEEPSTAKES);
    // Data Dictionary Attributes population.
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), getPageModel(), new TMSMessageContainer());
    return returnValue;
    
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new RegisterEmailPageModel();
  }
}
