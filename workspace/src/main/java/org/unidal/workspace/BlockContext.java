package org.unidal.workspace;

import java.io.File;

import org.unidal.workspace.program.entity.Block;

public interface BlockContext {
	public File getBaseDir();

	public Block getBlock();

	public String getOutput();

	public String getProject();

	public void markAsError(String message);

	public void markAsIgnored();

	public void markAsSkipped();

	public void markAsUpdated();
	
	public void out(String message);
	
	public boolean shouldIgnore();

	public boolean shouldSkip();
}
