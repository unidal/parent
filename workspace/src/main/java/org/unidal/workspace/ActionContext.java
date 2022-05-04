package org.unidal.workspace;

import java.io.File;
import java.util.List;

public interface ActionContext {
   public File getBaseDir();

   public String getCategory();

   public ActionContext getParent();

   public boolean isAnyDependencyFailed(List<String> ids);

   public boolean isMarkedAsError();

   public boolean isMarkedAsIgnored();

   public void markAsError(String id);

   public void markAsIgnored();

   public void print(String line);
}