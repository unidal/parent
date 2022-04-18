package org.unidal.workspace;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import org.unidal.workspace.model.WorkspaceHelper;
import org.unidal.workspace.model.entity.Project;
import org.unidal.workspace.model.entity.Workspace;
import org.unidal.workspace.model.transform.BaseVisitor;

public class WorkspaceRunner extends Suite {
   private Workspace m_workspace;

   public WorkspaceRunner(Class<?> klass) throws Exception {
      super(klass, Collections.<Runner> emptyList());

      InputStream in = getClass().getResourceAsStream("workspace.xml");

      m_workspace = WorkspaceHelper.fromXml(in);
      m_workspace.accept(new ModelInitializer());
   }

   @Override
   protected List<Runner> getChildren() {
      List<Runner> children = new ArrayList<Runner>();

      m_workspace.accept(new RunnersBuilder(children, new JobBuilder()));
      return children;
   }

   private void print(Project project, String line) {
      String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
      String message = String.format("[%s] [%s] %s", timestamp, project.getName(), line);

      System.out.println(message);
   }

   private static interface Job {
      /**
       * Do the real work.
       * 
       * @return true means successful, false means skipped
       * 
       * @throws Exception
       *            thrown if any error happens
       */
      public boolean run() throws Exception;
   }

   private class JobBuilder {
      public Job buildGitClone(final Project project) {
         if (!project.getBaseDir().exists()) {
            String url = project.getGitUrl();

            return new JobForGit(project, "clone", "--progress", url, project.getName());
         } else {
            String message = String.format("Git repository(%s) is existed!", project.getName());

            return new JobForMessage(project, message);
         }
      }

      public Job buildMvnInstall(Project project) {
         return new JobForMaven(project, "install", "-Dmaven.test.skip");
      }

      public Job buildMvnValidate(Project project) {
         return new JobForMaven(project, "validate", "-q");
      }

      public Job checkSetupSSHKeys() {
         return new Job() {
            @Override
            public boolean run() throws Exception {
               String userDir = System.getProperty("user.home");
               File file = new File(userDir, ".ssh/id_rsa").getCanonicalFile();

               if (file.isFile() && file.length() > 0) {
                  return true; // passed
               }

               throw new RuntimeException(String.format("File(%s) is NOT found!", file));
            }
         };
      }
   }

   private class JobForGit extends JobSupport {
      private List<String> m_args = new ArrayList<>();

      public JobForGit(Project project, String... args) {
         super(project);

         m_args.add("git");

         for (String arg : args) {
            m_args.add(arg);
         }
      }

      @Override
      public boolean run() throws Exception {
         if (!checkPreconditions()) {
            return false; // skip it
         }

         printCommandLine(m_args);

         return execute(project().getBaseDir().getParentFile(), m_args);
      }
   }

   private class JobForMaven extends JobSupport {
      private List<String> m_args = new ArrayList<>();

      public JobForMaven(Project project, String... args) {
         super(project);
         m_args.add("mvn");

         for (String arg : args) {
            m_args.add(arg);
         }

         if (project.getMvnArgs() != null) {
            m_args.add(project.getMvnArgs());
         }
      }

      @Override
      public boolean run() throws Exception {
         if (!checkPreconditions()) {
            return false; // skip it
         }

         printCommandLine(m_args);

         return execute(project().getBaseDir(), m_args);
      }
   }

   private class JobForMessage extends JobSupport {
      private String m_message;

      public JobForMessage(Project project, String message) {
         super(project);
         m_message = message;
      }

      @Override
      public boolean run() throws Exception {
         if (!checkPreconditions()) {
            return false; // skip it
         }

         print(project(), m_message);

         return true;
      }
   }

   private abstract class JobSupport implements Job {
      private Project m_project;

      public JobSupport(Project project) {
         m_project = project;
      }

      protected boolean checkPreconditions() {
         if (m_project.isError()) {
            return false;
         }

         for (Project dependency : m_project.getDependOn()) {
            if (dependency.isError()) {
               m_project.setError(true);
               return false;
            }
         }

         return true;
      }

      protected boolean execute(File workDir, List<String> command) throws Exception {
         long start = System.currentTimeMillis();
         Process process = new ProcessBuilder(command).directory(workDir).redirectErrorStream(true).start();

         try {
            waitFor(process);

            return true;
         } catch (Exception e) {
            m_project.setError(true);
            throw e;
         } finally {
            long ms = (System.currentTimeMillis() - start);

            print(m_project, String.format("Done in %s ms", ms));
            process.destroy();
         }
      }

      protected void printCommandLine(List<String> args) {
         StringBuilder sb = new StringBuilder(256);

         sb.append("$ ");

         for (String part : args) {
            sb.append(part).append(' ');
         }

         print(m_project, sb.toString());
      }

      protected Project project() {
         return m_project;
      }

      private void waitFor(Process process) throws IOException, InterruptedException {
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
                  print(m_project, line);
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

   private static class LeafRunner extends Runner {
      private String m_displayName;

      private String m_category;

      private Job m_job;

      private boolean m_ignore;

      public LeafRunner(String category, String displayName, Job job) {
         this(category, displayName, job, false);
      }

      public LeafRunner(String category, String displayName, Job job, boolean ignore) {
         m_category = category;
         m_displayName = displayName;
         m_job = job;
         m_ignore = ignore;
      }

      @Override
      public Description getDescription() {
         return Description.createTestDescription(m_category, m_displayName);
      }

      @Override
      public void run(RunNotifier notifier) {
         notifier.fireTestStarted(getDescription());

         try {
            if (m_ignore || m_job != null && !m_job.run()) {
               notifier.fireTestIgnored(getDescription());
            }

            notifier.fireTestRunFinished(new Result());
         } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(getDescription(), e));
         } finally {
            notifier.fireTestFinished(getDescription());
         }
      }
   }

   private static class ModelInitializer extends BaseVisitor {
      private Workspace m_workspace;

      @Override
      public void visitProject(Project project) {
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

   private static class NodeRunner extends ParentRunner<Runner> {
      private String m_displayName;

      private List<Runner> m_children;

      public NodeRunner(String displayName, List<Runner> children) throws InitializationError {
         super(new TestClass(null));

         m_displayName = displayName;
         m_children = children;
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
         return m_displayName;
      }

      @Override
      public void run(RunNotifier notifier) {
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

      @Override
      protected void runChild(Runner child, RunNotifier notifier) {
         child.run(notifier);
      }
   }

   private static class RunnersBuilder extends BaseVisitor {
      private List<Runner> m_children;

      private JobBuilder m_builder;

      public RunnersBuilder(List<Runner> children, JobBuilder builder) {
         m_children = children;
         m_builder = builder;
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
         boolean ignore = !project.isEnabled();

         project.setBaseDir(new File("/Users/qmwu2000/project/lab", name)); // TODO

         try {
            List<Runner> runners = new ArrayList<Runner>();

            runners.add(new LeafRunner(name, "clone git repository", m_builder.buildGitClone(project), ignore));
            runners.add(new LeafRunner(name, "validate maven POM", m_builder.buildMvnValidate(project), ignore));
            runners.add(new LeafRunner(name, "build and install library", m_builder.buildMvnInstall(project), ignore));

            m_children.add(new NodeRunner("repository: " + name, runners));
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      @Override
      public void visitWorkspace(Workspace workspace) {
         m_children.add(new LeafRunner("", "check SSH keys", m_builder.checkSetupSSHKeys()));

         List<Project> projects = getSortedProjects(workspace);

         for (Project project : projects) {
            visitProject(project);
         }
      }
   }
}
