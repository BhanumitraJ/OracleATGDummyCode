/*
 * <ATGCOPYRIGHT> Copyright (C) 1997-2001 Art Technology Group, Inc. All Rights Reserved. No use, copying or distribution of this work may be made except in accordance with a valid license agreement
 * from Art Technology Group. This notice must be included on all copies, modifications and derivatives of this work.
 * 
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR
 * ITS DERIVATIVES.
 * 
 * "Dynamo" is a trademark of Art Technology Group, Inc. </ATGCOPYRIGHT>
 */

package com.nm.commerce.promotion;

import atg.commerce.promotion.*;
import atg.commerce.order.*;
import atg.commerce.CommerceException;
import atg.commerce.states.StateDefinitions;
import atg.repository.rql.*;
import atg.servlet.*;

import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;

import atg.repository.*;
import atg.scenario.*;
import atg.scenario.action.*;

import java.util.*;
import com.nm.commerce.*;
import com.nm.commerce.order.*;

/**
 * This class extends dynamo out of the box add item to order action to include additional Neiman's commerce item specifics.
 * 
 * @author Chee-Chien Loo
 */

public class NMAddItemToOrder extends AddItemToOrder {
  
  /** Parameter for product id information */
  public static final String PROMO_TYPE_PARAM = "promo_type";
  
  public void initialize(Map pParameters) throws ProcessException {
    super.initialize(pParameters);
    storeOptionalParameter(pParameters, PROMO_TYPE_PARAM, java.lang.String.class);
  }
  
  /**
   * This method will exted dynamo's add item to order action to suit NM's business requirement.
   * 
   * <P>
   * 
   * If different behavior is desired when an item is being added to the order, this method should be overriden.
   * 
   * This method will check for Instock before adding the promotional item to the cart The free item is designated in the scenario file when the addItemToOrder method is used, the skuid is entered
   * there.
   * 
   * @param pSkuId
   *          the sku id of the commerce item that will be created and added to the order
   * @param pProductId
   *          product id of the commerce item that will be created and added
   * @param pQuantity
   *          the quantity of the particular item to add
   * @param pContext
   *          the context in which the action is occuring
   * @exception CommerceException
   *              if an error occurs
   * @exception RepositoryException
   *              if an error occurs
   */
  protected void addItem(String pSkuId, String pProductId, long pQuantity, ProcessExecutionContext pContext) throws CommerceException, RepositoryException {
    DynamoHttpServletRequest req = pContext.getRequest();
    
    // System.out.println("*******1. DynamoHttpServletRequest getRequest()**********");
    
    Repository repository = (Repository) req.resolveName("/atg/commerce/catalog/ProductCatalog");
    // System.out.println("********productCatalog.xml has been resolved***********");
    
    // First look up the cmosSku id from the SkuId
    RepositoryItem SKUITEM = (RepositoryItem) repository.getItem(pSkuId, "sku");
    
    // System.out.println("**********3. executeQuery()***");
    // System.out.println("**SKUITEM***"+SKUITEM);
    
    // When we get the cmosSku, then check for inStock
    if (SKUITEM != null) {
      
      // System.out.println("items returned: " + SKUITEM.getRepositoryId());
      
      if (((Boolean) SKUITEM.getPropertyValue("inStock")).booleanValue()) {
        // System.out.println("instock is true");
        
        List sgs;
        String promoType;
        Order o = getOrderToAddItemTo(pContext);
        
        try {
          promoType = (String) getParameterValue(PROMO_TYPE_PARAM, pContext);
        } catch (ProcessException se) {
          throw new CommerceException(se);
        }
        
        if (o != null) {
          sgs = o.getShippingGroups();
          if (sgs != null && sgs.size() > 0) {
            ShippingGroup sg = (ShippingGroup) o.getShippingGroups().get(0);
            
            Long NMquantity = new Long(pQuantity);
            int ciQuantity = NMquantity.intValue();
            
            for (int ciNo = 0; ciNo < ciQuantity; ciNo++) {
              if (mOrderManager instanceof NMOrderManager) {
                NMOrderManager om = (NMOrderManager) mOrderManager;
                CommerceItem ci = om.addSeparateItemToShippingGroup(o, pSkuId, pProductId, Long.parseLong("1"), sg);
                if (ci instanceof NMCommerceItem) {
                  NMCommerceItem NMci = (NMCommerceItem) ci;
                  RepositoryItem prodItem = (RepositoryItem) NMci.getAuxiliaryData().getProductRef();
                  RepositoryItem skuItem = (RepositoryItem) NMci.getAuxiliaryData().getCatalogRef();
                  NMci.setCmosCatalogId((String) prodItem.getPropertyValue("cmosCatalogId"));
                  NMci.setCmosItemCode((String) prodItem.getPropertyValue("cmosItemCode"));
                  NMci.setCmosProdName((String) prodItem.getPropertyValue("displayName"));
                  NMci.setCmosSKUId((String) skuItem.getPropertyValue("cmosSKU"));
                  NMci.setDeptCode((String) prodItem.getPropertyValue("deptCode"));
                  NMci.setSourceCode((String) prodItem.getPropertyValue("sourceCode"));
                  NMci.setWebDesignerName((String) prodItem.getPropertyValue("cmDesignerName"));
                  NMci.setProdVariation1((String) skuItem.getPropertyValue("codeVariation1"));
                  NMci.setProdVariation2((String) skuItem.getPropertyValue("codeVariation2"));
                  NMci.setProdVariation3((String) skuItem.getPropertyValue("codeVariation3"));
                  NMci.setQuickOrder(false);
                  
                  // This is hardcoded for the assumption that the item added is an promotional item
                  NMci.setTransientStatus("promotion");
                  NMci.setPromoType(promoType);
                }
              }
            }
          }
          
        }
      }// if instock is true
    }
  }
}// end of class
