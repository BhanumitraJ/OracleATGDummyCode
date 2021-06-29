package com.nm.commerce.checkout.beans;

import atg.repository.RepositoryItem;
import atg.servlet.ServletUtil;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.components.CommonComponentHelper;
import com.nm.profile.ProfileProperties;
import com.nm.utils.StringUtilities;

public class ResultBeanHelper {

  ResultBeanHelper INSTANCE = null;

  private ResultBeanHelper() {}

  public ResultBeanHelper getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new ResultBeanHelper();
    }
    return INSTANCE;
  }

  public static void addSaveForLaterItem(final ResultBean bean, final NMCommerceItem item) {
    bean.setEventType(ResultBeanEventType.ADD_SFL_ITEM);
    bean.setCommerceItemId(item.getId());
    bean.setCmosSkus(item.getCmosSKUId()+":"+item.getQuantity());
    bean.setProductId(item.getProductId());
    bean.setOriginalQuantity(item.getQuantity());
    bean.setPrice(item.getPriceInfo().getAmount());
    bean.setCmosProductId(item.getCmosCatalogId() + "_" + item.getCmosItemCode());
    bean.setCmosItemCode(item.getCmosItemCode());
  }

  public static void addSaveForLaterItemToCart(final ResultBean bean, final NMCommerceItem item) {
    bean.setEventType(ResultBeanEventType.MOVE_SFL_TO_CART);
    bean.setCommerceItemId(item.getId());
    bean.setProductId(item.getProductId());
    bean.setCmosSkus(item.getCmosSKUId());
    bean.setOriginalQuantity(0);
    bean.setQuantity(item.getQuantity());
    bean.setPrice(item.getPriceInfo().getAmount());
    bean.setCmosProductId(item.getCmosCatalogId() + "_" + item.getCmosItemCode());
    bean.setCmosItemCode(item.getCmosItemCode());
    if (StringUtilities.isNotEmpty(item.getSelectedInterval())) {
      bean.setReplenishment("Every " + item.getSelectedInterval() + " days");
    }
  }

  public static void removeItemFromCart(final ResultBean bean, final NMCommerceItem item) {
    bean.setEventType(ResultBeanEventType.REMOVE_ORDER_ITEM);
    bean.setCommerceItemId(item.getId());
    bean.setProductId(item.getProductId());
    bean.setCmosProductId(item.getCmosCatalogId() + "_" + item.getCmosItemCode());
    bean.setCmosItemCode(item.getCmosItemCode());
    bean.setOriginalQuantity(item.getQuantity());
    bean.setQuantity(item.getQuantity());
    bean.setPrice(item.getPriceInfo().getAmount());
    bean.getAdditionalAttributes().put("prodName", item.getProduct().getDisplayName());
    bean.getAdditionalAttributes().put("skuId", item.getSkuNumber());
    bean.getAdditionalAttributes().put("salePrice", Double.toString(item.getPriceInfo().getSalePrice()));
    bean.getAdditionalAttributes().put("retailPrice", item.getProduct().getRetailPrice());
    bean.getAdditionalAttributes().put("color", item.getProdVariation1());
  }

  public static void removeItemFromSFL(final ResultBean bean, final RepositoryItem saveForLaterItem) {
    if (saveForLaterItem != null) {
      final String productId = (String) saveForLaterItem.getPropertyValue("productId");
      final Integer quantity = (Integer) saveForLaterItem.getPropertyValue("quantity");
      final String selectedInterval = (String) saveForLaterItem.getPropertyValue("selectedInterval");
      if (StringUtilities.isNotEmpty(productId)) {
        final NMProduct nmProduct = CommonComponentHelper.getProdSkuUtil().getProductObject(productId);
        if (nmProduct != null) {
          bean.setEventType(ResultBeanEventType.REMOVE_SFL_ITEM);
          bean.setProductId(productId);
          bean.setCmosProductId(nmProduct.getCmosCatalogItem());
          bean.setCmosItemCode(nmProduct.getCmosItemCode());
          bean.setOriginalQuantity(quantity.longValue());
          bean.setQuantity(0);
          bean.setCmosCatalogId(nmProduct.getCmosCatalogId());
          bean.setPrice(nmProduct.getPrice());
          bean.setSelectedInterval(selectedInterval);
          bean.setCmosSkus(nmProduct.getSku(0).getCmosSku());
        }
      }
    }
  }

  public static void updateItemInCart(final ResultBean bean, final NMCommerceItem item, final long originalQuantity) {
    bean.setEventType(ResultBeanEventType.UPDATE_ORDER_ITEM);
    bean.setCommerceItemId(item.getId());
    bean.setProductId(item.getProductId());
    bean.setCmosProductId(item.getCmosCatalogId() + "_" + item.getCmosItemCode());
    bean.setCmosItemCode(item.getCmosItemCode());
    bean.setCmosSkus(item.getCmosSKUId());
    bean.setQuantity(item.getQuantity());
    bean.setOriginalQuantity(originalQuantity);
    bean.setPrice(item.getPriceInfo().getAmount());
  }

  public static void updateReplenishmentItem(final ResultBean bean, final NMCommerceItem item, final String replenishment, final boolean addedReplishment) {
    updateItemInCart(bean, item, 0);
    bean.setEventType(ResultBeanEventType.UPDATE_REPLENISHMENT_ITEM);
    bean.setReplenishment(replenishment);
    bean.setAddedReplishment(addedReplishment);
  }

  public static void login(final ResultBean bean, final NMProfile profile, final ResultBeanEventType type) {
    final ContactInfo billingAddress = CheckoutAPI.getContactInfo(profile, ProfileProperties.Profile_billingAddress);
    bean.setProfile(profile);
    bean.setBillingAddress(billingAddress);
    bean.setEventType(type);
  }

  public static void addGWPSamples(final ResultBean bean) {
    bean.setEventType(ResultBeanEventType.ADD_GWP_SAMPLES);
  }

}
