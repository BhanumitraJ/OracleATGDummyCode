package com.nm.commerce.pagedef.evaluator.assistance;

import java.util.ArrayList;

import javax.servlet.jsp.PageContext;

import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.ajax.customercontact.services.CustomerContactService;
import com.nm.collections.EmailForwardingArray;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.components.CommonComponentHelper;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.NmoUtils;

/**
 * Updates variables and sets up any redirects before page execution. Returns true if page content should be output.
 */
public class ContactUsPageEvaluator extends SimplePageEvaluator {
  
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    DynamoHttpServletRequest request = this.getRequest();
    String emailType = !NmoUtils.isEmpty(request.getParameter("emailType")) ? request.getParameter("emailType") : "custemailforwarding";
    String subject = request.getParameter("subject");
    String department = !NmoUtils.isEmpty(request.getParameter("department")) ? request.getParameter("department") : null;
    
    CustomerContactService customerContactService = new CustomerContactService();
    request.setParameter("emailType", emailType);
    request.setParameter("emailArray", (EmailForwardingArray) customerContactService.getEmailArray(emailType));
    
    if (!NmoUtils.isEmpty(subject)) {
      request.setParameter("departments", customerContactService.getDepartments(subject, emailType));
      request.setParameter("emailforward", customerContactService.getFragments(subject, department, emailType));
    }
    getPageModel().setPageType(TMSDataDictionaryConstants.CONTACTUS);
    // Data Dictionary Attributes population.
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), getPageModel(), new TMSMessageContainer());
    
    return true;
  }
}
