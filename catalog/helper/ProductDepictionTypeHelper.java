package com.nm.commerce.catalog.helper;

import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSuite;
import com.nm.commerce.catalog.NMSuperSuite;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.StringUtilities;

public class ProductDepictionTypeHelper {
  
  public final static String BEAUTY = "BEAUTY";
  public final static String GENERIC = "GENERIC";
  public final static String HOME = "HOME";
  
  private final static String beautyDepictionLetters = "CR";
  private final static String homeDepictionLetters = "EGQH";
  
  public static String getProductDepiction(NMProduct nmProduct) throws Exception {
    String depiction = "";
    try {
      if (nmProduct != null) {
        String cmosItem = "";
        switch (nmProduct.getType()) {
          case 0:
            cmosItem = nmProduct.getCmosItemCode();
          break;
          case 1:
            NMSuite nmSuite = new NMSuite(nmProduct.getId());
            /*
             * WR:41536 Added a null and size on suites check to avoid IndexOutOfBoundsException
             */
            if (nmSuite.getProductList() != null && !nmSuite.getProductList().isEmpty()) {
              cmosItem = ((NMProduct) nmSuite.getProductList().get(0)).getCmosItemCode();
            }
          break;
          case 2:
            NMSuperSuite nmSSuite = new NMSuperSuite(nmProduct.getId());
            /*
             * WR:41536 Added a null and size check on super suites to avoid IndexOutOfBoundsException
             */
            if (nmSSuite.getProductList() != null && !nmSSuite.getProductList().isEmpty()) {
              cmosItem = ((NMProduct) nmSSuite.getProductList().get(0)).getCmosItemCode();
            }
          break;
        }
        
        if (StringUtilities.isNotEmpty(cmosItem)) {
          try {
            String depictionCode = cmosItem.substring(0, 1).toUpperCase();
            if (beautyDepictionLetters.contains(depictionCode)) {
              depiction = BEAUTY;
            } else if (homeDepictionLetters.contains(depictionCode)) {
              depiction = HOME;
            } else {
              depiction = GENERIC;
            }
          } catch (IndexOutOfBoundsException e) {
            System.out.println("ProductDepictionTypeHelper: " + nmProduct.getId() + " could not get depiction code from CmosItemCode \"" + cmosItem + "\".");
            throw e;
          }
        }
      }
    } catch (Exception ex) {
      System.out.println("ProductDepictionTypeHelper has Exception: " + ex.toString());
    }
    return depiction;
  }
  
  private ProdSkuUtil prodSkuUtil;
  
  public ProdSkuUtil getProdSkuUtil() {
    if (prodSkuUtil == null) {
      prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    }
    return prodSkuUtil;
  }
}// end class
