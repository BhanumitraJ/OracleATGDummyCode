package com.nm.commerce.promotion.rules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.order.IOrderItem;

public class ItemDeptRule extends ItemRule {
  
  public ItemDeptRule() {
    setType(RuleHelper.ITEM_DEPT_RULE);
    setName("Item is in Dept");
  }
  
  public boolean test(ICommerceObject obj) {
    // System.out.println("testing ItemDeptRule");
    boolean isPass = false;
    IOrderItem item = getItem(obj);
    
    if (item != null) {
      String department = item.getDepartment();
      Iterator<String> i = getDepartmentList().iterator();
      
      switch (getValueComparator()) {
        case RuleHelper.EQUALS: {
          while (i.hasNext()) {
            String dept = (String) i.next();
            if (RuleHelper.compare(department, dept, RuleHelper.EQUALS)) return true;
          }
          break;
        }
        case RuleHelper.NOTEQUAL: {
          isPass = true;
          while (i.hasNext()) {
            String dept = (String) i.next();
            // if any departments match, then the rule fails
            if (RuleHelper.compare(department, dept, RuleHelper.EQUALS)) return false;
          }
          break;
        }
      }
      
    }
    // System.out.println("   pass? " + isPass);
    return isPass;
  }
  
  private List<String> getDepartmentList() {
    
    List<String> departmentList = new ArrayList<String>();
    try {
      StringTokenizer st = new StringTokenizer(getValue(), ",");
      while (st.hasMoreElements()) {
        String dept = ((String) st.nextElement()).trim();
        departmentList.add(dept);
      }
    } catch (Exception e) {}
    return departmentList;
  }
  
  public String getDisplayValue() {
    String not = "";
    if (getValueComparator() == RuleHelper.NOTEQUAL) not = "Not";
    return "Item(s) " + not + " in Department(s): " + getValue();
  }
}
