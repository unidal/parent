/* THIS FILE WAS AUTO GENERATED BY codegen-maven-plugin, DO NOT EDIT IT */
package org.unidal.workspace.program.entity;

import static org.unidal.workspace.program.Constants.ATTR_NAME;
import static org.unidal.workspace.program.Constants.ENTITY_BLOCK;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.unidal.workspace.program.BaseEntity;
import org.unidal.workspace.program.IVisitor;

public class Block extends BaseEntity<Block> {
   private String m_name;

   private Boolean m_ignored;

   private String m_id;

   private List<String> m_dependOns = new ArrayList<String>();

   private List<Instrument> m_instruments = new ArrayList<Instrument>();

   private List<Block> m_blocks = new ArrayList<Block>();

   private Status m_status;

   private transient Block m_parent;

   private Map<String, String> m_dynamicAttributes = new LinkedHashMap<String, String>();

   public Block() {
   }

   public Block(String name) {
      m_name = name;
   }

   @Override
   public void accept(IVisitor visitor) {
      visitor.visitBlock(this);
   }

   public Block addBlock(Block block) {
      m_blocks.add(block);
      return this;
   }

   public Block addDependOn(String dependOn) {
      m_dependOns.add(dependOn);
      return this;
   }

   public Block addInstrument(Instrument instrument) {
      m_instruments.add(instrument);
      return this;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Block) {
         Block _o = (Block) obj;

         if (!equals(getName(), _o.getName())) {
            return false;
         }

         return true;
      }

      return false;
   }

   public Block findBlock(String name) {
      for (Block block : m_blocks) {
         if (!equals(block.getName(), name)) {
            continue;
         }

         return block;
      }

      return null;
   }

   public Block findOrCreateBlock(String name) {
      synchronized (m_blocks) {
         for (Block block : m_blocks) {
            if (!equals(block.getName(), name)) {
               continue;
            }

            return block;
         }

         Block block = new Block(name);

         m_blocks.add(block);
         return block;
      }
   }

   public String getDynamicAttribute(String name) {
      return m_dynamicAttributes.get(name);
   }

   public Map<String, String> getDynamicAttributes() {
      return m_dynamicAttributes;
   }

   public List<Block> getBlocks() {
      return m_blocks;
   }

   public List<String> getDependOns() {
      return m_dependOns;
   }

   public String getId() {
      return m_id;
   }

   public Boolean getIgnored() {
      return m_ignored;
   }

   public List<Instrument> getInstruments() {
      return m_instruments;
   }

   public String getName() {
      return m_name;
   }

   public Block getParent() {
      return m_parent;
   }

   public Status getStatus() {
      return m_status;
   }

   @Override
   public int hashCode() {
      int hash = 0;

      hash = hash * 31 + (m_name == null ? 0 : m_name.hashCode());

      return hash;
   }

   public boolean isIgnored() {
      return m_ignored != null && m_ignored.booleanValue();
   }

   @Override
   public void mergeAttributes(Block other) {
      assertAttributeEquals(other, ENTITY_BLOCK, ATTR_NAME, m_name, other.getName());

      for (Map.Entry<String, String> e : other.getDynamicAttributes().entrySet()) {
         m_dynamicAttributes.put(e.getKey(), e.getValue());
      }

      if (other.getIgnored() != null) {
         m_ignored = other.getIgnored();
      }

      if (other.getId() != null) {
         m_id = other.getId();
      }
   }

   public Block removeBlock(String name) {
      int len = m_blocks.size();

      for (int i = 0; i < len; i++) {
         Block block = m_blocks.get(i);

         if (!equals(block.getName(), name)) {
            continue;
         }

         return m_blocks.remove(i);
      }

      return null;
   }

   public Block setDynamicAttribute(String name, String value) {
      m_dynamicAttributes.put(name, value);
      return this;
   }

   public Block setId(String id) {
      m_id = id;
      return this;
   }

   public Block setIgnored(Boolean ignored) {
      m_ignored = ignored;
      return this;
   }

   public Block setName(String name) {
      m_name = name;
      return this;
   }

   public Block setParent(Block _parent) {
      m_parent = _parent;
      return this;
   }

   public Block setStatus(Status status) {
      m_status = status;
      return this;
   }

   /********* Code Snippet Start *********/
   public Block getChildBlock(String name) {
      Block child = findOrCreateBlock(name);

      child.setParent(this);
      return child;
   }

   public List<String> getProjectDependOns() {
      if (m_id != null) {
         return m_dependOns;
      } else {
         return m_parent.getDependOns();
      }
   }

   public Instrument newAction(String type) {
      Instrument instrument = new Instrument().setType(type);

      m_instruments.add(instrument);
      return instrument;
   }

   public Instrument newCommand() {
      Instrument instrument = new Instrument().setType("command");

      m_instruments.add(instrument);
      return instrument;
   }

   public Instrument newMessage() {
      Instrument instrument = new Instrument().setType("message");

      m_instruments.add(instrument);
      return instrument;
   }

   /********* Code Snippet End *********/
}
