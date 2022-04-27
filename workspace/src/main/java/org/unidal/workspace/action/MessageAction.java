package org.unidal.workspace.action;

import java.util.List;

import org.unidal.workspace.Action;
import org.unidal.workspace.ActionContext;

/**
 * Display the messages to console.
 *
 * @author qmwu2000
 */
public class MessageAction implements Action {
   public void execute(ActionContext ctx, List<String> messages) throws Exception {
      for (String message : messages) {
         ctx.print(message);
      }
   }

   @Override
   public String getName() {
      return "message";
   }
}