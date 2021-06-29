package com.nm.commerce.pagedef.evaluator.product;

import com.nm.choicestream.ChoiceStreamParameters;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.helper.ProductDepictionTypeHelper;
import com.nm.commerce.pagedef.model.ProductPageModel;
import com.nm.formhandler.ProdHandler;
import com.nm.utils.StringUtilities;

import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import java.util.ArrayList;
import java.util.List;

public class ProductListHelper {
  public static List<NMProduct> setupLineItemsProductList(NMProduct currentProduct, ProdHandler prodHandler) {
    List<NMProduct> productList = setupProductList(currentProduct, prodHandler, false, false, true, false);

    return productList;
  }

  public static List<NMProduct> setupLineItemsProductList(NMProduct currentProduct, ProdHandler prodHandler, boolean isShowUnavailableProducts) {
    List<NMProduct> productList = setupProductList(currentProduct, prodHandler, false, isShowUnavailableProducts, true, false);

    return productList;
  }

  public static List<RelatedProductBean> setupCompleteTheLookProductList(NMProduct currentProduct, ProdHandler prodHandler, DynamoHttpServletRequest request, boolean isShowLiveProductsOnly) {
    List<NMProduct> productList = setupProductList(currentProduct, prodHandler, true, false, false, isShowLiveProductsOnly);
    List<RelatedProductBean> relatedProdList = setupRelatedProductsList("NM", productList, request);
    return relatedProdList;
  }
  
  public static List<NMProduct> buildLCDisplayProductList(NMProduct currentProduct, ProdHandler prodHandler, DynamoHttpServletRequest request, boolean isShowLiveProductsOnly ) {
	    List<RepositoryItem> relatedColorProducts = (List<RepositoryItem>) currentProduct.getPropertyValue("fixedRelatedProducts");
	    if (relatedColorProducts == null) {
	      return null;
	    }
	    List<NMProduct> productList = new ArrayList<NMProduct>();
	    for (RepositoryItem moreColorsItem : relatedColorProducts) {
	      productList.add(new NMProduct(moreColorsItem));
	    }
	    prodHandler.setClearDisplayProductList("");
	    prodHandler.setIsShowUnavailableProducts(false);
	    prodHandler.setShowLiveProductsOnly(isShowLiveProductsOnly);
	    prodHandler.setAddMultipleProductsToDisplayProductList(productList);
	    List<NMProduct> displayProductList = prodHandler.getDisplayProductList();
	    setupRelatedProductsList("NM", displayProductList, request);
	    //System.out.println("productList--"+productList.size());
	    return productList;
	  }

  public static List<RelatedProductBean> buildColorsRelatedProductBeanList(NMProduct currentProduct, ProdHandler prodHandler, DynamoHttpServletRequest request, boolean isShowLiveProductsOnly ) {
    List<RepositoryItem> relatedColorProducts = (List<RepositoryItem>) currentProduct.getPropertyValue("fixedRelatedProducts");
    if (relatedColorProducts == null) {
      return null;
    }
    List<NMProduct> productList = new ArrayList<NMProduct>();
    for (RepositoryItem moreColorsItem : relatedColorProducts) {
      productList.add(new NMProduct(moreColorsItem));
    }
    prodHandler.setClearDisplayProductList("");
    prodHandler.setIsShowUnavailableProducts(false);
    prodHandler.setShowLiveProductsOnly(isShowLiveProductsOnly);
    prodHandler.setAddMultipleProductsToDisplayProductList(productList);
    List<NMProduct> displayProductList = prodHandler.getDisplayProductList();

    return setupRelatedProductsList("NM", displayProductList, request);
  }

  public static List<NMProduct> setupQuickViewLineItemsProductList(NMProduct currentProduct, ProdHandler prodHandler) {
    return setupProductList(currentProduct, prodHandler, false, false, true, false);
  }

  /**
   *
   * setup display product list for the main body line item of product page or the Complete The Look section of product page
   *
   * @param currentProduct
   * @param prodHandler
   * @param isSetupAsRelatedProductList
   * @param isShowUnavailableProducts
   * @param isShowFirstUnavailableProduct
   * @return NMProduct list to be displayed
   */
  public static List<NMProduct> setupProductList(NMProduct currentProduct, ProdHandler prodHandler, boolean isSetupAsRelatedProductList, boolean isShowUnavailableProducts,
          boolean isShowFirstUnavailableProduct, boolean isShowLiveProductsOnly) {
    prodHandler.setClearDisplayProductList("");
    prodHandler.setIsShowUnavailableProducts(isShowUnavailableProducts);
    prodHandler.setShowLiveProductsOnly(isShowLiveProductsOnly);
    List<NMProduct> relatedProducts = currentProduct.getRelatedProductList();
    List<NMProduct> productList = currentProduct.getProductList();

    if (isSetupAsRelatedProductList) {
      if (!relatedProducts.isEmpty()) {
        prodHandler.setAddMultipleProductsToDisplayProductList(relatedProducts);
      }
    } else {
      if (!productList.isEmpty()) {
        prodHandler.setAddMultipleProductsToDisplayProductList(productList);
        List<NMProduct> displayList = prodHandler.getDisplayProductList();
        if (displayList.isEmpty()) {
          if (isShowFirstUnavailableProduct) {
            prodHandler.setIsShowUnavailableProducts(true);
            List<NMProduct> unavailableProd = currentProduct.getUnavailableProductList();
            if (!unavailableProd.isEmpty()) {
              prodHandler.setAddSingleProductToDisplayProductList(unavailableProd.get(0));
            }
          } else {
            prodHandler.setIsShowUnavailableProducts(false);
          }
        }
      }
    }

    List<NMProduct> displayProductList = new ArrayList<NMProduct>(prodHandler.getDisplayProductList());
    return displayProductList;
  }

