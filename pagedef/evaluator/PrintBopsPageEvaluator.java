package com.nm.commerce.pagedef.evaluator;

import java.util.ArrayList;

import javax.servlet.jsp.PageContext;

import com.nm.bops.BopsItem;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSku;
import com.nm.commerce.catalog.NMSuite;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.PrintBopsPageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.repository.stores.Store;
import com.nm.storeinventory.ItemAvailabilityLevel;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;

public class PrintBopsPageEvaluator extends SimplePageEvaluator {
  @Override
  protected PageModel allocatePageModel() {
    return new PrintBopsPageModel();
  }
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    PrintBopsPageModel pageModel = (PrintBopsPageModel) getPageModel();
    String storeId = (String) getRequest().getParameter("store");
    Store store = CheckoutComponents.getShipToStoreHelper().getStoreAddressByStoreNumber(storeId);
    String[] skuIds = ((String) getRequest().getParameter("skus")).split(",");
    String[] prodIds = ((String) getRequest().getParameter("productIds")).split(",");
    String[] qtys = ((String) getRequest().getParameter("qtys")).split(",");
    String suiteId = ((String) getRequest().getParameter("suiteId"));
    String[] frequencies = ((String) getRequest().getParameter("frequencies")).split(",");
    if (suiteId != null) {
      NMSuite suite = new NMSuite(suiteId);
      pageModel.setSuite(suite);
    }
    ArrayList<BopsItem> itemList = new ArrayList<BopsItem>();
    for (int i = 0; i < skuIds.length; i++) {
      NMSku sku = new NMSku(skuIds[i]);
      NMProduct product = new NMProduct(prodIds[i]);
      String qty = qtys[i];
      String frequency = frequencies[i];
      String cmVariation1 = product.getProdSkuUtil().getProdSkuVariation(product.getDataSource(), sku.getDataSource(), "cmVariation1");
      String cmVariation2 = product.getProdSkuUtil().getProdSkuVariation(product.getDataSource(), sku.getDataSource(), "cmVariation2");
      ItemAvailabilityLevel availabilityLevel = CommonComponentHelper.getStoreSkuInventoryUtils().getSkuInventoryByStoreNum(sku.getId(), storeId, qty);
      itemList.add(new BopsItem(product, sku, availabilityLevel, cmVariation1, cmVariation2, qty, frequency));
    }
    pageModel.setStoreBopsEligible(store.getflgShipToStore());
    pageModel.setStore(store);
    pageModel.setItemList(itemList);
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), pageModel, new TMSMessageContainer());
    return true;
  }
}
