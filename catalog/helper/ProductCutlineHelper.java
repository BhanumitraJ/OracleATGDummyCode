package com.nm.commerce.catalog.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;
import atg.servlet.ServletUtil;

import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.utils.LocalizationUtils;

public class ProductCutlineHelper {
  
  private static final String productTopDiv = "<div class=\"productTop\">";
  private static final String cutlineOverviewTitleDiv = "<div class=\"cutlineOverviewTitle\">";
  private static final String suiteOverviewTitleDiv = "<div class=\"suiteOverviewTitle\">";
  private static final String cutlineDetailsTitleDiv = "<div class=\"cutlineDetailsTitle\">";
  private static final String suiteDetailsTitleDiv = "<div class=\"suiteDetailsTitle\">";
  private static final String cutlineDetailsDiv = "<div class=\"cutlineDetails\">";
  private static final String suiteDetailsDiv = "<div class=\"suiteDetails\">";
  private static final String productCutlineDiv = "<div itemprop=\"description\" class=\"productCutline\">";
  private static final String productSizeGuideDiv = "<div class=\"productSizeGuide\">";
  private static final String productBottomDiv = "<div class=\"productBottom\">";
  private static final String suiteCutlineDiv = "<div class=\"suiteCutline\">";
  private static final String suiteTopDiv = "<div class=\"suiteTop\">";
  private static final String suiteProductDiv = "<ul class=\"suiteProducts\">";
  private static final String suiteProductCutlineDiv = "<div itemprop=\"description\" class=\"suiteProductCutline\">";
  private static final String suiteProductSizeGuideDiv = "<ul class=\"suiteProductSizeGuides\">";
  private static final String suiteBottomDiv = "<div class=\"suiteBottom\">";
  private static final String CHINA_COUNTRY_CODE = "chinaCountryCode";
  
