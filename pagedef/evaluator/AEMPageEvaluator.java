package com.nm.commerce.pagedef.evaluator;

import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.AEM.AEMService;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.TemplatePageModel;

public class AEMPageEvaluator extends TemplatePageEvaluator {
	
	@Override
	public boolean evaluate( final PageDefinition pageDefinition, final PageContext pageContext ) throws Exception {
		if ( !super.evaluate( pageDefinition, pageContext ) ) {
			return false; 
		}
		
		final DynamoHttpServletRequest request = getRequest();
		final AEMService aemService = AEMService.getInstance();
		String body = aemService.getResult( request );
		
		final TemplatePageModel pageModel = (TemplatePageModel) getPageModel();
		if ( !StringUtils.isBlank( body ) ) {
			pageModel.setAemResponse( body );
		}
		
		return true;
	}
}
