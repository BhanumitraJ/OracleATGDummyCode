package com.nm.commerce.promotion.rules;

import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;

import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.order.IOrderItem;

public class ItemMultiDeptRule extends ItemRule {
  
  public ItemMultiDeptRule() {
    setType(RuleHelper.ITEM_MULTI_DEPT_RULE);
  }
  
  public boolean test(ICommerceObject obj) {
    // System.out.println("testing ItemDeptRule");
    boolean isPass = false;
    IOrderItem item = getItem(obj);
    
    if (item != null) {
      String department = item.getDepartment();
      // System.out.println("item department: " + department);
      String classCode = item.getClassCode();
      // System.out.println("item class code: " + classCode);
      
      isPass = isItemInMultiDept(getValue(), department, classCode);
    }
    // System.out.println("   pass? " + isPass);
    return isPass;
  }
  
  private boolean isItemInMultiDept(String codeIn, String dept, String classCode) {
    
    // System.out.println("   looking up multidept with code " + codeIn);
    
    try {
      Repository productPromo = (Repository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/CSRMultiDeptClassRepository");
      RepositoryView productPromoView = (RepositoryView) productPromo.getView("multideptclass");
      RqlStatement statement = RqlStatement.parseRqlStatement("id = ?0");
      if (statement != null) {
        Object params[] = new Object[1];
        params[0] = codeIn;
        RepositoryItem multiDept[] = statement.executeQuery(productPromoView, params);
        if (multiDept != null) {
          for (int i = 0; i < multiDept.length; i++) {
            RepositoryItem multi = multiDept[i];
            String depts = (String) multi.getPropertyValue("deptCodes");
            String classCodes = (String) multi.getPropertyValue("classCodes");
            
            if (dept.equals(depts) && classCodes.indexOf(classCode) != -1) return true;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return false;
  }
  
  public String getDisplayValue() {
    String not = "";
    if (getValueComparator() == RuleHelper.NOTEQUAL) not = "Not";
    return "Item(s) " + not + " in Department(s): " + getValue();
  }
}
