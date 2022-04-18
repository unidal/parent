/* THIS FILE WAS AUTO GENERATED BY codegen-maven-plugin, DO NOT EDIT IT */
package org.unidal.workspace.model.transform;

import static org.unidal.workspace.model.Constants.ATTR_ENABLED;
import static org.unidal.workspace.model.Constants.ATTR_FOR;
import static org.unidal.workspace.model.Constants.ATTR_NAME;
import static org.unidal.workspace.model.Constants.ELEMENT_GIT_URL;
import static org.unidal.workspace.model.Constants.ELEMENT_MVN_ARGS;
import static org.unidal.workspace.model.Constants.ENTITY_DEPEND_ON;
import static org.unidal.workspace.model.Constants.ENTITY_PROJECT;
import static org.unidal.workspace.model.Constants.ENTITY_WORKSPACE;

import java.lang.reflect.Array;
import java.util.Collection;

import org.unidal.workspace.model.IEntity;
import org.unidal.workspace.model.IVisitor;
import org.unidal.workspace.model.entity.Project;
import org.unidal.workspace.model.entity.Workspace;

public class DefaultXmlBuilder implements IVisitor {

   private IVisitor m_visitor = this;

   private int m_level;

   private StringBuilder m_sb;

   private boolean m_compact;

   public DefaultXmlBuilder() {
      this(false);
   }

   public DefaultXmlBuilder(boolean compact) {
      m_compact = compact;
   }

   public String build(IEntity<?> entity) {
      m_sb = new StringBuilder(8192);
      m_sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");

      entity.accept(m_visitor);
      return m_sb.toString();
   }

   protected void endTag(String name) {
      m_level--;

      indent();
      m_sb.append("</").append(name).append(">\r\n");
   }

   protected String escape(Object value) {
      return escape(value, false);
   }
   
   protected String escape(Object value, boolean text) {
      if (value == null) {
         return null;
      }

      String str = toString(value);
      int len = str.length();
      StringBuilder sb = new StringBuilder(len + 16);

      for (int i = 0; i < len; i++) {
         final char ch = str.charAt(i);

         switch (ch) {
         case '<':
            sb.append("&lt;");
            break;
         case '>':
            sb.append("&gt;");
            break;
         case '&':
            sb.append("&amp;");
            break;
         case '"':
            if (!text) {
               sb.append("&quot;");
               break;
            }
         default:
            sb.append(ch);
            break;
         }
      }

      return sb.toString();
   }
   
   protected void indent() {
      if (!m_compact) {
         for (int i = m_level - 1; i >= 0; i--) {
            m_sb.append("   ");
         }
      }
   }

   protected void startTag(String name) {
      startTag(name, false, null);
   }
   
   protected void startTag(String name, boolean closed, java.util.Map<String, String> dynamicAttributes, Object... nameValues) {
      startTag(name, null, closed, dynamicAttributes, nameValues);
   }

   protected void startTag(String name, java.util.Map<String, String> dynamicAttributes, Object... nameValues) {
      startTag(name, null, false, dynamicAttributes, nameValues);
   }

   protected void startTag(String name, Object text, boolean closed, java.util.Map<String, String> dynamicAttributes, Object... nameValues) {
      indent();

      m_sb.append('<').append(name);

      int len = nameValues.length;

      for (int i = 0; i + 1 < len; i += 2) {
         Object attrName = nameValues[i];
         Object attrValue = nameValues[i + 1];

         if (attrValue != null) {
            m_sb.append(' ').append(attrName).append("=\"").append(escape(attrValue)).append('"');
         }
      }

      if (dynamicAttributes != null) {
         for (java.util.Map.Entry<String, String> e : dynamicAttributes.entrySet()) {
            m_sb.append(' ').append(e.getKey()).append("=\"").append(escape(e.getValue())).append('"');
         }
      }

      if (text != null && closed) {
         m_sb.append('>');
         m_sb.append(escape(text, true));
         m_sb.append("</").append(name).append(">\r\n");
      } else {
         if (closed) {
            m_sb.append('/');
         } else {
            m_level++;
         }
   
         m_sb.append(">\r\n");
      }
   }

