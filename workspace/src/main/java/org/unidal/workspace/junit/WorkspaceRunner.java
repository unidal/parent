package org.unidal.workspace.junit;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.unidal.workspace.Action;
import org.unidal.workspace.ActionContext;
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
 * <li>JDK: Oracle JDK or Open JDK</li>
 * <li>jenv: <a href="https://www.jenv.be/">jEnv</a> is a command line tool to help you forget how to set the JAVA_HOME environment
 * variable</li>
 * <li>git: <a href="https://git-scm.com/">Git</a> is a free and open source distributed version control system</li>
 * <li>maven: <a href="http://maven.apache.org">Apache Maven</a> is a software project management and comprehension tool</li>
 *
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
   private static final String TARGET_DIRECTORY = "/Users/qmwu2000/project/lab";

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
      workspace.accept(new ProgramBuilder(m_program));

      register(new CommandAction());
      register(new MessageAction());
      register(new CheckSshKeys());
      register(new CheckUpdate());
   }

   private ActionContext buildContext(ActionContext parent, Block block) {
      ActionJobContext ctx = new ActionJobContext(parent);

      String category = block.getDynamicAttribute("category");
      String baseDir = block.getDynamicAttribute("baseDir");

      if (category != null) {
         ctx.setCategory(category);
      }

      if (baseDir != null) {
         ctx.setBaseDir(new File(baseDir));
      }

      return ctx;
   }

   @Override
   protected List<Runner> getChildren() {
      List<Runner> children = new ArrayList<Runner>();
      ActionJobContext parent = new ActionJobContext(null) {
         @Override
         public ActionContext getParent() {
            return this;
         }
      };

      for (Block block : m_program.getBlocks()) {
         ActionContext ctx = buildContext(parent, block);

         if (block.getBlocks().isEmpty()) {
            children.add(new LeafRunner(ctx, block));
         } else {
            try {
               children.add(new NodeRunner(ctx, block));
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

   private class ActionJob {
      private Action m_action;

      private List<String> m_args;

      public ActionJob(String name, List<String> args) {
         m_action = m_actions.get(name);
         m_args = args;

         if (m_action == null) {
            throw new RuntimeException(String.format("Instrument type(%s) is NOT supported yet! " + //
                  "Write custom action please!", name));
         }
      }

      public boolean run(ActionContext ctx) throws Exception {
         if (ctx.isMarkedAsError()) {
            return false; // skip it
         }

         m_action.execute(ctx, m_args);
         return true;
      }
   }

   private static class ActionJobContext implements ActionContext {
      private ActionContext m_parent;

      private String m_category;

      private File m_baseDir;

      private boolean m_error;

      private boolean m_ignored;

      private Set<String> m_failedBlocks = new HashSet<>();

      public ActionJobContext(ActionContext parent) {
         m_parent = parent;
      }

      @Override
      public File getBaseDir() {
         if (m_baseDir != null) {
            return m_baseDir;
         } else if (m_parent != null) {
            return m_parent.getBaseDir();
         } else {
            return null;
         }
      }

      @Override
      public String getCategory() {
         if (m_category != null) {
            return m_category;
         } else if (m_parent != null) {
            return m_parent.getCategory();
         } else {
            return null;
         }
      }

      @Override
      public ActionContext getParent() {
         return m_parent;
      }

      @Override
      public boolean isAnyDependencyFailed(List<String> ids) {
         for (String id : ids) {
            if (m_failedBlocks.contains(id)) {
               return true;
            }
         }

         if (m_parent != null) {
            return m_parent.isAnyDependencyFailed(ids);
         }

         return false;
      }

      @Override
      public boolean isMarkedAsError() {
         return m_error || m_parent != null && m_parent.isMarkedAsError();
      }

      @Override
      public boolean isMarkedAsIgnored() {
         return m_ignored;
      }

      @Override
      public void markAsError(String id) {
         m_error = true;

         if (m_parent != null) {
            m_parent.markAsError(id);
         }

         if (id != null) {
            m_failedBlocks.add(id);
         }
      }

      @Override
      public void markAsIgnored() {
         m_ignored = true;
      }

      @Override
      public void print(String line) {
         String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
         String message = String.format("[%s] [%s] %s", timestamp, getCategory(), line);

         System.out.println(message);
      }

      public void setBaseDir(File baseDir) {
         m_baseDir = baseDir;
      }

      public void setCategory(String category) {
         m_category = category;
      }
   }

   private class LeafRunner extends Runner {
      private Block m_block;

      private ActionContext m_ctx;

      public LeafRunner(ActionContext ctx, Block block) {
         m_block = block;
         m_ctx = ctx;
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
         return Description.createTestDescription(m_ctx.getCategory(), m_block.getName());
      }

      @Override
      public void run(RunNotifier notifier) {
         List<ActionJob> jobs = buildJobs();

         notifier.fireTestStarted(getDescription());

         try {
            if (m_block.isIgnored()) {
               notifier.fireTestIgnored(getDescription());
            } else {
               for (ActionJob job : jobs) {
                  if (!job.run(m_ctx)) {
                     notifier.fireTestIgnored(getDescription());
                  }
               }
            }

            notifier.fireTestRunFinished(new Result());
         } catch (Throwable e) {
            m_ctx.getParent().markAsError(m_block.getId());
            notifier.fireTestFailure(new Failure(getDescription(), e));
         } finally {
            notifier.fireTestFinished(getDescription());
         }
      }
   }

   private class NodeRunner extends ParentRunner<Runner> {
      private ActionContext m_ctx;

      private Block m_block;

      private List<Runner> m_children = new ArrayList<>();

      public NodeRunner(ActionContext ctx, Block block) throws InitializationError {
         super(new TestClass(null));

         m_ctx = ctx;
         m_block = block;

         for (Block child : m_block.getBlocks()) {
            ActionContext cctx = buildContext(ctx, child);

            if (child.getBlocks().isEmpty()) {
               m_children.add(new LeafRunner(cctx, child));
            } else {
               m_children.add(new NodeRunner(cctx, child));
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
         return m_block.getName();
      }

      @Override
      public void run(RunNotifier notifier) {
         if (m_ctx.isAnyDependencyFailed(m_block.getDependOns())) {
            Failure failure = new Failure(getDescription(), new AssumptionViolatedException("SKIPPED"));

            notifier.fireTestAssumptionFailed(failure);
         } else {
            CountDownLatch latch = new CountDownLatch(m_children.size());

            notifier.addListener(new RunListener() {
               @Override
               public void testIgnored(Description description) throws Exception {
                  latch.countDown();
               }
            });

            super.run(notifier);

            if (latch.getCount() == 0) { // all children are ignored
               notifier.fireTestIgnored(getDescription());
            }
         }
      }

      @Override
      protected void runChild(Runner child, RunNotifier notifier) {
         if (m_ctx.isMarkedAsIgnored()) {
            notifier.fireTestIgnored(child.getDescription());
         } else if (m_ctx.isMarkedAsError()) {
            Failure failure = new Failure(child.getDescription(), new AssumptionViolatedException("SKIPPED"));

            notifier.fireTestAssumptionFailed(failure);
         } else {
            child.run(notifier);
         }
      }
   }

   private class ProgramBuilder extends BaseVisitor {
      private Program m_program;

      public ProgramBuilder(Program program) {
         m_program = program;
      }

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
         File baseDir = new File(m_baseDir, name);

         repo.setId(name);
         repo.setDynamicAttribute("category", name);
         repo.setDynamicAttribute("baseDir", baseDir.getPath());

         for (Project p : project.getDependOn()) {
            repo.addDependOn(p.getName());
         }

         // git clone
         {
            Block step = repo.findOrCreateBlock("clone the git repository");

            if (baseDir.exists()) {
               String message = String.format("Git repository(%s) is already existed.", name);

               step.newMessage().withProperties(message);
            } else {
               String gitUrl = project.getGitUrl();
               Instrument inst = step.newCommand().withProperties("git", "clone", "--depth", "1", gitUrl, name);

               step.setDynamicAttribute("baseDir", m_baseDir.getPath());

               if (project.getGitCloneArgs() != null) {
                  inst.addProperty(project.getGitCloneArgs());
               }
            }
         }

         // check update start
         {
            Block step = repo.findOrCreateBlock("check update start");

            step.newAction("checkUpdate").addProperty("start");
         }

         // jdk version
         {
            Block step = repo.findOrCreateBlock("set JDK version to " + project.getJdkVersion());

            step.newCommand().withProperties("jenv", "local", project.getJdkVersion());
            step.newCommand().withProperties("jenv", "version-name");
         }

         // mvn install
         {
            Block step = repo.findOrCreateBlock("clean install the artifacts");
            Instrument inst = step.newCommand().withProperties("mvn", "clean", "install", "-Dmaven.test.skip");

            if (project.getMvnInstallArgs() != null) {
               inst.addProperty(project.getMvnInstallArgs());
            }
         }

         // mvn test
         {
            Block step = repo.findOrCreateBlock("run the unit tests");

            Instrument inst = step.newCommand().withProperties("mvn", "test");

            if (project.getMvnTestArgs() != null) {
               inst.addProperty(project.getMvnTestArgs());
            }
         }

         // check update end
         {
            Block step = repo.findOrCreateBlock("check update end");

            step.newAction("checkUpdate").addProperty("end");
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

         for (Project dependency : project.getDependOn()) {
            Project link = m_workspace.findProject(dependency.getText());

            link.setText(dependency.getText());
            links.add(link);
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
