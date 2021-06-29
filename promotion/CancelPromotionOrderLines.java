package com.nm.commerce.promotion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import atg.nucleus.GenericService;

import com.nm.cmos.data.CmosOrder;
import com.nm.utils.GenericLogger;

public class CancelPromotionOrderLines extends GenericService {
  
  protected GenericLogger genericLogger;
  private CancelGwp cancelGwp;
  private CancelGwpSelect cancelGwpSelect;
  private CancelPwp cancelPwp;
  private Date gwpCancelInstallDate;
  private Date pwpCancelInstallDate;
  public static final int GWPTYPE = 1;
  public static final int PWPTYPE = 2;
  
  public CancelPromotionOrderLines() {}
  
  public ArrayList<String> getPromoLinesToBeCancelled(CmosOrder cmosOrder, List<String> cancelledLineItems, int type) {
    ArrayList<String> promoCancelledLines = new ArrayList<String>();
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date cmosOrderDate;
    try {
      cmosOrderDate = sdf.parse(cmosOrder.order_header.order_date);
    } catch (ParseException e) {
      genericLogger.error(e.getMessage());
      return promoCancelledLines;
    }
    
    if (cmosOrderDate.after(getGwpCancelInstallDate()) && type == GWPTYPE) {
      // Add gwp line cancels
      List<String> gwpList = getCancelGwp().getGwpsToBeCancelled(cmosOrder, cancelledLineItems);
      if (gwpList != null && !gwpList.isEmpty()) promoCancelledLines.addAll(gwpList);
      
      // Add gwp select line cancels
      List<String> gwpSelectList = getCancelGwpSelect().getGwpSelectsToBeCancelled(cmosOrder, cancelledLineItems);
      if (gwpSelectList != null && !gwpSelectList.isEmpty()) promoCancelledLines.addAll(gwpSelectList);
    }
    
    if (cmosOrderDate.after(getPwpCancelInstallDate()) && type == PWPTYPE) {
      // Add pwp line cancels
      List<String> pwpList = getCancelPwp().getPwpsToBeCancelled(cmosOrder, cancelledLineItems);
      if (pwpList != null && !pwpList.isEmpty()) promoCancelledLines.addAll(pwpList);
    }
    
    return promoCancelledLines;
  }
  
  public CancelGwp getCancelGwp() {
    return cancelGwp;
  }
  
  public void setCancelGwp(CancelGwp cancelGwp) {
    this.cancelGwp = cancelGwp;
  }
  
  public CancelGwpSelect getCancelGwpSelect() {
    return cancelGwpSelect;
  }
  
  public void setCancelGwpSelect(CancelGwpSelect cancelGwpSelect) {
    this.cancelGwpSelect = cancelGwpSelect;
  }
  
  public CancelPwp getCancelPwp() {
    return cancelPwp;
  }
  
  public void setCancelPwp(CancelPwp cancelPwp) {
    this.cancelPwp = cancelPwp;
  }
  
  public GenericLogger getGenericLogger() {
    return genericLogger;
  }
  
  public void setGenericLogger(GenericLogger genericLogger) {
    this.genericLogger = genericLogger;
  }
  
  public Date getGwpCancelInstallDate() {
    return gwpCancelInstallDate;
  }
  
  public void setGwpCancelInstallDate(Date gwpCancelInstallDate) {
    this.gwpCancelInstallDate = gwpCancelInstallDate;
  }
  
  public Date getPwpCancelInstallDate() {
    return pwpCancelInstallDate;
  }
  
  public void setPwpCancelInstallDate(Date pwpCancelInstallDate) {
    this.pwpCancelInstallDate = pwpCancelInstallDate;
  }
}
