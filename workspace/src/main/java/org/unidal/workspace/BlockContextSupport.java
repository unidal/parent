package org.unidal.workspace;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.unidal.workspace.program.entity.Block;
import org.unidal.workspace.program.entity.Status;

public class BlockContextSupport implements BlockContext {
	private Block m_block;

	private StringBuilder m_out = new StringBuilder(1024);

	public BlockContextSupport(Block block) {
		m_block = block;
		m_block.setStatus(new Status());
	}

	@Override
	public File getBaseDir() {
		Block current = m_block;

		while (current != null) {
			String baseDir = current.getDynamicAttribute("baseDir");

			if (baseDir != null) {
				return new File(baseDir);
			}

			current = current.getParent();
		}

		return null;
	}

	@Override
	public Block getBlock() {
		return m_block;
	}

	@Override
	public String getOutput() {
		return m_out.toString();
	}

	@Override
	public String getProject() {
		Block project = getProjectBlock();

		if (project != null) {
			return project.getId();
		} else {
			return null;
		}
	}

	private Block getProjectBlock() {
		Block current = m_block;

		while (current != null) {
			if (current.getId() != null) {
				return current;
			}

			current = current.getParent();
		}

		return null;
	}

	private boolean hasError(Block block) {
		Status status = block.getStatus();

		if (status != null && (status.isError())) {
			return true;
		}

		for (Block ref : block.getDependOnRefs()) {
			if (hasError(ref)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void markAsError(String message) {
		Block current = m_block;

		while (true) {
			current.getStatus().setError(true);
			current.getStatus().setMessage(message);

			if (current.getId() != null) { // project level
				break;
			}

			current = current.getParent();
		}
	}

	@Override
	public void markAsIgnored() {
		m_block.getStatus().setIgnored(true);
	}

	@Override
	public void markAsSkipped() {
		m_block.getStatus().setSkipped(true);
	}

	@Override
	public void markAsUpdated() {
		Block project = getProjectBlock();

		if (project != null) {
			project.getStatus().setUpdated(true);
		}
	}

	public void out(String line) {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
		String message = String.format("[%s] [%s] %s", timestamp, getProject(), line);

		m_out.append(line).append("\r\n");
		System.out.println(message);
	}

	@Override
	public boolean shouldIgnore() {
		Block current = m_block;

		while (current != null) {
			if (current.isIgnored()) {
				return true;
			}

			current = current.getParent();
		}

		return false;
	}

	@Override
	public boolean shouldSkip() {
		Block current = m_block;

		while (current != null) {
			Status status = current.getStatus();

			if (status != null && (status.isSkipped() || status.isError())) {
				return true;
			}

			current = current.getParent();
		}

		Block project = getProjectBlock();

		if (project != null) {
			for (Block ref : project.getDependOnRefs()) {
				if (hasError(ref)) {
					return true;
				}
			}
		}

		return false;
	}
}
