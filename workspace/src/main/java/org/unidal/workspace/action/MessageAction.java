package org.unidal.workspace.action;

import java.util.List;

import org.unidal.workspace.Action;
import org.unidal.workspace.BlockContext;

/**
 * Display the messages to console.
 *
 * @author qmwu2000
 */
public class MessageAction implements Action {
   public void execute(BlockContext ctx, List<String> messages) throws Exception {
      for (String message : messages) {
         ctx.out(message);
      }
   }

   @Override
   public String getName() {
      return "message";
   }
}