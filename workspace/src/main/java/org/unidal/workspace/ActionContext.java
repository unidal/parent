package org.unidal.workspace;

import java.io.File;
import java.util.List;

public interface ActionContext {
	public File getBaseDir();

	public ActionContext getParent();

	public String getProject();

	public boolean isAnyDependencyFailed(List<String> ids);

	public boolean isAnyDependencyUpdated(List<String> ids);

	public void markAsError();

	public void markAsIgnored();

	public void markAsSkipped();

	public void markAsUpdated();

	public void print(String line);

	public boolean shouldIgnored();

	public boolean shouldSkipped();
}