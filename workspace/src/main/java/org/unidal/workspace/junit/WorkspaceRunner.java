package org.unidal.workspace.junit;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.unidal.workspace.Action;
import org.unidal.workspace.BlockContext;
import org.unidal.workspace.BlockContextSupport;
import org.unidal.workspace.action.CheckSshKeys;
import org.unidal.workspace.action.CheckUpdate;
import org.unidal.workspace.action.CommandAction;
import org.unidal.workspace.action.MessageAction;
import org.unidal.workspace.model.WorkspaceHelper;
import org.unidal.workspace.model.entity.Project;
import org.unidal.workspace.model.entity.Workspace;
import org.unidal.workspace.model.transform.BaseVisitor;
import org.unidal.workspace.program.entity.Block;
import org.unidal.workspace.program.entity.Instrument;
import org.unidal.workspace.program.entity.Program;

/**
 * A personal workspace setup tool driven by JUnit test driver either in IDE or maven command line.
 * <p>
 * 
 * Following environments setup is required before the runner:
 * <p>
 * <li>JDK: short of Java Development Kit, such as Oracle <a href="https://www.oracle.com/java/technologies/downloads">JDK</a> or
 * Oracle <a href="http://jdk.java.net/">OpenJDK</a></li>
 * <li>jenv: <a href="https://www.jenv.be/">jEnv</a> is a command line tool to help you forget how to set the JAVA_HOME environment
 * variable</li>
 * <li>git: <a href="https://git-scm.com/">Git</a> is a free and open source distributed version control system</li>
 * <li>maven: <a href="http://maven.apache.org">Apache Maven</a> is a software project management and comprehension tool</li>
 * <li>docker: <a href="https://www.docker.com">Docker</a> is an open-source software project automating the deployment of
 * applications inside software containers. For Max OSX or Windows, docker desktop is suggested.</li>
 * <li>kubernetes: <a href="https://kubernetes.io">Kubernetes</a> is an open source container orchestration engine for automating
 * deployment, scaling, and management of containerized applications</li>
 * 
 * <p>
 * Usage:
 * 
 * <code><pre>
 * &#64;RunWith(WorkspaceRunner.class)
 * public class SetupWorkspace {
 * 
 * }
 * </pre></code>
 * 
 * @author qmwu2000
 */
public class WorkspaceRunner extends Suite {
	private static final String TARGET_DIRECTORY = "/Users/qmwu2000/.joya/repos";

	private static final String DEFAULT_JDK_VERSION = "1.7";

	private Program m_program = new Program();

	private File m_baseDir;

	private Map<String, Action> m_actions = new HashMap<>();

	public WorkspaceRunner(Class<?> klass) throws Exception {
		super(klass, Collections.<Runner> emptyList());

		m_baseDir = new File(TARGET_DIRECTORY);

		if (!m_baseDir.exists()) {
			m_baseDir.mkdirs();
		}

		InputStream in = getClass().getResourceAsStream("../workspace.xml");
		Workspace workspace = WorkspaceHelper.fromXml(in);

		workspace.accept(new WorkspaceInitializer());
		workspace.accept(new ProgramBuilder());

		register(new CommandAction());
		register(new MessageAction());
		register(new CheckSshKeys());
		register(new CheckUpdate());
	}

	@Override
	protected List<Runner> getChildren() {
		List<Runner> children = new ArrayList<Runner>();

		for (Block block : m_program.getBlocks()) {
			if (block.getBlocks().isEmpty()) {
				children.add(new LeafRunner(new BlockContextSupport(block)));
			} else {
				try {
					children.add(new NodeRunner(new BlockContextSupport(block)));
				} catch (InitializationError e) {
					e.printStackTrace();
				}
			}
		}

		return children;
	}

	private void register(Action action) {
		m_actions.put(action.getName(), action);
	}

	@Override
	public void run(RunNotifier notifier) {
		super.run(notifier);

		System.out.println(m_program);
	}

	private class ActionJob {
		private Action m_action;

		private List<String> m_args;

		public ActionJob(String name, List<String> args) {
			m_action = m_actions.get(name);
			m_args = args;

			if (m_action == null) {
				throw new RuntimeException(String.format("Instrument type(%s) is NOT defined! " + //
				      "Use custom action please!", name));
			}
		}

		public void run(BlockContext ctx) throws Exception {
			m_action.execute(ctx, m_args);
		}
	}

	private class LeafRunner extends Runner {
		private BlockContext m_ctx;

		private Block m_block;

		private Description m_description;

		public LeafRunner(BlockContext ctx) {
			m_ctx = ctx;
			m_block = ctx.getBlock();
			m_description = Description.createTestDescription(m_ctx.getProject(), m_block.getName());
		}