   @SuppressWarnings("unchecked")
   protected String toString(Object value) {
      if (value instanceof String) {
         return (String) value;
      } else if (value instanceof Collection) {
         Collection<Object> list = (Collection<Object>) value;
         StringBuilder sb = new StringBuilder(32);
         boolean first = true;

         for (Object item : list) {
            if (first) {
               first = false;
            } else {
               sb.append(',');
            }

            if (item != null) {
               sb.append(item);
            }
         }

         return sb.toString();
      } else if (value.getClass().isArray()) {
         int len = Array.getLength(value);
         StringBuilder sb = new StringBuilder(32);
         boolean first = true;

         for (int i = 0; i < len; i++) {
            Object item = Array.get(value, i);

            if (first) {
               first = false;
            } else {
               sb.append(',');
            }

            if (item != null) {
               sb.append(item);
            }
         }
		
         return sb.toString();
      }
 
      return String.valueOf(value);
   }

   protected void tagWithText(String name, String text, java.util.Map<String, String> attributes) {
      if (text == null) {
         return;
      }
      
      indent();

      m_sb.append('<').append(name);

      if (attributes != null) {
         for (java.util.Map.Entry<String, String> e : attributes.entrySet()) {
            m_sb.append(' ').append(e.getKey()).append("=\"").append(escape(e.getValue())).append('"');
         }
      }

      m_sb.append(">");
      m_sb.append(escape(text, true));
      m_sb.append("</").append(name).append(">\r\n");
   }

   protected void tagWithText(String name, String text, Object... nameValues) {
      if (text == null) {
         return;
      }
      
      indent();

      m_sb.append('<').append(name);

      int len = nameValues.length;

      for (int i = 0; i + 1 < len; i += 2) {
         Object attrName = nameValues[i];
         Object attrValue = nameValues[i + 1];

         if (attrValue != null) {
            m_sb.append(' ').append(attrName).append("=\"").append(escape(attrValue)).append('"');
         }
      }

      m_sb.append(">");
      m_sb.append(escape(text, true));
      m_sb.append("</").append(name).append(">\r\n");
   }

   protected void element(String name, String text, String defaultValue, boolean escape) {
      if (text == null || text.equals(defaultValue)) {
         return;
      }
      
      indent();
      
      m_sb.append('<').append(name).append(">");
      
      if (escape) {
         m_sb.append(escape(text, true));
      } else {
         m_sb.append("<![CDATA[").append(text).append("]]>");
      }
      
      m_sb.append("</").append(name).append(">\r\n");
   }

   @Override
   public void visitProject(Project project) {
      startTag(ENTITY_PROJECT, null, ATTR_NAME, project.getName(), ATTR_ENABLED, project.getEnabled());

      if (project.getText() != null) {
         m_sb.append(project.getText());
      }

      element(ELEMENT_GIT_URL, project.getGitUrl(), null,  true);

      element(ELEMENT_MVN_ARGS, project.getMvnArgs(), null,  true);

      if (!project.getDependOn().isEmpty()) {
         startTag(ENTITY_DEPEND_ON);

         for (Project project_ : project.getDependOn()) {
            project_.accept(m_visitor);
         }

         endTag(ENTITY_DEPEND_ON);
      }

      endTag(ENTITY_PROJECT);
   }

   @Override
   public void visitWorkspace(Workspace workspace) {
      startTag(ENTITY_WORKSPACE, null, ATTR_FOR, workspace.getFor());

      if (!workspace.getProjects().isEmpty()) {
         for (Project project_ : workspace.getProjects().values()) {
            project_.accept(m_visitor);
         }
      }

      endTag(ENTITY_WORKSPACE);
   }
}
