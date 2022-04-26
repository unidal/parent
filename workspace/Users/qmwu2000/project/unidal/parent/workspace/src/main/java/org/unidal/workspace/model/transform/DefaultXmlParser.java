/* THIS FILE WAS AUTO GENERATED BY codegen-maven-plugin, DO NOT EDIT IT */
package org.unidal.workspace.model.transform;

import static org.unidal.workspace.model.Constants.ELEMENT_GIT_CLONE_ARGS;
import static org.unidal.workspace.model.Constants.ELEMENT_GIT_URL;
import static org.unidal.workspace.model.Constants.ELEMENT_MVN_INSTALL_ARGS;
import static org.unidal.workspace.model.Constants.ELEMENT_MVN_TEST_ARGS;

import static org.unidal.workspace.model.Constants.ENTITY_PROJECT;
import static org.unidal.workspace.model.Constants.ENTITY_WORKSPACE;
import static org.unidal.workspace.model.Constants.ENTITY_DEPEND_ON;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.unidal.workspace.model.IEntity;
import org.unidal.workspace.model.entity.Project;
import org.unidal.workspace.model.entity.Workspace;

public class DefaultXmlParser extends DefaultHandler {

   private DefaultLinker m_linker = new DefaultLinker(true);

   private DefaultXmlMaker m_maker = new DefaultXmlMaker();

   private Stack<String> m_tags = new Stack<String>();

   private Stack<Object> m_objs = new Stack<Object>();

   private IEntity<?> m_root;

   private StringBuilder m_text = new StringBuilder(256);

   @SuppressWarnings("unchecked")
   public <T extends IEntity<?>> T parse(Class<T> entityType, InputSource input) throws IOException {
      try {
         SAXParserFactory factory = SAXParserFactory.newInstance();

         factory.setValidating(false);
         factory.setFeature("http://xml.org/sax/features/validation", false);
         factory.newSAXParser().parse(input, this);

         m_linker.finish();

         if (entityType.isAssignableFrom(m_root.getClass())) {
            return (T) m_root;
         } else {
            throw new IllegalArgumentException(String.format("Expected %s, but was %s", entityType, m_root.getClass()));
         }
      } catch (ParserConfigurationException e) {
         throw new IllegalStateException("Unable to get SAX Parser! " + e, e);
      } catch (SAXException e) {
         throw new IOException("Unable to parse XML! " + e, e);
      }
   }

   @SuppressWarnings("unchecked")
   protected <T> T convert(Class<T> type, String value, T defaultValue) {
      if (value == null || value.length() == 0) {
         return defaultValue;
      }

      if (type == Boolean.class) {
         return (T) Boolean.valueOf(value);
      } else if (type == Integer.class) {
         return (T) Integer.valueOf(value);
      } else if (type == Long.class) {
         return (T) Long.valueOf(value);
      } else if (type == Short.class) {
         return (T) Short.valueOf(value);
      } else if (type == Float.class) {
         return (T) Float.valueOf(value);
      } else if (type == Double.class) {
         return (T) Double.valueOf(value);
      } else if (type == Byte.class) {
         return (T) Byte.valueOf(value);
      } else if (type == Character.class) {
         return (T) (Character) value.charAt(0);
      } else {
         return (T) value;
      }
   }

   @Override
   public void characters(char[] ch, int start, int length) throws SAXException {
      m_text.append(ch, start, length);
   }

   @Override
   public void endElement(String uri, String localName, String qName) throws SAXException {
      if (uri == null || uri.length() == 0) {
         Object currentObj = m_objs.pop();
         String currentTag = m_tags.pop();

         if (currentObj instanceof Project) {
            Project project = (Project) currentObj;

            if (ELEMENT_GIT_URL.equals(currentTag)) {
               project.setGitUrl(getText());
            } else if (ELEMENT_GIT_CLONE_ARGS.equals(currentTag)) {
               project.setGitCloneArgs(getText());
            } else if (ELEMENT_MVN_INSTALL_ARGS.equals(currentTag)) {
               project.setMvnInstallArgs(getText());
            } else if (ELEMENT_MVN_TEST_ARGS.equals(currentTag)) {
               project.setMvnTestArgs(getText());
            } else {
               project.setText(getText());
            }
         }
      }

      m_text.setLength(0);
   }

   protected String getText() {
      return m_text.toString();
   }

   private void parseForProject(Project parentObj, String parentTag, String qName, Attributes attributes) throws SAXException {
      if (ELEMENT_GIT_URL.equals(qName) || ELEMENT_GIT_CLONE_ARGS.equals(qName) || ELEMENT_MVN_INSTALL_ARGS.equals(qName) || ELEMENT_MVN_TEST_ARGS.equals(qName) || ENTITY_DEPEND_ON.equals(qName)) {
         m_objs.push(parentObj);
      } else if (ENTITY_PROJECT.equals(qName)) {
         Project project_ = m_maker.buildProject(attributes);

         m_linker.onProject(parentObj, project_);
         m_objs.push(project_);
      } else {
         throw new SAXException(String.format("Element(%s) is not expected under project!", qName));
      }

      m_tags.push(qName);
   }

   private void parseForWorkspace(Workspace parentObj, String parentTag, String qName, Attributes attributes) throws SAXException {
      if (ENTITY_PROJECT.equals(qName)) {
         Project project_ = m_maker.buildProject(attributes);

         m_linker.onProject(parentObj, project_);
         m_objs.push(project_);
      } else {
         throw new SAXException(String.format("Element(%s) is not expected under workspace!", qName));
      }

      m_tags.push(qName);
   }

   private void parseRoot(String qName, Attributes attributes) throws SAXException {
      if (ENTITY_WORKSPACE.equals(qName)) {
         Workspace workspace = m_maker.buildWorkspace(attributes);

         m_root = workspace;
         m_objs.push(workspace);
         m_tags.push(qName);
      } else if (ENTITY_PROJECT.equals(qName)) {
         Project project = m_maker.buildProject(attributes);

         m_root = project;
         m_objs.push(project);
         m_tags.push(qName);
      } else {
         throw new SAXException("Unknown root element(" + qName + ") found!");
      }
   }

   @Override
   public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      if (uri == null || uri.length() == 0) {
         if (m_objs.isEmpty()) { // root
            parseRoot(qName, attributes);
         } else {
            Object parent = m_objs.peek();
            String tag = m_tags.peek();

            if (parent instanceof Workspace) {
               parseForWorkspace((Workspace) parent, tag, qName, attributes);
            } else if (parent instanceof Project) {
               parseForProject((Project) parent, tag, qName, attributes);
            } else {
               throw new RuntimeException(String.format("Unknown entity(%s) under %s!", qName, parent.getClass().getName()));
            }
         }

         m_text.setLength(0);
        } else {
         throw new SAXException(String.format("Namespace(%s) is not supported by %s.", uri, this.getClass().getName()));
      }
   }
}
