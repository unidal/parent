/* THIS FILE WAS AUTO GENERATED BY codegen-maven-plugin, DO NOT EDIT IT */
package org.unidal.workspace.model.entity;

import java.util.LinkedHashMap;
import java.util.Map;

import org.unidal.workspace.model.BaseEntity;
import org.unidal.workspace.model.IVisitor;

public class Workspace extends BaseEntity<Workspace> {
   private String m_for;

   private Map<String, Project> m_projects = new LinkedHashMap<String, Project>();

   public Workspace() {
   }

   @Override
   public void accept(IVisitor visitor) {
      visitor.visitWorkspace(this);
   }

   public Workspace addProject(Project project) {
      m_projects.put(project.getName(), project);
      return this;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Workspace) {
         Workspace _o = (Workspace) obj;

         if (!equals(getFor(), _o.getFor())) {
            return false;
         }

         if (!equals(getProjects(), _o.getProjects())) {
            return false;
         }


         return true;
      }

      return false;
   }

   public Project findProject(String name) {
      return m_projects.get(name);
   }

   public String getFor() {
      return m_for;
   }

   public Map<String, Project> getProjects() {
      return m_projects;
   }

   @Override
   public int hashCode() {
      int hash = 0;

      hash = hash * 31 + (m_for == null ? 0 : m_for.hashCode());
      hash = hash * 31 + (m_projects == null ? 0 : m_projects.hashCode());

      return hash;
   }

   @Override
   public void mergeAttributes(Workspace other) {
      if (other.getFor() != null) {
         m_for = other.getFor();
      }
   }

   public Project removeProject(String name) {
      return m_projects.remove(name);
   }

   public Workspace setFor(String _for) {
      m_for = _for;
      return this;
   }

}
