package com.nm.commerce.upsell;

import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;

import atg.process.action.ActionImpl;

/**
 * Class Name: FillUpsellSlotsAction A custom action that intiates the upsell slot population by calling fillUpsellSlots() on the ShoppingCartUpsellFiller.
 * 
 * @author C. Chadwick
 * @author $Author: nmmc5 $
 * @since 10/2/2004 Last Modified Date: $Date: 2008/04/29 13:59:00CDT $
 * @version $Revision: 1.2 $
 */
public class FillUpsellSlotsAction extends ActionImpl {
  public static final String SHOPPING_CART_UPSELL_FILLER_PATH = "/nm/commerce/upsell/ShoppingCartUpsellFiller";
  
  /**
   * Executes the action for the custom action
   * 
   * @param processExecutionContext
   * 
   * @throws ProcessException
   */
  protected void executeAction(ProcessExecutionContext processExecutionContext) throws ProcessException {
    ShoppingCartUpsellFiller upsellFiller = (ShoppingCartUpsellFiller) processExecutionContext.resolveName(SHOPPING_CART_UPSELL_FILLER_PATH);
    upsellFiller.fillUpsellSlots();
  }
}

// ====================================================================
// File: FillUpsellSlotsAction.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
