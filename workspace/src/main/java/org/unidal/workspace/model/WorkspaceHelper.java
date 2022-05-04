/* THIS FILE WAS AUTO GENERATED BY codegen-maven-plugin, DO NOT EDIT IT */
package org.unidal.workspace.model;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.unidal.workspace.model.entity.Workspace;
import org.unidal.workspace.model.transform.DefaultXmlBuilder;
import org.unidal.workspace.model.transform.DefaultXmlParser;

import org.xml.sax.InputSource;

public class WorkspaceHelper {
   public static String asXml(IEntity<?> entity) {
      return new DefaultXmlBuilder().build(entity);
   }

   public static <T extends IEntity<?>> T fromXml(Class<T> entityType, InputStream in) throws IOException {
      return new DefaultXmlParser().parse(entityType, new InputSource(withoutBom(in)));
   }

   public static <T extends IEntity<?>> T fromXml(Class<T> entityType, String xml) throws IOException {
      return new DefaultXmlParser().parse(entityType, new InputSource(new StringReader(xml)));
   }

   public static Workspace fromXml(InputStream in) throws IOException {
      return fromXml(Workspace.class, in);
   }

   public static Workspace fromXml(String xml) throws IOException {
      return fromXml(Workspace.class, xml);
   }

   /**
    * removes Byte Order Mark(BOM) at the head of windows UTF-8 file.
    */
   private static InputStream withoutBom(InputStream in) throws IOException {
      if (!(in instanceof BufferedInputStream)) {
         in = new BufferedInputStream(in);
      }

      in.mark(3);

      /** UTF-8, with BOM **/
      if (in.read() != 0xEF || in.read() != 0xBB || in.read() != 0xBF) {
         in.reset();
      }

      return in;
   }
}