		private List<ActionJob> buildJobs() {
			List<ActionJob> jobs = new ArrayList<>();

			for (Instrument instrument : m_block.getInstruments()) {
				jobs.add(new ActionJob(instrument.getType(), instrument.getProperties()));
			}

			return jobs;
		}

		@Override
		public Description getDescription() {
			return m_description;
		}

		@Override
		public void run(RunNotifier notifier) {
			List<ActionJob> jobs = buildJobs();

			notifier.fireTestStarted(m_description);

			try {
				if (m_ctx.shouldIgnore()) {
					notifier.fireTestIgnored(m_description);
				} else if (m_ctx.shouldSkip()) {
					Failure failure = new Failure(m_description, new AssumptionViolatedException("SKIPPED"));

					notifier.fireTestAssumptionFailed(failure);
				} else {
					for (ActionJob job : jobs) {
						job.run(m_ctx);
					}
				}
			} catch (StoppedByUserException e) {
				throw e;
			} catch (Throwable e) {
				RuntimeException error = new RuntimeException(e.getMessage() + "\r\n" + m_ctx.getOutput());

				m_ctx.markAsError(e.getMessage());
				notifier.fireTestFailure(new Failure(m_description, error));
			} finally {
				notifier.fireTestFinished(m_description);
			}
		}
	}

	private class NodeRunner extends ParentRunner<Runner> {
		private BlockContext m_ctx;

		private Block m_block;

		private List<ActionJob> m_jobsBefore = new ArrayList<>();

		private List<ActionJob> m_jobsAfter = new ArrayList<>();

		private List<Runner> m_children = new ArrayList<>();

		public NodeRunner(BlockContext ctx) throws InitializationError {
			super(new TestClass(null));

			m_ctx = ctx;
			m_block = m_ctx.getBlock();

			for (Instrument instrument : m_block.getInstruments()) {
				String order = instrument.getOrder();

				if ("before".equals(order)) {
					m_jobsBefore.add(new ActionJob(instrument.getType(), instrument.getProperties()));
				} else if ("after".equals(order)) {
					m_jobsAfter.add(new ActionJob(instrument.getType(), instrument.getProperties()));
				} else {
					throw new RuntimeException(String.format("Unknown order(%s) of instrument: %s", order, instrument));
				}
			}

			for (Block child : m_block.getBlocks()) {
				if (child.getBlocks().isEmpty()) {
					m_children.add(new LeafRunner(new BlockContextSupport(child)));
				} else {
					m_children.add(new NodeRunner(new BlockContextSupport(child)));
				}
			}
		}

		@Override
		protected Description describeChild(Runner child) {
			return child.getDescription();
		}

		@Override
		protected List<Runner> getChildren() {
			return m_children;
		}

		@Override
		protected String getName() {
			return String.format("%s(%s)", m_ctx.getProject(), m_block.getName());
		}

		@Override
		public void run(RunNotifier notifier) {
			Description description = getDescription();

			if (m_ctx.shouldIgnore()) {
				super.run(notifier);
				notifier.fireTestIgnored(description);
			} else if (m_ctx.shouldSkip()) {
				Failure failure = new Failure(description, new AssumptionViolatedException("SKIPPED"));

				notifier.fireTestAssumptionFailed(failure);
				super.run(notifier);
			} else if (m_jobsBefore.size() + m_jobsAfter.size() > 0) {
				for (ActionJob job : m_jobsBefore) {
					try {
						job.run(m_ctx);
					} catch (Exception e) {
						m_ctx.markAsError(e.getMessage());

						notifier.fireTestFailure(new Failure(description, e));
					}
				}

				super.run(notifier);

				for (ActionJob job : m_jobsAfter) {
					try {
						job.run(m_ctx);
					} catch (Exception e) {
						m_ctx.markAsError(e.getMessage());

						notifier.fireTestFailure(new Failure(description, e));
					}
				}
			} else {
				super.run(notifier);
			}
		}

		@Override
		protected void runChild(Runner child, RunNotifier notifier) {
			child.run(notifier);
		}
	}

	private class ProgramBuilder extends BaseVisitor {
		private List<Project> getSortedProjects(Workspace workspace) {
			for (Project project : workspace.getProjects().values()) {
				setOrder(project, workspace);
			}

			List<Project> projects = new ArrayList<Project>(workspace.getProjects().values());

			Collections.sort(projects, new Comparator<Project>() {
				@Override
				public int compare(Project o1, Project o2) {
					return o1.getOrder() - o2.getOrder();
				}
			});

			return projects;
		}

