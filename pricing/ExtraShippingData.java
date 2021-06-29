/*
 * <ATGCOPYRIGHT> Copyright (C) 2001 Art Technology Group, Inc. All Rights Reserved. No use, copying or distribution ofthis work may be made except in accordance with a valid license agreement from
 * Art Technology Group. This notice must be included on all copies, modifications and derivatives of this work.
 * 
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR
 * ITS DERIVATIVES.
 * 
 * "Dynamo" is a trademark of Art Technology Group, Inc. </ATGCOPYRIGHT>
 */

package com.nm.commerce.pricing;

import java.util.*;

/**
 * A class which holds the information of the extra shipping address charges and non-continental charges. It also holds the abbreviation of non-continental states in a string array and priorty map for
 * charts. Easy to made changes through DCC.
 * 
 * @author Sunil Bindal
 */

public class ExtraShippingData {
  public ExtraShippingData() {}
  
  // Property Extra Shipping Charge: If order is shipped Next Day or Second Day to a shipping
  // address in Alaska, Hawaii or Puerto Rico, then this extra charge needs to be applied
  
  private double mExtraShippingCharge = 0.0;
  
  public double getExtraShippingCharge() {
    return mExtraShippingCharge;
  }
  
  public void setExtraShippingCharge(double pExtraShippingCharge) {
    mExtraShippingCharge = pExtraShippingCharge;
  }
  
  // Property Extra Shipping Charge States: a list of states to be charged extra for Next or Second Day shipping
  
  private String[] mExtraShippingChargeStates;
  
  public String[] getExtraShippingChargeStates() {
    return mExtraShippingChargeStates;
  }
  
  public void setExtraShippingChargeStates(String[] pExtraShippingChargeStates) {
    mExtraShippingChargeStates = pExtraShippingChargeStates;
  }
  
  // Property Non Continental US Address Charge: If order is shipped to any address
  // in Non Continental US then this charge need to be applied for every such address
  
  private double mExtraNonContinentalUSAddressCharge = 0.0;
  
  public double getExtraNonContinentalUSAddressCharge() {
    return mExtraNonContinentalUSAddressCharge;
  }
  
  public void setExtraNonContinentalUSAddressCharge(double pExtraNonContinentalUSAddressCharge) {
    mExtraNonContinentalUSAddressCharge = pExtraNonContinentalUSAddressCharge;
  }
  
  // Property Non Continental US States:a list of non continental states
  
  private String[] mNonContinentalUSStates;
  
  public String[] getNonContinentalUSStates() {
    return mNonContinentalUSStates;
  }
  
  public void setNonContinentalUSStates(String[] pNonContinentalUSStates) {
    mNonContinentalUSStates = pNonContinentalUSStates;
  }
  
  // Property Chart Priority: This hashmap maps the chart ids to the priorities
  
  private Properties mPriorityMap;
  
  public Properties getPriorityMap() {
    return mPriorityMap;
  }
  
  public void setPriorityMap(Properties pPriorityMap) {
    mPriorityMap = pPriorityMap;
  }
  
}
