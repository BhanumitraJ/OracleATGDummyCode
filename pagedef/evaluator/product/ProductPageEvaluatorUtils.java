package com.nm.commerce.pagedef.evaluator.product;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.nm.components.CommonComponentHelper;
import com.nm.droplet.AkamaiUrlDroplet;
import com.nm.droplet.ProductDepictionTypeLookup;
import com.nm.utils.AkamaiUrlHelper;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.StringUtilities;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSuite;

import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;

/**
 * This class contains helper methods for ProductPageEvaluator class
 * 
 * @author nmjh94
 * 
 */
public class ProductPageEvaluatorUtils {
  private ProductPageEvaluatorUtils() {}
  
  /**
   * This method is in replace of droplet HtmlWebDesignerNameLookup
   * 
   * @param name
   * @return
   */
  public static String findWebDesignerName(String name) {
    String webDesignerName = "";
    Map<String, String> mapPairing = null;
    String foundAsciiDesignerName = null;
    if (name == null || name.equals("")) {
      return webDesignerName;
    } else {
      webDesignerName = name.toString().trim();
      ProdSkuUtil psu = CommonComponentHelper.getProdSkuUtil();
      mapPairing = psu.getAsciiDesignerNamePairing();
      foundAsciiDesignerName = (String) mapPairing.get(webDesignerName);
      
      if (foundAsciiDesignerName != null) {
        webDesignerName = foundAsciiDesignerName;
      }
    }
    return webDesignerName;
  }
  
  /**
   * populate RelatedProductBean with values for current product
   * 
   * @param product
   * @return
   */
  public static RelatedProductBean buildRelatedProduct(String recommendationSource, NMProduct product, DynamoHttpServletRequest request) {
    RelatedProductBean relatedProduct = new RelatedProductBean();
    String prodUrl = "";
    String designerName = "";
    NMSuite parentSuite = null;
    String cmosCatalogId = product.getCmosCatalogId();
    String cmosItemCode = product.getCmosItemCode();
    String parentCmosCatalogId = "";
    String parentCmosItemCode = "";
    Boolean flgPricingAdornments;
    RepositoryItem productDataSource = product.getDataSource();
    Boolean isInSuite = false;
    String relatedProdId = "";
    
    if (product.getIsInSuite()) {
      isInSuite = true;
      parentSuite = product.getParentSuite();
      prodUrl = findProductImageUrl(product, true, request);
      designerName = parentSuite.getWebDesignerName();
      relatedProdId = parentSuite.getId();
      parentCmosCatalogId = parentSuite.getCmosCatalogId();
      parentCmosItemCode = parentSuite.getCmosItemCode();
      flgPricingAdornments = parentSuite.getFlgPricingAdornments();
      
    } else {
      prodUrl = findProductImageUrl(product, false, request);
      designerName = product.getWebDesignerName();
      relatedProdId = product.getId();
      flgPricingAdornments = product.getFlgPricingAdornments();
    }
    
    relatedProduct.setRecommendationSource(recommendationSource);
    relatedProduct.setCmosCatalogId(cmosCatalogId);
    relatedProduct.setCmosItemCode(cmosItemCode);
    relatedProduct.setDesignerName(designerName);
    relatedProduct.setFlgPricingAdornments(flgPricingAdornments);
    relatedProduct.setImageUrl(prodUrl);
    relatedProduct.setIsInSuite(isInSuite);
    relatedProduct.setParentCmosCatalogId(parentCmosCatalogId);
    relatedProduct.setParentCmosItemCode(parentCmosItemCode);
    relatedProduct.setParentSuite(parentSuite);
    relatedProduct.setProduct(product);
    relatedProduct.setProductDataSource(productDataSource);
    relatedProduct.setRelatedProdId(relatedProdId);
    return relatedProduct;
  }
  
  /**
   * find the mc or ec image Url for the product if mc or ec is not available, do not display any further images
   * 
   * @param prod
   * @param isInSuite
   * @return imageUrl;
   */
  private static String findProductImageUrl(NMProduct prod, boolean isInSuite, DynamoHttpServletRequest request) {
    String imageUrl = "";
    if (isInSuite) {
      imageUrl = (String) prod.getParentSuite().getImageUrl("mc");
    } else {
      imageUrl = (String) prod.getImageUrl("mc");
    }
    
    if (imageUrl == null || imageUrl.equals("")) {
      if (isInSuite) {
        imageUrl = (String) prod.getParentSuite().getImageOrShimUrl().get("ec");
      } else {
        imageUrl = (String) prod.getImageOrShimUrl().get("ec");
      }
    }
    
    imageUrl = calculateImageUrl(imageUrl, AkamaiUrlHelper.PRODUCT_URL, request);
    return imageUrl;
  }
  
  private static String calculateImageUrl(String urlIn, String urlType, DynamoHttpServletRequest request) {
    AkamaiUrlHelper akamaiUrlHelper = (AkamaiUrlHelper) request.resolveName("/nm/utils/AkamaiUrlHelper");
    String isSecure = request.getHeader("isSecure");
    boolean secure = ("1".equals(isSecure));
    String urlOut = akamaiUrlHelper.getAkamaiUrl(secure, urlType, urlIn);
    return urlOut;
  }
  
  /**
   * Find the size guide to represent the suite. If there are multiple types of size guides, take the size guide of the first product.
   * 
   * @param name
   * @return
   */
  public static String findSuiteSizeGuide(NMProduct nmProduct) {
    boolean sameSizeGuide = true;
    String suiteSizeGuide = "";
    String firstSizeGuide = "";
    String prevSizeGuide = "";
    
    List<NMProduct> productList = nmProduct.getProductList();
    Iterator<NMProduct> productListIterator = productList.iterator();
    while (productListIterator.hasNext()) {
      NMProduct nmProd = productListIterator.next();
      String sizeGuide = nmProd.getSizeGuide();
      if (StringUtilities.isNotEmpty(sizeGuide)) {
        if (StringUtilities.isEmpty(prevSizeGuide)) {
          prevSizeGuide = sizeGuide;
          firstSizeGuide = sizeGuide; // becomes default if not all the same size guide
        } else if (!prevSizeGuide.equals(sizeGuide)) {
          sameSizeGuide = false;
        }
      }
    }
    if (sameSizeGuide) {
      suiteSizeGuide = prevSizeGuide;
    } else {
      suiteSizeGuide = firstSizeGuide;
    }
    return suiteSizeGuide;
  }
}
