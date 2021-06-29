package com.nm.commerce;

import java.util.Calendar;
import java.util.Date;

import atg.commerce.gifts.InvalidDateException;

/**
 * Validates a credit card's expiration date.
 */
public class CreditCardValidator {
  // we should factor this into the credit card object itself once we get a
  // true list of requirements and error conditions.
  
  public static void validateExpirationDate(Calendar expDate) throws InvalidDateException {
    Calendar minDate = Calendar.getInstance();
    Calendar maxDate = Calendar.getInstance();
    maxDate.set(Calendar.YEAR, minDate.get(Calendar.YEAR) + 10);
    if (expDate.before(minDate)) throw new InvalidDateException("The card expiration date has already passed.");
    if (expDate.after(maxDate)) throw new InvalidDateException("The year entered for the date exceeds the range allowed from the current year.");
  }
  
  public static void validateExpirationDate(Date expDate) throws InvalidDateException {
    Calendar cal = Calendar.getInstance();
    cal.setTime(expDate);
    CreditCardValidator.validateExpirationDate(cal);
  }
  
}