		private void setOrder(Project project, Workspace workspace) {
			if (project.getOrder() == 0) {
				if (project.getDependOn().isEmpty()) {
					project.setOrder(1);
				} else {
					int order = 0;

					for (Project dependency : project.getDependOn()) {
						Project p = workspace.findProject(dependency.getName());

						if (p == null) {
							throw new IllegalStateException(String.format("Project(%s) is NOT found!", dependency.getName()));
						}

						setOrder(p, workspace);

						if (order <= p.getOrder()) {
							order = p.getOrder() + 1;
						}
					}

					project.setOrder(order);
				}
			}
		}

		@Override
		public void visitProject(Project project) {
			String name = project.getName();
			Block repo = m_program.findOrCreateBlock("repository: " + name);
			File projectBaseDir = new File(m_baseDir, name);

			repo.setId(name);
			repo.setDynamicAttribute("baseDir", projectBaseDir.getPath());

			for (Project p : project.getDependOn()) {
				String id = p.getName();
				Block ref = m_program.findBlockById(id);

				repo.addDependOn(id);
				repo.addDependOnRef(ref);
			}

			// git clone or pull
			{
				if (projectBaseDir.exists()) {
					Block step = repo.getChildBlock("update the git repository");
					Instrument inst = step.newCommand().withProperties("git", "pull");

					if (project.getGitPullArgs() != null) {
						inst.addProperty(project.getGitPullArgs());
					}
				} else {
					Block step = repo.getChildBlock("clone the git repository");
					String gitUrl = project.getGitUrl();
					Instrument inst = step.newCommand().withProperties("git", "clone", "--depth", "1", gitUrl, name);

					// override baseDir
					step.setDynamicAttribute("baseDir", projectBaseDir.getParent());

					if (project.getGitCloneArgs() != null) {
						inst.addProperty(project.getGitCloneArgs());
					}
				}
			}

			// code compile and test and install
			{
				Block codeAndTest = repo.getChildBlock("make code and run tests");

				codeAndTest.newAction("checkUpdate").setOrder("before").addProperty("start");
				codeAndTest.newAction("checkUpdate").setOrder("after").addProperty("end");

				// jdk version
				{
					Block step = codeAndTest.getChildBlock("set JDK version to " + project.getJdkVersion());

					step.newCommand().withProperties("jenv", "local", project.getJdkVersion());
					step.newCommand().withProperties("jenv", "version-name");
				}

				// mvn test
				{
					Block step = codeAndTest.getChildBlock("run the unit tests");
					Instrument inst = step.newCommand().withProperties("mvn", "test");

					if (project.getMvnTestArgs() != null) {
						inst.addProperty(project.getMvnTestArgs());
					}
				}

				// mvn install
				{
					Block step = codeAndTest.getChildBlock("clean install the artifacts");
					Instrument inst = step.newCommand().withProperties("mvn", "clean", "install", "-Dmaven.test.skip");

					if (project.getMvnInstallArgs() != null) {
						inst.addProperty(project.getMvnInstallArgs());
					}
				}

				// build docker image
				if ("war".equals(project.getType())) {
					Block step = codeAndTest.getChildBlock("build docker image");
					Instrument inst = step.newCommand().withProperties("docker", "build", "-t", project.getName(), ".");

					if (project.getDockerBuildArgs() != null) {
						inst.addProperty(project.getDockerBuildArgs());
					}
				}

				// deploy to kubernetes
				if ("war".equals(project.getType())) {
					Block step = codeAndTest.getChildBlock("deploy to kubernetes");
					Instrument inst = step.newCommand().withProperties("kubectl", "apply", "-f", "deploy.yaml");

					if (project.getKubectlApplyArgs() != null) {
						inst.addProperty(project.getKubectlApplyArgs());
					}
				}
			}
		}

		@Override
		public void visitWorkspace(Workspace workspace) {
			// check SSH keys
			{
				Block block = m_program.findOrCreateBlock("check SSH keys");

				block.newAction("checkSshKeys");
			}

			List<Project> projects = getSortedProjects(workspace);

			for (Project project : projects) {
				visitProject(project);
			}
		}
	}

	private static class WorkspaceInitializer extends BaseVisitor {
		private Workspace m_workspace;

		@Override
		public void visitProject(Project project) {
			if (project.getJdkVersion() == null) {
				project.setJdkVersion(DEFAULT_JDK_VERSION);
			}

			List<Project> links = new ArrayList<>();

			for (Project dependon : project.getDependOn()) {
				String name = dependon.getText();
				Project ref = m_workspace.findProject(name);

				if (ref != null) {
					ref.setText(name);
					links.add(ref);
				} else {
					String message = String.format("Depend-on(%s) of project(%s) is not defined!", name, project.getName());

					throw new IllegalStateException(message);
				}
			}

			project.getDependOn().clear();
			project.getDependOn().addAll(links);
		}

		@Override
		public void visitWorkspace(Workspace workspace) {
			m_workspace = workspace;

			super.visitWorkspace(workspace);
		}
	}
}