  /**
   * set up display product list for the You May Also Like section of the product page
   *
   * @param currentProduct
   * @param prodHandler
   * @return
   * @throws Exception
   */
  public static List<RelatedProductBean> setupYMALProductList(NMProduct currentProduct, ProdHandler prodHandler, List<String> choiceStreamProductIds, DynamoHttpServletRequest request)
          throws Exception {
    prodHandler.setClearDisplayProductList("");
    prodHandler.setIsShowUnavailableProducts(false);

    ChoiceStreamParameters choiceStream = (ChoiceStreamParameters) request.resolveName("/nm/choicestream/ChoiceStream");
    boolean choiceStreamRecommendationsEnabled = choiceStream.isRecommendationsEnabled();
    List<NMProduct> relatedProducts = currentProduct.getRelatedProductList2();

    String recommendationSource = "NM";
    if (choiceStreamRecommendationsEnabled) {
      List<NMProduct> recommendationsList = prodHandler.getRecommendationsList(currentProduct, choiceStreamProductIds, false);
      String depiction = (String) ProductDepictionTypeHelper.getProductDepiction(currentProduct);
      boolean isBeauty = StringUtilities.isNotEmpty(depiction) ? depiction.equalsIgnoreCase("BEAUTY") : false;
      if (isBeauty) {
        if (relatedProducts != null && !relatedProducts.isEmpty()) {
          prodHandler.setAddMultipleProductsToDisplayProductList(relatedProducts);
        }
      } else {
        boolean containsItem = currentProduct.getFlgMerchManual();
        if (containsItem) { // manually suggest ymal products
          if (relatedProducts != null && !relatedProducts.isEmpty()) {
            prodHandler.setAddMultipleProductsToDisplayProductList(relatedProducts);
          }
        } else if (recommendationsList != null && !recommendationsList.isEmpty()) { // choicestream suggest ymal products
          recommendationSource = "CS";
          prodHandler.setDplSource("CS");
          List<NMProduct> csProdRecommendationsList = (List<NMProduct>) currentProduct.getCSProdRecommendationsList();
          if (csProdRecommendationsList != null && !csProdRecommendationsList.isEmpty()) {
            prodHandler.setAddMultipleProductsToDisplayProductList(csProdRecommendationsList);
          } else {
            prodHandler.setAddMultipleProductsToDisplayProductList(recommendationsList);
          }
        } else if (relatedProducts != null && !relatedProducts.isEmpty()) { // choicestream failed
          prodHandler.setAddMultipleProductsToDisplayProductList(relatedProducts);
        }
      }
    } else if (relatedProducts != null && !relatedProducts.isEmpty()) { // not choiceStreamRecommendationsEnabled
      prodHandler.setAddMultipleProductsToDisplayProductList(relatedProducts);
    }

    List<NMProduct> productList = prodHandler.getDisplayProductList();
    List<RelatedProductBean> relatedProdList = setupRelatedProductsList(recommendationSource, productList, request);
    return relatedProdList;
  }

  /**
   * calculate the list of products to be displayed for ymal/ctl section on product page
   *
   * @param productList
   * @return a list of RelatedProduct
   */
  private static List<RelatedProductBean> setupRelatedProductsList(String recommendationSource, List<NMProduct> productList, DynamoHttpServletRequest request) {
    List<RelatedProductBean> relatedProdList = new ArrayList<RelatedProductBean>();
    boolean isDisplayable = false;

    for (NMProduct product : productList) {
      // only the first or the only product in suite is going to be displayed
      if (product.getIsInSuite()) {
        if (product.getIsFirstShowableProductInSuite() || product.getIsOnlyProductInSuite()) {
          isDisplayable = true;
        } else {
          isDisplayable = false;
        }
      } else {
        isDisplayable = true;
      }
      // this product is going to be displayed, add it to the RelatedProduct list
      if (isDisplayable) {
        RelatedProductBean relatedProd = ProductPageEvaluatorUtils.buildRelatedProduct(recommendationSource, product, request);
        relatedProdList.add(relatedProd);
      }
    }
    return relatedProdList;
  }
}
