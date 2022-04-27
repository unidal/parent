package org.unidal.workspace.action;

import java.io.File;
import java.util.List;

import org.unidal.workspace.Action;
import org.unidal.workspace.ActionContext;

/**
 * Check if the RSA security token exists for git operation.
 *
 * @author qmwu2000
 */
public class CheckSshKeys implements Action {
   public void execute(ActionContext ctx, List<String> args) throws Exception {
      String userDir = System.getProperty("user.home");
      File file = new File(userDir, ".ssh/id_rsa").getCanonicalFile();

      if (file.isFile() && file.length() > 0) {
         return; // passed
      } else {
         throw new RuntimeException(String.format("File(%s) is NOT found!", file));
      }
   }

   @Override
   public String getName() {
      return "checkSshKeys";
   }
}