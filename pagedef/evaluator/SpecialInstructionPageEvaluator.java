package com.nm.commerce.pagedef.evaluator;

import static com.nm.tms.constants.TMSDataDictionaryConstants.MONOGRAM_PATH;

import javax.servlet.jsp.PageContext;

import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.droplet.SpecInstStepLookup;
import com.nm.formhandler.SpecialInstructionsProdHandler;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.NMFormHelper;
import com.nm.utils.StringUtilities;

public class SpecialInstructionPageEvaluator extends SimplePageEvaluator {
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    boolean returnValue = super.evaluate(pageDefinition, pageContext);
    PageModel pageModel = getPageModel();
    
    if (returnValue) {
      DynamoHttpServletRequest request = getRequest();
      DynamoHttpServletResponse response = getResponse();
      SpecialInstructionsProdHandler specialInstructionsProdHandler = (SpecialInstructionsProdHandler) request.resolveName("/nm/formhandler/SpecialInstructionsProdHandler");
      
      if (!specialInstructionsProdHandler.getBeingMonogrammed()) {
        String url = "/specialinstruction/sessionexpired.jsp";
        url = NMFormHelper.reviseUrl(url);
        response.sendLocalRedirect(url, request);
        returnValue = false;
      }
      
      if (returnValue && pageDefinition.getId().equals("Step1All")) {
        // find out if there is a style or color
        SpecInstStepLookup specInstStepLookup = (SpecInstStepLookup) request.resolveName("/nm/droplet/SpecInstStepLookup");
        specInstStepLookup.service(specialInstructionsProdHandler.getProdId(), "styleorcolor");
        if (!specInstStepLookup.isColorFlag() && !specInstStepLookup.isStyleFlag()) {
          String url = "/specialinstruction/all/step2.jsp?hasStyle=false";
          // find out if this is csr by checking for test in the url
          if (StringUtilities.isNotEmpty(request.getParameter("test"))) {
            url += "&test=true&siCodeset=" + request.getParameter("siCodeset") + "&tabView=" + request.getParameter("tabView");
          }
          url = NMFormHelper.reviseUrl(url);
          response.sendLocalRedirect(url, request);
          returnValue = false;
        }
      }
      
      if (returnValue && pageDefinition.getId().equals("Step2All")) {
        // first find out if there is a style or color
        SpecInstStepLookup specInstStepLookup = (SpecInstStepLookup) request.resolveName("/nm/droplet/SpecInstStepLookup");
        specInstStepLookup.service(specialInstructionsProdHandler.getProdId(), "personalization");
        if (!specInstStepLookup.isPersonalizationFlag()) {
          String url = "/specialinstruction/preview.jsp";
          // checking for test in the url to find out if this was called by CSR
          if (StringUtilities.isNotEmpty(request.getParameter("test"))) {
            url += "?test=true&siCodeset=" + request.getParameter("siCodeset") + "&tabView=" + request.getParameter("tabView");
          }
          url = NMFormHelper.reviseUrl(url);
          response.sendLocalRedirect(url, request);
          returnValue = false;
        }
      }
    }
    pageModel.setPageType(MONOGRAM_PATH);
    // Data Dictionary Attributes population.
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), pageModel, new TMSMessageContainer());
    return returnValue;
  }
}
