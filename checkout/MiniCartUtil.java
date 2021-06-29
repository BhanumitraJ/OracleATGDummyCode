package com.nm.commerce.checkout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.nm.ajax.checkout.utils.MiniCartUtils;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;

public class MiniCartUtil {
  private static MiniCartUtil INSTANCE; // avoid static initialization

  // private constructor enforces singleton behavior
  private MiniCartUtil() {}

  public static synchronized MiniCartUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new MiniCartUtil() : INSTANCE;
    return INSTANCE;
  }

  public List<NMCommerceItem> getSortedMiniCartProductList(final ShoppingCartHandler cart) {

    final NMOrderImpl order = cart.getNMOrder();
    final List<NMCommerceItem> ciList = order.getCommerceItems();
    int qtyAddedToCart = 0;
    List<NMCommerceItem> sortedList = null;
    // sorted commerce item list
    if (ciList != null) {
      qtyAddedToCart = ciList.size();
      sortedList = new ArrayList(ciList);
      final Comparator comparatorSortSequence = MiniCartUtils.CommerceItemDateComparator;
      try {
        Collections.sort(sortedList, comparatorSortSequence);
      } catch (final RuntimeException e) {
        e.printStackTrace();
      }
      final int howMany = CommonComponentHelper.getSystemSpecs().getMiniCartViewAllPos();
      if (howMany < sortedList.size()) {
        sortedList = sortedList.subList(0, howMany);
      }
    }
    return sortedList;
  }

  public boolean containsReplenishmentItems(final List sortedList) {
    boolean hasReplenishment = false;
    final Iterator it = sortedList.iterator();
    while (it.hasNext()) {

      final NMCommerceItem nmci = (NMCommerceItem) it.next();
      if ((nmci.getSelectedInterval() != null) && !nmci.getSelectedInterval().equals("")) {
        hasReplenishment = true;
      }
    }

    return hasReplenishment;

  }

  public static Comparator CommerceItemDateComparator = new Comparator() {
    @Override
    public int compare(final Object ciObj, final Object anotherCiObj) {
      final NMCommerceItem commerceItem = (NMCommerceItem) ciObj;
      final NMCommerceItem anotherCommerceItem = (NMCommerceItem) anotherCiObj;

      final Date CommerceItemDate1 = commerceItem.getCommerceItemDate();
      final Date CommerceItemDate2 = anotherCommerceItem.getCommerceItemDate();

      final String id1 = commerceItem.getId();
      final String id2 = anotherCommerceItem.getId();

      if ((CommerceItemDate1 == null) || (CommerceItemDate2 == null)) {
        int result = 0;
        if ((CommerceItemDate2 == null) && (CommerceItemDate1 == null)) {
          result = id2.compareTo(id1);
        } else if (CommerceItemDate2 == null) {
          result = -1;
        } else if (CommerceItemDate1 == null) {
          result = 1;
        }
        return result;
      } else {
        if (!CommerceItemDate1.equals(CommerceItemDate2)) {
          // primary sort
          return CommerceItemDate2.compareTo(CommerceItemDate1);
        } else {
          // secondary sort
          return id2.compareTo(id1);
        }
      }
    }

  };
}
