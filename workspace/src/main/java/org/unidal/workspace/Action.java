package org.unidal.workspace;

import java.util.List;

public interface Action {
   /**
    * Action name.
    * 
    * @return action name
    */
   public String getName();

   /**
    * Execute the action.
    * 
    * @param ctx
    *           action context
    * @param args
    *           action arguments
    * @throws Exception
    *            thrown if any error occurs
    */
   public void execute(ActionContext ctx, List<String> args) throws Exception;
}