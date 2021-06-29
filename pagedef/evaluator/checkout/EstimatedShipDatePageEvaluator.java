package com.nm.commerce.pagedef.evaluator.checkout;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.PageContext;

import com.nm.collections.ServiceLevel;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.pagedef.definition.CheckoutPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;

public class EstimatedShipDatePageEvaluator extends CheckoutPageEvaluator {
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    
    final ShippingGroupAux[] shippingGroups = pageModel.getShippingGroups();
    if ((null == shippingGroups) || (shippingGroups.length == 0)) {
      getResponse().sendRedirect("/checkout/cart.jsp");
      return false;
    }
    
    final List<String> zipCodes = new ArrayList<String>();
    
    for (final NMRepositoryContactInfo address : pageModel.getProfile().getAllShippingNMAddresses()) {
      zipCodes.add(address.getPostalCode());
    }
    
    final String[] zipCodesArray = zipCodes.toArray(new String[0]);
    final String facilityNumber = "?";
    
    // pageModel.setShipDetails(CheckoutComponents.getEstimatedShipDateUtil().getEstimatedShipDates(zipCodesArray, facilityNumber));
    
    return estimateShipDate(pageModel);
  }
  
  public boolean estimateShipDate(final CheckoutPageModel pageModel) {
    
    /***************************************************************************************************
     * When implementing the new Estimated Ship Date, remove this and uncomment what's commented above.
     ***************************************************************************************************/
    
    final boolean isBOPSEnabled = CommonComponentHelper.getBrandSpecs().isBOPSEnabled();
    final boolean s2sServiceLevelUpgradeEnabled = CommonComponentHelper.getBrandSpecs().isS2sServiceLevelUpgradeEnabled();
    final String s2sServiceLevelUpgradeDisabledInterval = CommonComponentHelper.getBrandSpecs().getS2sServiceLevelUpgradeDisabledInterval();
    final boolean isShipToStoreEnabled = CommonComponentHelper.getSystemSpecs().isShipToStoreEnabled();
    for (final ShippingGroupAux group : pageModel.getShippingGroups()) {
      
      String grpSvcLvlCode = null;
      if ((group != null) && (group.getServiceLevel() != null)) {
        grpSvcLvlCode = group.getServiceLevel().getCode();
      }
      if (grpSvcLvlCode != null) {
        if (ServiceLevel.SL3_SERVICE_LEVEL_TYPE.equals(grpSvcLvlCode)) {
          // if this brand has BOPS enabled and ship to store enabled
          // and the ship to store service level upgrade is disabled
          // and if any commerce item has a ship to store id, set a
          // shipToStoreInterval on the commerce item using the
          // s2sUpgradeServiceLevelDisabledInterval
          if (!s2sServiceLevelUpgradeEnabled && isBOPSEnabled && isShipToStoreEnabled && (group.getItems() != null)) {
            final NMCommerceItem[] commerceItems = group.getItems();
            for (final NMCommerceItem commerceItem : commerceItems) {
              final boolean s2sEligible = commerceItem.isShipToStoreEligible();
              final boolean s2sFlag = commerceItem.getShipToStoreFlg();
              final String pickupStoreNo = commerceItem.getPickupStoreNo();
              if ((pickupStoreNo != null) && (pickupStoreNo != "")) {
                commerceItem.setShipToStoreInterval("(" + s2sServiceLevelUpgradeDisabledInterval + " business days)");
              }
            }
          }
        }
      }
    }
    // START: Samir: Patch:41837:
    /***************************************************************************************************/
    
    return true;
  }
  
  @Override
  protected void updateShippingGroups(final NMOrderImpl order, final CheckoutPageModel pageModel, final CheckoutPageDefinition checkoutPageDefinition) {
    super.updateShippingGroups(order, pageModel, checkoutPageDefinition);
    
    estimateShipDate(pageModel);
  }
}
