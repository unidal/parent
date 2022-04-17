package org.unidal.workspace;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
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

      System.out.println(m_workspace);
   }

   @Override
   protected List<Runner> getChildren() {
      List<Runner> children = new ArrayList<Runner>();

      m_workspace.accept(new RunnersBuilder(children));
      return children;
   }

   private static class LeafRunner extends Runner {
      private String m_displayName;

      private String m_category;

      public LeafRunner(String category, String displayName) {
         m_category = category;
         m_displayName = displayName;
      }

      @Override
      public Description getDescription() {
         return Description.createTestDescription(m_category, m_displayName);
      }

      @Override
      public void run(RunNotifier notifier) {
         notifier.fireTestStarted(getDescription());

         try {
            run0();

            notifier.fireTestRunFinished(new Result());
         } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(getDescription(), e));
         } finally {
            notifier.fireTestFinished(getDescription());
         }
      }

      private void run0() {
         // TODO

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
      protected void runChild(Runner child, RunNotifier notifier) {
         child.run(notifier);
      }
   }

   private static class RunnersBuilder extends BaseVisitor {
      private List<Runner> m_children;

      public RunnersBuilder(List<Runner> children) {
         m_children = children;
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
                  Project p = workspace.findProject(dependency.getText());

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
         try {
            List<Runner> runners = new ArrayList<Runner>();

            runners.add(new LeafRunner(project.getName(), "git clone"));
            runners.add(new LeafRunner(project.getName(), "mvn initialize"));
            runners.add(new LeafRunner(project.getName(), "mvn install"));

            m_children.add(new NodeRunner(project.getName(), runners));
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      @Override
      public void visitWorkspace(Workspace workspace) {
         m_children.add(new LeafRunner("", "setup SSH keys"));

         List<Project> projects = getSortedProjects(workspace);

         for (Project project : projects) {
            System.out.println(project.getName() + ": " + project.getOrder());
            visitProject(project);
         }
      }
   }
}