  public static String getLongDescriptionCutLine(NMProduct nmProduct) {
    
    String longDescription = "";
    String cutlineDetails = "";
    NMProfile nmProfile = null;
    String languageCode = null;
    
    LocalizationUtils utils = ((LocalizationUtils) Nucleus.getGlobalNucleus()
            .resolveName("/nm/utils/LocalizationUtils"));
    
    if(ServletUtil.getCurrentRequest() != null){
        nmProfile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
    }
    
    if(nmProfile != null){
        languageCode = nmProfile.getLanguagePreference();
    
    } else {
        languageCode = LocalizationUtils.ENGLISH_LANGUAGE_CODE; 
    }
    
    String suiteCopyTop = null;
    String suiteCopyBottom = null;
    
    /* INT-2246:International copy block */
    if (utils.isInternationalCopyBlock(languageCode)) {
      // long description is now overview
      longDescription = (String) nmProduct.getPropertyValue("intlLongDescription");// product
    
    } else {
      suiteCopyTop = (String) nmProduct.getPropertyValue("suiteCopyTop");// product
      longDescription = (String) nmProduct.getPropertyValue("longDescription");// product
      cutlineDetails = (String) nmProduct.getPropertyValue("cutlineDetails");// product
      suiteCopyBottom = (String) nmProduct.getPropertyValue("suiteCopyBottom");// product
    }
    
    /* INT-2246:International copy block */
    String cutlineOverviewTitle = (String) nmProduct.getPropertyValue("cutlineOverviewTitle");// product
    String cutlineDetailsTitle = (String) nmProduct.getPropertyValue("cutlineDetailsTitle");// product
    String sizeGuide = (String) nmProduct.getPropertyValue("sizeGuide");// product
    String altLongDescription = "";
    String cutLineDescription = "";
    String prodLongDescription = "";
    String SCT = "";
    String COT = "";
    String CDT = "";
    String CD = "";
    String LD = "";
    String SCB = "";
    String SG = "";
    // INDIVIDUAL PRODUCTS
    // 1. productTopDiv <div class="productTop">[product Copy Top]</div>
    // 2. productCutlineDiv <div class="productCutline">[product
    // cutline]</div>
    // 3. productBottomDiv <div class="productBottom">[product Copy
    // Bottom]</div>
    // 4. productSizeGuideDiv <div class="productSizeGuide">[unique size
    // guide]</div>
    
    int productType = nmProduct.getType();
    if (productType == 0) { // 0 = Product, 1 = Suite, 2= SuperSuite
      if (suiteCopyTop != null) {
        SCT = productTopDiv + suiteCopyTop + "</div>";
      }
      if (cutlineOverviewTitle != null) {
        COT = cutlineOverviewTitleDiv + cutlineOverviewTitle + "</div>";
      }
      
      // long desc is not called overview
      if (longDescription != null) {
        LD = productCutlineDiv + "<h2>" + longDescription + "</h2>" + "</div>";
      }
      
      if (cutlineDetailsTitle != null) {
        CDT = cutlineDetailsTitleDiv + cutlineDetailsTitle + "</div>";
      }
      
      if (!utils.isInternationalCopyBlock(languageCode)) {
        if (cutlineDetails != null) {
          CD = cutlineDetailsDiv + "<h2>" + cutlineDetails + "</h2>" + "</div>";
        }
      }
      
      if (suiteCopyBottom != null) {
        SCB = productBottomDiv + suiteCopyBottom + "</div>";
      }
      if (sizeGuide != null) {
        SG = productSizeGuideDiv + sizeGuide + "</div>";
      }
      altLongDescription = concatDescription(SCT, COT, LD, CDT, CD, SCB, SG);
    } else if (productType == 1) { // 0 = Product, 1 = Suite, 2= SuperSuite
      if (suiteCopyTop != null) {
        SCT = productTopDiv + suiteCopyTop + "</div>";
      }
      
      if ((longDescription == null) || (longDescription == "")) {
        // System.out.println("\n ***** ---->You are a SUITE without a LONGDESCRIPTION<----- ***");
        List<NMProduct> childProducts = new ArrayList<NMProduct>();
        childProducts = nmProduct.getProductList();
        Iterator<NMProduct> childProductsIterator = childProducts.iterator();
        int index = 0;
        String childProdLongDescription = "";
        String childCopyBeingBuilt = "";
        String prodCutlineDetails = "";
        
        while (childProductsIterator.hasNext()) {
          NMProduct prod = (NMProduct) childProductsIterator.next();
          RepositoryItem productRI = nmProduct.getProdSkuUtil().getProductRepositoryItem(prod.getId());
          if ((productRI != null) && (index == 0)) {
            String sg = (String) productRI.getPropertyValue("sizeGuide");
            if (sg != null && !(sg.equals(""))) {
              sizeGuide = "<li>" + sg + "</li>";
            }
          }
          // long description is now overview
          /* INT-2246:International copy block */
          if (utils.isInternationalCopyBlock(languageCode)) {
            prodLongDescription = (String) productRI.getPropertyValue("intlLongDescription");
          } else {
            prodLongDescription = (String) productRI.getPropertyValue("longDescription");
          }
          /* INT-2246:International copy block */
          
          String prodCutlineOverviewTitle = (String) productRI.getPropertyValue("cutlineOverviewTitle");// product
          
          String prodCutlineDetailsTitle = (String) productRI.getPropertyValue("cutlineDetailsTitle");// product
          /* INT-2246:International copy block */
          if (!utils.isInternationalCopyBlock(languageCode)) {
            prodCutlineDetails = (String) productRI.getPropertyValue("cutlineDetails");// product
          }
          /* INT-2246:International copy block */
          
          if (prodCutlineOverviewTitle != null) {
            childCopyBeingBuilt += suiteOverviewTitleDiv + prodCutlineOverviewTitle + "</div>";
          }
          
          String altProdDescription = "";
          if (prodLongDescription != null) {
            altProdDescription = "<li><strong>" + prod.getDisplayName() + ": </strong>" + suiteProductCutlineDiv + prodLongDescription + "</div>" + "</li>";
          } else {
            altProdDescription = "<li><strong>" + prod.getDisplayName() + "</strong>" + "</li>";
          }
          if (altProdDescription != null) {
            childProdLongDescription += altProdDescription;
          }
          
          childCopyBeingBuilt += altProdDescription;
          
          if (prodCutlineDetailsTitle != null) {
            childCopyBeingBuilt += suiteDetailsTitleDiv + prodCutlineDetailsTitle + "</div>";
          }
          /* INT-2246:International copy block */
          if (!utils.isInternationalCopyBlock(languageCode)) {
            if (prodCutlineDetails != null) {
              childCopyBeingBuilt += suiteDetailsDiv + prodCutlineDetails + "</div>";
            }
          }
          /* INT-2246:International copy block */
          index++;
        }// end child prod loop
        
        if (suiteCopyTop != null && !(suiteCopyTop.equals(""))) {
          SCT = suiteTopDiv + suiteCopyTop + "</div>";
        }
        if (childCopyBeingBuilt != null && !(childCopyBeingBuilt.equals(""))) {
          LD = suiteProductDiv + childCopyBeingBuilt + "</ul>";
        }
        if (suiteCopyBottom != null && !(suiteCopyBottom.equals(""))) {
          SCB = suiteBottomDiv + suiteCopyBottom + "</div>";
        }
        if (sizeGuide != null && !(sizeGuide.equals(""))) {
          SG = suiteProductSizeGuideDiv + sizeGuide + "</ul>";
        }
        altLongDescription = concatDescription(SCT, LD, SG, SCB);
      } else {
        // suite is not null or empty - this overrides suite break apart
        // logic.
        
        if (suiteCopyTop != null && !(suiteCopyTop.equals(""))) {
          SCT = suiteTopDiv + suiteCopyTop + "</div>";
        }
        if (cutlineOverviewTitle != null) {
          COT = suiteOverviewTitleDiv + cutlineOverviewTitle + "</div>";
        }
        
        LD = suiteCutlineDiv + longDescription + "</div>";
        
        if (cutlineDetailsTitle != null) {
          CDT = suiteDetailsTitleDiv + cutlineDetailsTitle + "</div>";
        }
        /* INT-2246:International copy block */
        if (!utils.isInternationalCopyBlock(languageCode)) {
          if (cutlineDetails != null) {
            CD = suiteDetailsDiv + cutlineDetails + "</div>";
          }
        }
        /* INT-2246:International copy block */
        
        if (suiteCopyBottom != null) {
          SCB = productBottomDiv + suiteCopyBottom + "</div>";
        }
        
        if (sizeGuide != null && !(sizeGuide.equals(""))) {
          SG = suiteProductSizeGuideDiv + sizeGuide + "</ul>";
        }
        // concatDescription(SCT, COT, LD, CDT, CD, SCB) + SG;
        altLongDescription = concatDescription(SCT, COT, LD, CDT, CD, SCB, SG);
      }
    }
    cutLineDescription = formatLongDescription(altLongDescription, nmProduct);
    return cutLineDescription;
  }
  
