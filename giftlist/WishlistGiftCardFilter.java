package com.nm.commerce.giftlist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.giftlist.GiftItem;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.StringUtilities;

public class WishlistGiftCardFilter {
  private ProdSkuUtil prodSkuUtil;
  private int productsPerPage = 50;
  private int numberOfPagesToDisplay = 15;
  
  public WishlistGiftCardFilter() {}
  
  public WishlistPagingModel filter(int currPageNumber, List<GiftItem> registryProductList, HttpServletRequest request) {
    WishlistPagingModel returnValue = new WishlistPagingModel();
    List<GiftItem> filteredList = new ArrayList<GiftItem>();
    double vgcTotal = 0d;
    double tgcTotal = 0d;
    int prevPageNumber = 0;
    int nextPageNumber = 2;
    int lastPageNumber = 1;
    List<Integer> pageNumberList = new ArrayList<Integer>();
    String wishlistUrl = "";
    
    if (registryProductList != null && !registryProductList.isEmpty()) {
      // remove type 6 & 7 (gift cards) from list.
      Iterator<GiftItem> iterator = registryProductList.iterator();
      while (iterator.hasNext()) {
        GiftItem giftItem = (GiftItem) iterator.next();
        if (giftItem == null) {
          iterator.remove();
          continue;
        }
        
        String productId = giftItem.getProductId();
        NMProduct product = prodSkuUtil.getProductObject(productId);
        if (product == null) {
          iterator.remove();
          continue;
        }
        
        if (product.getMerchandiseType() != null && (product.getMerchandiseType().equals("6") || product.getMerchandiseType().equals("7"))) {
          long quantityPurchased = giftItem.getQuantityPurchased();
          
          if (product.getMerchandiseType().equals("6")) {
            if (product.getPrice() != null) {
              tgcTotal += (product.getPrice().doubleValue() * quantityPurchased);
            }
          }
          if (product.getMerchandiseType().equals("7")) {
            if (product.getPrice() != null) {
              vgcTotal += (product.getPrice().doubleValue() * quantityPurchased);
            }
          }
          iterator.remove();
        }
      }
      
      // Determine last page
      lastPageNumber = new Double(Math.ceil((double) registryProductList.size() / (double) getProductsPerPage())).intValue();
      
      // if the user changes the pg number to be larger than the last page number, send them page 1
      if (currPageNumber > lastPageNumber) {
        currPageNumber = 1;
      }
      
      // determine the start and stop products for this page
      int startPos = (currPageNumber - 1) * getProductsPerPage();
      int stopPos = startPos + getProductsPerPage();
      
      // make sure that we do not go beyond the products that are available
      if (stopPos >= registryProductList.size()) {
        stopPos = registryProductList.size();
      }
      
      // get the list of items to be displayed on page
      if (startPos <= stopPos) {
        for (int pos = startPos; pos < stopPos; pos++) {
          GiftItem giftItem = (GiftItem) registryProductList.get(pos);
          if (giftItem != null) {
            filteredList.add(giftItem);
          }
        }
      }
      
      // Determine the page numbers to be displayed
      int pagesBeforeAfterCurrent = ((getNumberOfPagesToDisplay() - 1) / 2);
      int startPageNumber = currPageNumber - pagesBeforeAfterCurrent;
      int stopPageNumber = currPageNumber + pagesBeforeAfterCurrent;
      if (startPageNumber < 1) {
        startPageNumber = 1;
        if (getNumberOfPagesToDisplay() > lastPageNumber) {
          stopPageNumber = lastPageNumber;
        } else {
          stopPageNumber = getNumberOfPagesToDisplay();
        }
      } else if (stopPageNumber > lastPageNumber) {
        stopPageNumber = lastPageNumber;
        startPageNumber = lastPageNumber - getNumberOfPagesToDisplay() + 1;
        if (startPageNumber < 1) {
          startPageNumber = 1;
        }
      }
      
      // Add display page numbers to list
      for (int pos = startPageNumber; pos <= stopPageNumber; pos++) {
        pageNumberList.add(new Integer(pos));
      }
      
      // Define the previous and next page numbers
      if (currPageNumber > 1) {
        prevPageNumber = currPageNumber - 1;
      }
      if (currPageNumber < lastPageNumber) {
        nextPageNumber = currPageNumber + 1;
      }
      
      // START: Build url without page number
      // will add in jhtml from pageNumberList
      String query = request.getQueryString();
      if (StringUtilities.isEmpty(query)) {
        wishlistUrl = request.getRequestURI() + "?pg=";
      } else {
        String pgValue = request.getParameter("pg");
        if (pgValue == null) {
          wishlistUrl = request.getRequestURI() + "?" + request.getQueryString() + "&pg=";
        } else {
          StringBuffer queryWithoutPage = null;
          String[] params = query.split("&");
          for (String param : params) {
            if (!param.startsWith("pg=")) {
              if (queryWithoutPage == null) {
                queryWithoutPage = new StringBuffer("?");
                queryWithoutPage.append(param);
              } else {
                queryWithoutPage.append("&").append(param);
              }
            }
          }
          
          if (queryWithoutPage != null) {
            wishlistUrl = request.getRequestURI() + queryWithoutPage.toString() + "&pg=";
          } else {
            wishlistUrl = request.getRequestURI() + "?pg=";
          }
        }
      }
      // END: Build url without page number
    }
    
    returnValue.setVgcTotal(vgcTotal);
    returnValue.setTgcTotal(tgcTotal);
    returnValue.setFilteredList(filteredList);
    returnValue.setCurrPageNumber(currPageNumber);
    returnValue.setPrevPageNumber(prevPageNumber);
    returnValue.setNextPageNumber(nextPageNumber);
    returnValue.setPageNumberList(pageNumberList);
    returnValue.setLastPageNumber(lastPageNumber);
    returnValue.setWishlistUrl(wishlistUrl);
    returnValue.setTotalNumberOfPages(lastPageNumber);
    
    return returnValue;
  }
  
  /**
   * @return the prodSkuUtil
   */
  public ProdSkuUtil getProdSkuUtil() {
    return prodSkuUtil;
  }
  
  /**
   * @param prodSkuUtil
   *          the prodSkuUtil to set
   */
  public void setProdSkuUtil(ProdSkuUtil prodSkuUtil) {
    this.prodSkuUtil = prodSkuUtil;
  }
  
  public int getProductsPerPage() {
    return productsPerPage;
  }
  
  public void setProductsPerPage(int productsPerPage) {
    this.productsPerPage = productsPerPage;
  }
  
  public int getNumberOfPagesToDisplay() {
    return numberOfPagesToDisplay;
  }
  
  public void setNumberOfPagesToDisplay(int numberOfPagesToDisplay) {
    this.numberOfPagesToDisplay = numberOfPagesToDisplay;
  }
}
