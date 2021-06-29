package com.nm.commerce.promotion.rules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.order.IOrderItem;

public class ItemClassCodeRule extends ItemRule {
  
  public ItemClassCodeRule() {
    setType(RuleHelper.ITEM_CLASS_CODE_RULE);
    setName("Item has Class Code");
  }
  
  public boolean test(ICommerceObject obj) {
    boolean isPass = false;
    IOrderItem item = getItem(obj);
    
    if (item != null) {
      String classCode = item.getClassCode();
      Iterator<String> i = getClassCodeList().iterator();
      
      switch (getValueComparator()) {
        case RuleHelper.EQUALS: {
          while (i.hasNext()) {
            String value = (String) i.next();
            if (RuleHelper.compare(classCode, value, RuleHelper.EQUALS)) return true;
          }
          break;
        }
        case RuleHelper.NOTEQUAL: {
          isPass = true;
          while (i.hasNext()) {
            String value = (String) i.next();
            // if any classCode match, then the rule fails
            if (RuleHelper.compare(classCode, value, RuleHelper.EQUALS)) return false;
          }
          break;
        }
      }
      
    }
    return isPass;
  }
  
  private List<String> getClassCodeList() {
    
    List<String> classCodeList = new ArrayList<String>();
    try {
      StringTokenizer st = new StringTokenizer(getValue(), ",");
      while (st.hasMoreElements()) {
        String classCode = ((String) st.nextElement()).trim();
        classCodeList.add(classCode);
      }
    } catch (Exception e) {}
    return classCodeList;
  }
  
  public String getDisplayValue() {
    String have = " have ";
    if (getValueComparator() == RuleHelper.NOTEQUAL) have = " don't have ";
    return "Item(s) " + have + " class code(s) " + getValue();
  }
}
