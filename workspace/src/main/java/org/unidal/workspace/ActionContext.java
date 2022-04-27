package org.unidal.workspace;

import java.io.File;

public interface ActionContext {
   public File getBaseDir();

   public String getCategory();

   public boolean hasError();

   public void print(String line);

   public void setBaseDir(File baseDir);

   public void setError(boolean error);
}