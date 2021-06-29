package com.nm.commerce.giftlist;

import atg.commerce.gifts.*;
import java.util.*;
import java.io.IOException;
import javax.servlet.ServletException;
import atg.commerce.CommerceException;
import atg.repository.*;
import atg.repository.rql.*;

public class NMGiftlistTools extends GiftlistTools {
  
  public Collection getGiftlists(RepositoryItem profileRep) {
    Collection giftlistsReturn = new ArrayList();
    try {
      Collection giftlists = super.getGiftlists(profileRep);
      
      if (giftlists == null) giftlists = new ArrayList();
      
      Iterator giftlistsIterator = giftlists.iterator();
      
      while (giftlistsIterator.hasNext()) {
        MutableRepositoryItem giftlistRI = (MutableRepositoryItem) giftlistsIterator.next();
        if (giftlistRI != null) giftlistsReturn.add(giftlistRI);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return giftlistsReturn;
  }
}
