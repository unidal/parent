/* THIS FILE WAS AUTO GENERATED BY codegen-maven-plugin, DO NOT EDIT IT */
package org.unidal.workspace.program.entity;

import java.util.ArrayList;
import java.util.List;

import org.unidal.workspace.program.BaseEntity;
import org.unidal.workspace.program.IVisitor;

public class Instrument extends BaseEntity<Instrument> {
   private String m_type;

   private String m_order;

   private List<String> m_properties = new ArrayList<String>();

   public Instrument() {
   }

   @Override
   public void accept(IVisitor visitor) {
      visitor.visitInstrument(this);
   }

   public Instrument addProperty(String property) {
      m_properties.add(property);
      return this;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Instrument) {
         Instrument _o = (Instrument) obj;

         if (!equals(getType(), _o.getType())) {
            return false;
         }

         if (!equals(getOrder(), _o.getOrder())) {
            return false;
         }

         if (!equals(getProperties(), _o.getProperties())) {
            return false;
         }


         return true;
      }

      return false;
   }

   public String getOrder() {
      return m_order;
   }

   public List<String> getProperties() {
      return m_properties;
   }

   public String getType() {
      return m_type;
   }

   @Override
   public int hashCode() {
      int hash = 0;

      hash = hash * 31 + (m_type == null ? 0 : m_type.hashCode());
      hash = hash * 31 + (m_order == null ? 0 : m_order.hashCode());
      for (String e : m_properties) {
         hash = hash * 31 + (e == null ? 0 :e.hashCode());
      }


      return hash;
   }

   @Override
   public void mergeAttributes(Instrument other) {
      if (other.getType() != null) {
         m_type = other.getType();
      }

      if (other.getOrder() != null) {
         m_order = other.getOrder();
      }
   }

   public Instrument setOrder(String order) {
      m_order = order;
      return this;
   }

   public Instrument setType(String type) {
      m_type = type;
      return this;
   }

   /********* Code Snippet Start *********/
   public Instrument withProperties(String... properties) {
   for (String property : properties) {
   m_properties.add(property);
   }

   return this;
   }

   /********* Code Snippet End *********/
}
