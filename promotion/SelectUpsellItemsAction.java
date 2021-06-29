package com.nm.commerce.promotion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.nm.commerce.NMCommerceItem;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.scenario.ScenarioException;
import atg.servlet.DynamoHttpServletRequest;

/**
 * Dynamo scenario action that chooses highest ranked depiction code from order item list in a shopping cart. This depiction code is then used to lookup a list of categories that is associated with
 * it. This action is instantiated/fired per http request
 * 
 * @author Anthony Barnes
 * 
 */
public class SelectUpsellItemsAction extends ActionImpl {
  
  /**
   * Action intialization
   */
  public void initialize(Map pParameters) {
    orderManager_ = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    upSellRepo_ = (Repository) Nucleus.getGlobalNucleus().resolveName(UPSELL_REPO_PATH);
    
    orderHolderName_ = ComponentName.getComponentName(SHOPPING_CART_PATH);
  }
  
  /**
   * The upsell selection action. This action iterates through the items in the current order, and finds the highest ranked depiction code. It then finds the upsell categories associated with that
   * depiction code (via a table lookup) and stores the found categories in a session scoped component (that is later referenced in the shopping cart page
   */
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    DynamoHttpServletRequest request = pContext.getRequest();
    
    if (request != null) {
      // get current order
      OrderHolder oh = (OrderHolder) request.resolveName(SHOPPING_CART_PATH);
      Order order = oh.getCurrent();
      
      if (order != null) {
        // initialize highest rank depiction code
        String highestCode = "";
        
        // initialize highest rank counter
        int highestRank = Integer.MAX_VALUE;
        
        try {
          // Get an iterator over each commerceItemRelationship
          List items = orderManager_.getAllCommerceItemRelationships(order);
          Iterator iter = items.iterator();
          
          HashSet excludeSet = new HashSet();
          
          // iterate through order items
          while (iter.hasNext()) {
            CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
            
            CommerceItem thisItem = ciRel.getCommerceItem();
            
            if (thisItem instanceof NMCommerceItem) {
              NMCommerceItem nmci = (NMCommerceItem) thisItem;
              
              // parse depiction code from order item
              String itemCode = nmci.getCmosItemCode();
              String currCode = itemCode.substring(0, 1);
              
              // find rank of depiction code
              RepositoryItem rankings = upSellRepo_.getItem(currCode, "depictionRankings");
              if (rankings != null) {
                Integer rankObj = (Integer) rankings.getPropertyValue("ranking");
                
                // if depiction code is of higher ranking than previously stored highest code/rank,
                // replace highest code/rank with current code/rank
                int currRank = rankObj.intValue();
                if (currRank < highestRank) {
                  highestRank = currRank;
                  highestCode = currCode;
                }
              }
              
              // add productId of this item to list of products to exclude from upsells
              excludeSet.add(nmci.getAuxiliaryData().getProductId());
            }
          } // while (iter.hasNext())
          
          ArrayList categories = new ArrayList();
          
          // if the shopping cart isn't empty and a ranking was actually found
          if ((!items.isEmpty()) && (highestRank != Integer.MAX_VALUE)) {
            // look up category ids of all slots associated with highest ranked depiction code
            RepositoryView view = upSellRepo_.getView("depictionUpSellList");
            RqlStatement stmt = RqlStatement.parseRqlStatement("depiction_code = ?0 ORDER BY slot SORT ASC");
            
            RepositoryItem[] catItems = stmt.executeQuery(view, new String[] {highestCode});
            
            if (catItems != null) {
              // assign category ids to slots
              for (int i = 0; i < catItems.length; i++) {
                categories.add(catItems[i].getPropertyValue("category_id"));
              }
            }
          }
          
          UpSellRandomizer rand = (UpSellRandomizer) request.resolveName(RANDOMIZER_PATH, true);
          
          if (rand.isLoggingDebug()) rand.logDebug("highest ranked depiction code found was: " + highestCode);
          
          rand.setCategoriesToInclude(categories);
          rand.setProductsToExclude(excludeSet);
          rand.setDirty();
          
        } catch (RepositoryException re) {
          throw new ScenarioException(re);
        } catch (CommerceException ce) {
          throw new ScenarioException(ce);
        }// end catch
      } // if (order != null)
      
    }
  }
  
  // global/session component paths
  private static String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  private static String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  private static String UPSELL_REPO_PATH = "/nm/xml/UpSellRepository";
  private static String RANDOMIZER_PATH = "/nm/formhandler/UpSellRandomizer";
  
  private OrderManager orderManager_;
  private ComponentName orderHolderName_;
  private Repository upSellRepo_;
}
