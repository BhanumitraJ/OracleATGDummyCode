package com.nm.commerce;

import atg.commerce.order.*;
import atg.commerce.order.processor.*;
import atg.service.pipeline.*;
import java.util.*;

public class NMProcValidateCreditCard extends ProcValidateCreditCard {
  
  protected void validateCreditCardFields(CreditCard creditcard, PipelineResult pipelineresult, ResourceBundle resourcebundle) {
    // override the existing function to prevent erroneous validation
  }
  
}
