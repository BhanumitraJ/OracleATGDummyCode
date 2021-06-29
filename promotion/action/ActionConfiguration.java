package com.nm.commerce.promotion.action;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.nm.commerce.promotion.IPromoAction;
import com.nm.utils.ExceptionUtil;

import atg.nucleus.GenericService;
import atg.process.ProcessExecutionContext;
import atg.process.action.Action;

public class ActionConfiguration extends GenericService {
  public boolean isScenarioDrivenPromotions() {
    return (scenarioDrivenPromotions);
  }
  
  public void setScenarioDrivenPromotions(final boolean scenarioDrivenPromotions) {
    this.scenarioDrivenPromotions = scenarioDrivenPromotions;
  }
  
  public Action[] getActionsForEvent(final ProcessExecutionContext procCtx) {
    final Object event = procCtx.getMessage();
    final String eventClassName = event.getClass().getName();
    Action[] actions = (Action[]) eventActionMap.get(eventClassName);
    if (null == actions) actions = new Action[0];
    
    return (actions);
  }
  
  public IPromoAction[] getPromoActionsForEvent(final String eventName) {
    final Action[] actions = (Action[]) eventActionMap.get(eventName);
    if (null == actions) return (new IPromoAction[0]);
    
    final IPromoAction[] promoActions = new IPromoAction[actions.length];
    for (int i = 0; i < promoActions.length; ++i) {
      promoActions[i] = (IPromoAction) actions[i];
    }
    
    return (promoActions);
  }
  
  public Map<String, String> getEventActionConfig() {
    return (eventActionConfig);
  }
  
  public void setEventActionConfig(final Map<String, String> eventActionConfig) {
    this.eventActionConfig = eventActionConfig;
    eventActionMap.clear();
    for (Iterator<String> i = eventActionConfig.keySet().iterator(); i.hasNext();) {
      final String eventClassName = i.next();
      final String actions = (String) eventActionConfig.get(eventClassName);
      final Action[] actionArray = buildActionArray(actions);
      eventActionMap.put(eventClassName, actionArray);
    }
  }
  
  private Action[] buildActionArray(final String actions) {
    final String[] actionClasses = actions.split(":");
    final Action[] actionArray = new Action[actionClasses.length];
    for (int i = 0; i < actionClasses.length; ++i) {
      try {
        final String className = actionClasses[i].trim();
        actionArray[i] = getActionClass(className);
      } catch (final Exception e) {
        final String msg = "Unable to instanitate action class: " + actionClasses[i];
        logError(msg, e);
        throw new RuntimeException(msg, e);
      }
      
      try {
        actionArray[i].initialize(null);
      } catch (final Exception e) {
        final String msg = "Error initializing action class: " + actionClasses[i] + ": " + ExceptionUtil.getExceptionInfo(e);
        logError(msg, e);
        throw new RuntimeException(msg, e);
      }
    }
    
    return (actionArray);
  }
  
  private Action getActionClass(final String className) {
    Action action = (Action) actionClasses.get(className);
    if (null == action) {
      try {
        final Object o = Class.forName(className).newInstance();
        if (!(o instanceof IPromoAction)) logWarning("Promo Action class " + className + " does not implement " + IPromoAction.class.getName());
        
        if (o instanceof Action) {
          action = (Action) o;
          actionClasses.put(className, action);
        } else {
          final StringBuffer msg = new StringBuffer("Class " + className + " does not extend " + Action.class.getName());
          Class<?> superClass = o.getClass().getSuperclass();
          while (null != superClass) {
            msg.append(", ").append(superClass.getName());
            superClass = superClass.getSuperclass();
          }
          
          if (isLoggingDebug()) {
        	  logDebug(msg.toString());
          }
          throw new RuntimeException(msg.toString());
        }
      } catch (final Exception e) {
        final String msg = "Unable to instanitate action class: " + className;
        if (isLoggingError()) {
        	logError(msg, e);
        }
        throw new RuntimeException(msg, e);
      }
    }
    
    return (action);
  }
  
  private boolean scenarioDrivenPromotions = true;
  private Map<String, String> eventActionConfig;
  private Map<String, Action[]> eventActionMap = new HashMap<String, Action[]>();
  private Map<String, Action> actionClasses = new HashMap<String, Action>();
}
