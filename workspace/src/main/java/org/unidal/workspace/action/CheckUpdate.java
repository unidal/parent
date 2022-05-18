package org.unidal.workspace.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.unidal.workspace.Action;
import org.unidal.workspace.BlockContext;

/**
 * Check if any update there since last successful build.
 *
 * @author qmwu2000
 */
public class CheckUpdate implements Action {
	public void execute(BlockContext ctx, List<String> args) throws Exception {
		String type = args.get(0);
		File file = new File(ctx.getBaseDir(), "build.txt");
		List<String> lines = file.exists() ? Files.readAllLines(file.toPath()) : Collections.emptyList();

		if ("start".equals(type)) {
			String commitId = getLastCommitId(ctx);

			if (lines.size() == 2) {
				String first = lines.get(0);
				String second = lines.get(1);

				if (first.equals(commitId) && second.equals("OK")) {
					ctx.markAsSkipped();
					return;
				}
			}

			ctx.out("Source update found.");
			ctx.markAsUpdated();
			Files.write(file.toPath(), commitId.getBytes());
		} else if ("end".equals(type)) {
			if (ctx.shouldSkip()) {
				return;
			}

			if (lines.size() == 1) {
				String commitId = lines.get(0);

				Files.write(file.toPath(), (commitId + "\r\nOK").getBytes());
			}
		}
	}

	private String getLastCommitId(BlockContext ctx) throws IOException, InterruptedException {
		String[] args = { "git", "rev-parse", "--short", "HEAD" };
		Process process = new ProcessBuilder(args).directory(ctx.getBaseDir()).redirectErrorStream(true).start();
		List<String> lines = waitFor(process);

		if (lines.size() > 0) {
			String first = lines.get(0);

			return first;
		}

		return null;
	}

	@Override
	public String getName() {
		return "checkUpdate";
	}

	private List<String> waitFor(Process process) throws IOException, InterruptedException {
		List<String> lines = new ArrayList<>();
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
					lines.add(line);
				}
			}

			try {
				int exitCode = process.exitValue();

				if (exitCode != 0) {
					throw new RuntimeException("Process exited with code " + exitCode);
				} else {
					break;
				}
			} catch (IllegalThreadStateException e) {
				// ignore it
			}

			TimeUnit.MILLISECONDS.sleep(500);
		}

		return lines;
	}
}