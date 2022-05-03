package org.unidal.workspace;

import java.io.File;

public interface ActionContext {
   public File getBaseDir();

   public String getCategory();

   public ActionContext getParent();

   public boolean isMarkedAsError();

   public boolean isMarkedAsIgnored();

   public void markAsError();

   public void markAsIgnored();

   public void print(String line);
}