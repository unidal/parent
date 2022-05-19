package org.unidal.workspace.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.unidal.workspace.Action;
import org.unidal.workspace.BlockContext;

/**
 * Execute shell command line via Java process invocation.
 *
 * @author qmwu2000
 */
public class CommandAction implements Action {
	private boolean m_dryRun = false;

	public void execute(BlockContext ctx, List<String> args) throws Exception {
		showCommand(ctx, args);

		if (m_dryRun) {
			return;
		}

		long start = System.currentTimeMillis();
		Process process = new ProcessBuilder(args).directory(ctx.getBaseDir()).redirectErrorStream(true).start();

		try {
			waitFor(ctx, process);
		} catch (Exception e) {
			throw e;
		} finally {
			long ms = (System.currentTimeMillis() - start);

			ctx.out(String.format("Done in %s ms", ms));
			process.destroy();
		}
	}

	@Override
	public String getName() {
		return "command";
	}

	private void showCommand(BlockContext ctx, List<String> args) {
		StringBuilder sb = new StringBuilder(256);

		sb.append("$ ");

		for (String arg : args) {
			sb.append(arg).append(' ');
		}

		ctx.out(sb.toString());
	}

	private void waitFor(BlockContext ctx, Process process) throws IOException, InterruptedException {
		BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
		boolean eof = false;

		while (!eof) {
			while (!eof && in.ready()) {
				String line = in.readLine();

				if (line == null) {
					eof = true;
					in.close();
					break;
				} else {
					ctx.out(line);
				}
			}

			try {
				int exitCode = process.exitValue();

				if (exitCode != 0) {
					throw new RuntimeException("Process exited with code " + exitCode);
				} else {
					return;
				}
			} catch (IllegalThreadStateException e) {
				// ignore it
			}

			TimeUnit.MILLISECONDS.sleep(500);
		}
	}
}