  private static String concatDescription(String topdesc, String cutlineOverviewTitle, String longDescr, String cutlineDetailTitle, String cutlineDetail, String bottomdesc, String sizeguide) {
    StringBuffer rtn = new StringBuffer();
    if (topdesc != null) rtn.append(topdesc);
    if (cutlineOverviewTitle != null) rtn.append(cutlineOverviewTitle);
    if (longDescr != null) rtn.append(longDescr);
    if (cutlineDetailTitle != null) rtn.append(cutlineDetailTitle);
    if (cutlineDetail != null) rtn.append(cutlineDetail);
    if (bottomdesc != null) rtn.append(bottomdesc);
    if (sizeguide != null) rtn.append(sizeguide);
    
    return rtn.toString();
  }
  
  private static String concatDescription(String topdesc, String longDescr, String sizeguide, String bottomdesc) {
    StringBuffer rtn = new StringBuffer();
    if (topdesc != null) rtn.append(topdesc);
    if (longDescr != null) rtn.append(longDescr);
    if (bottomdesc != null) rtn.append(bottomdesc);
    if (sizeguide != null) rtn.append(sizeguide);
    
    return rtn.toString();
  }
  
  /**
   * Name of Method: formatLongDescription
   * 
   * @return String
   * 
   *         The long description cut line may need to be reformatted because of encoding of more info More info is detected if %!--EJMP% appears in the long description
   * 
   *         The query below finds products that have this encoding. select * from dcs_product prd, nm_product_aux aux where long_description like '%!--EJMP%' and prd.product_id = aux.product_id and
   *         aux.flg_display = 1 and product_type = 0
   * 
   */
  private static String formatLongDescription(String longDescription, NMProduct nmProduct) {
    String cutLineDescription = "";
    try {
      int jumpIndexStart = -1;
      int jumpIndexEnd = -1;
      String jumpTextStart = "!--EJMP:";
      String jumpTextEnd = "-->";
      if (longDescription != null) {
        jumpIndexStart = longDescription.indexOf(jumpTextStart);
      }
      if (jumpIndexStart > -1) {
        jumpIndexEnd = longDescription.indexOf(jumpTextEnd, jumpIndexStart);
      }
      if (jumpIndexStart > -1 && jumpIndexEnd > -1) {
        cutLineDescription = longDescription.substring(0, (jumpIndexStart - 1));
        nmProduct.setMoreInfoLink(longDescription.substring(jumpIndexStart + jumpTextStart.length(), jumpIndexEnd));
        nmProduct.setMoreInfoText(longDescription.substring(jumpIndexEnd + jumpTextEnd.length()));
      } else {
        cutLineDescription = longDescription;
      }
    } catch (Exception ex) {
      System.out.println(ex.toString() + ": in frg_prodPage - error trying to break up long description for " + nmProduct.getId());
    }
    return cutLineDescription;
  }
  
}
