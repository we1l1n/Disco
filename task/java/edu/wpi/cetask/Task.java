/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import edu.wpi.cetask.ScriptEngineWrapper.Compiled;
import edu.wpi.cetask.TaskClass.Slot;

import java.text.DateFormat;
import java.util.*;
import javax.script.*;

/**
 * Representation for instances of a task class.
 */
public class Task extends Instance {
 
   protected Task (TaskClass type, TaskEngine engine) { 
      super(type, engine); 
      synchronized (bindings) {
         // create JavaScript instance object (no initial slot properties)
         // do not use Instance.eval below because Decomposition.Step includes
         // call to updateBindings, but during constructor, steps are not yet added
         bindings.put("$this", engine.newObject());
         type.updateBindings(this); // extension
      }
   }
   
   @Override
   public TaskClass getType () { return (TaskClass) super.getType(); }

   @Override
   public boolean equals (Object object) {
      if ( this == object ) return true;
      if ( !(object instanceof Task) ) return false;
      Task task = (Task) object;
      if ( !task.getType().equals(type) ) return false;
      synchronized (bindings) {
         try {
            bindings.put("$$value", task.bindings.get("$this")); 
            return (Boolean) super.evalCondition(equals, compiledEquals, "equals");
         } finally { bindings.remove("$$value"); }
      }
   }

   @Override 
   public int hashCode () {
      return type.hashCode() ^
         eval(hashCode, compiledHashCode, "hashCode").hashCode();
   }
   
   // see Javascript function definitions in default.js
   final static String hashCode = "edu.wpi.cetask_helper.hashCode($this)",
                       cloneThis = "edu.wpi.cetask_helper.clone($this);",
                       cloneSlot = "$this[$$value] = edu.wpi.cetask_helper.clone($this[$$value]);",
                       equals = "edu.wpi.cetask_helper.equals($this,$$value)";

   static Compiled compiledEquals, compiledHashCode, compiledCloneThis, compiledCloneSlot;
   
   /**
    * Return value of the named slot.<br>
    * <br>
    * Note that this method returns a Java object, but the <em>real</em>
    * values are stored in JavaScript and must be converted to Java objects when
    * returned by this method. Refer to Chapter 12 of O'Reilly JavaScript guide
    * for details of conversion, but note following subtleties here:
    * <ul>
    * <li> JavaScript objects and arrays (i.e., everything except for numbers,
    * strings and booleans) convert to Java objects of "opaque" type. These
    * values can be passed back to JavaScript, but have an unpublished API that
    * is not intended for use by Java programs.
    * <li> <em>Both</em> null and undefined in JavaScript convert to null
    * </ul>
    * Since there is no attribute for "optional" slots in CEA-2018, all input
    * slot values must be defined before executing a task and all output slots
    * will be defined after execution. However, a null value can be used unless
    * a pre- or postcondition specifies otherwise.
    * 
    * @see #isDefinedSlot(String)
    * @see #deleteSlotValue(String)
    * @see #isDefinedInputs()
    * @see #isDefinedOutputs()
    */
   public Object getSlotValue (String name) { 
      checkIsSlot(name);
      if ( isScriptable(name) ) {
         Object value = engine.get(bindings.get("$this"), name);
         return engine.isDefined(value) ? value : null;
      } else return eval("$this."+name, "getSlotValue"); 
   }

   private void checkIsSlot (String name) {
      if ( !getType().isSlot(name) ) 
         throw new IllegalArgumentException(name+" is not slot of "+getType());
   }
   
   private Boolean getSlotValueBoolean (String name) {
      checkIsSlot(name);
      if ( isScriptable(name) ) {
         Object value = engine.get(bindings.get("$this"), name);
         return engine.isDefined(value) ? (Boolean) value : null;
      } else return evalCondition("$this."+name, "getSlotValue"); 
   }
   
   private boolean isScriptable (String name) {
      // for Java objects (or if could be Java object), must use LiveConnect
      Slot slot = getType().getSlot(name);
      return TaskEngine.SCRIPTABLE && slot.getType() != null && slot.getJava() == null;
   }

   /**
    * Return the string obtained by calling <em>JavaScript</em> toString method 
    * on value of named slot.   Good for debugging output.
    * 
    * @see TaskEngine#toString(Object)
    */
   public String getSlotValueToString (String name) {
      return engine.toString(getSlotValue(name));
   }

   /**
    * Test whether slot value is defined. Note if getSlotValue() returns null,
    * isDefinedSlot() may return true <em>or</em> false.
    * 
    * @see #getSlotValue(String)
    * @see #deleteSlotValue(String)
    */
   public boolean isDefinedSlot (String name) {
      checkIsSlot(name);
      return TaskEngine.SCRIPTABLE ?
         engine.isDefined(engine.get(bindings.get("$this"), name)) 
         // note using !== (not ===) below b/c null==undefined in JavaScript
         : evalCondition("$this."+name+" !== undefined", "isDefinedSlot");
   }   
  
   // see Decomposition
   private boolean modified = true;
   boolean isModified () { return modified; }
   void setModified (boolean modified) { this.modified = modified; }
   
   /**
    * Set the value of named slot to given value.<br>
    * <br>
    * Note that the given Java object must be converted to a JavaScript object
    * for storage. Refer to Chapter 12 of O'Reilly JavaScript guide for details
    * of conversion, but note following subtleties here:
    * <ul>
    * <li> Java objects and arrays (i.e., everything but numbers, booleans and 
    * strings) convert to instances of JavaObject and JavaArray 
    * </ul>
    * 
    * @see #setSlotValue(String,Object,boolean)
    * @see #getSlotValue(String)
    * @see #setSlotValueScript(String,String,String)
    */
   public Object setSlotValue (String name, Object value) {
      setSlotValue(name, value, true);
      return value;
   }   
  
   /**
    * Set the value of named slot to given value.<br>
    * 
    * @see #setSlotValue(String,Object)
    */
   public void setSlotValue (String name, Object value, boolean check) { 
      checkIsSlot(name);
      checkCircular(name, value);
      String type = getType().getSlotType(name);
      if ( !check || checkSlotValue(name, type, value) ) {
         if ( engine.isScriptable(value) ) {
            modified = true;
            engine.put(bindings.get("$this"), name, value);
         } else 
            synchronized (bindings) {
               try {
                  bindings.put("$$value", value); // convert to JavaScript value
                  modified = true;
                  eval("$this."+name+" = $$value;", "setSlotValue"); 
               } finally { bindings.remove("$$value"); }
            }
      } else failCheck(name, value.toString(), "setSlotValue");
   }  
      
   protected void checkCircular (String name, Object value) {
      if ( value == this ) 
         // will cause infinite loop when printing
         throw new RuntimeException("Attempting to store pointer to self in slot "
               +name+" of "+this);
   }
   
   private boolean checkSlotValue (String name, String type, Object value) {
      if ( type == null || 
            ( type.equals("boolean") && value instanceof Boolean ) ||
            ( type.equals("string") && value instanceof String ) ||
            ( type.equals("number") && value instanceof Number ) )
         return true;
      Slot slot = getType().getSlot(name);
      if ( slot.isOptional() && value == null ) return true;
      if ( slot.getJava() != null ) return slot.getJava().isInstance(value);
      if ( engine.getScriptEngine() instanceof JintScriptEngine // for Unity only
            && Utils.startsWith(type, "Packages.") )
         try { return Class.forName(type.substring(9)).isInstance(value); }
         catch (ClassNotFoundException e) { return false; }
      synchronized (bindings) {
         try {
            bindings.put("$$value", value); // convert to JavaScript object
            return Utils.booleanValue(
                  eval( // workaround bug: "$$value instanceof Date" returns false
                        type.equals("Date") ? "$$value.getUTCDate !== undefined" :
                           ("$$value instanceof "+type),
                        "checkSlotValue"));
         } finally { bindings.remove("$$value"); }
      }
   }

   /**
    * Set the value of named slot to result of evaluating given JavaScript
    * expression. This method avoids issues of conversion from Java to
    * JavaScript objects.
    * 
    * @see #setSlotValue(String,Object)
    */
   public void setSlotValueScript (String name, String expression, String where) {
      // only evaluate expression once (in case of side effects)
      try {
         if ( !evalCondition(makeExpression("$this", getType(), name, expression, false), where) )
            failCheck(name, expression, where);
         else modified = true;
      } finally { bindings.remove("$$value"); }
   }

   void setSlotValueScript (String name, Compiled compiled, String where) {
      if ( !evalCondition(compiled, bindings, where) )
         failCheck(name, "compiled script", where);
      else modified = true;
   }
   
   static String makeExpression (String self, TaskClass type, String name, 
                                 String value, boolean onlyDefined) {
      StringBuilder buffer = new StringBuilder();
      buffer.append("$$value = ").append(value).append(';');
      if ( onlyDefined ) 
         buffer.append("if ( $$value == undefined ) $$value = true; else ");
      buffer.append("if ( ").append(checkExpression(type, name)).append(" ) {")
         // note using [ ] to protect keywords
         .append(self).append("['").append(name).append("']")
         .append(" = $$value; $$value = true; } else $$value = false; $$value");
      return buffer.toString();
   }
   
   void failCheck (String name, String expression, String where) {
      throw new IllegalArgumentException(
            "Type error: Cannot assign result of "+expression+" to "
            +getType().getSlotType(name)+" slot "+name
            +" of "+getType()+" in "+where);
   }
  
   private static String checkExpression (TaskClass task, String name) {
      String type = task.getSlotType(name);
      return type == null ? "true" :
          // allow any slot to be set to null (for "optional" inputs)
          ("$$value === null || "+ 
             ("boolean".equals(type) ? "typeof $$value == \"boolean\"" :
               "string".equals(type) ? "typeof $$value == \"string\"" :
                  "number".equals(type) ? "typeof $$value == \"number\"" :
                     // work around bug: "$$value instanceof Date" returns false
                     "Date".equals(type) ? "$$value.getUTCDate !== undefined" :
                        "$$value instanceof "+type));
   }
   
   /**
    * Similar to {@link #setSlotValueScript(String,String,String)} but with extra 
    * bindings to use for evaluation of expression (instead of task bindings)
    */
   void setSlotValueScript (String name, String expression, String where,
                            Bindings extra) {
      try {
         extra.put("$$this", bindings.get("$this"));
         if ( !evalCondition(expression, extra, where) )
            failCheck(name, expression, where);
         else  modified = true;
      } 
      finally { extra.remove("$$value"); extra.remove("$$this"); }
   }
   
   void setSlotValueScript (String name, Compiled compiled, String where,
                            Bindings extra) {
      try {
         extra.put("$$this", bindings.get("$this"));
         if ( !evalCondition(compiled, extra, where) )
            failCheck(name, "compiled script", where);
         else  modified = true;

      } 
      finally { extra.remove("$$value"); extra.remove("$$this"); }
   }
   
   /**
    * Make named slot undefined.
    * 
    * @see #isDefinedSlot(String)
    */
   public void deleteSlotValue (String name) {
      checkIsSlot(name);
      eval("delete $this."+name, "deleteSlotValue");
      getType().updateBindings(this);
      modified = true;
   }
   
   public Boolean isApplicable () {
      TaskClass type = getType();
      return evalCondition(type.precondition, type.compiledPrecondition, 
            type+" precondition");
   }
   
   private Boolean achieved;
   
   public void setAchieved (Boolean achieved) { 
      this.achieved = achieved;
      if ( achieved ) setSuccess(true);
      engine.clearLiveAchieved();
   }
   
   public Boolean isAchieved () {
      if ( achieved != null ) return achieved; // overrides condition
      if ( engine.containsAchieved(this) )  // check cached value
         return engine.isAchieved(this);
      TaskClass type = getType();
      if ( type.getPostcondition() == null ) return null;
      Boolean achieved;
      if ( !type.hasModifiedInputs() || !occurred() )
         achieved = evalCondition(type.postcondition, type.compiledPostcondition, 
               type+" postcondition");
      else {
         if ( clonedInputs == null ) // not checking each modified inputs
            throw new IllegalStateException("Modified inputs have not been cloned: "+this);
         synchronized (bindings) {
            Object old = bindings.get("$this");
            try {
               bindings.put("$this", clonedInputs);
               achieved = evalCondition(type.postcondition, type.compiledPostcondition, 
                     type+" postcondition");
            } finally { bindings.put("$this", old); } 
         }
      }
      return engine.setAchieved(this, achieved);// store cache
   }

   @Override
   protected Boolean evalCondition (String condition, Compiled compiled, String where) {
	   if ( clonedInputs == null ) return super.evalCondition(condition,  compiled, where);
       synchronized (bindings) {
          Object old = bindings.get("$this");
          try {
             bindings.put("$this", clonedInputs);
             return super.evalCondition(condition, compiled, where);
           } finally { bindings.put("$this", old); }
       }
   }			
 
   @Override
   public Object eval (String expression, String where) {
	   if  ( clonedInputs == null ) return super.eval(expression, where);
	   synchronized (bindings) {
	      Object old = bindings.get("$this");
		   try {
			   bindings.put("$this", clonedInputs);	
			   return super.eval(expression, where);	
		   } finally { bindings.put("$this", old); } 
	   }
   }	

   /**
    * Test whether all declared input slots (i.e., not including
    * "external") have defined values.
    * 
    * @see #getSlotValue(String)
    */
   public boolean isDefinedInputs() {
      for (String name : getType().getDeclaredInputNames())
         if ( !isDefinedSlot(name) ) return false;
      return true;
   }

   /**
    * Test whether all declared output slots (i.e., not including "success" and
    * "when") have defined values. 
    * 
    * @see #getSlotValue(String)
    */
   public boolean isDefinedOutputs() {
      for (String name : getType().getDeclaredOutputNames())
         if ( !isDefinedSlot(name) ) return false;
      return true;
   }

   /**
    * Test whether this task starts given plan.  (For extensions.)
    * 
    * @see Plan#isStarted()
    */
   public boolean isStarter (Plan plan) { return true; };
   
   /**
    * Test whether this task is instance of primitive type.
    */
   public boolean isPrimitive () { return getType().isPrimitive(); }
   
   /**
    * Test whether this task is instance of an internal type.
    */
   public boolean isInternal () { return getType().isInternal(); }
   
   // predefined slots
   
   /**
    * Get time when this task occurred, if it did.
    *
    * @return milliseconds or 0 (if not occurred)
    * @see #setWhen(long)
    */
   public long getWhen () {
      try { 
         return // for some reason, millis get converted to double first 
               engine.evalDouble("$this.when.getTime()", bindings, "getWhen").longValue(); 
      } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); }
   }

   /**
    * Set the time when this task occurred. Javascript instance of Date object
    * is created with given milliseconds.
    * 
    * @see #getWhen()
    */
   public void setWhen (long milliseconds) {
      setSlotValueScript("when", "new Date("+Long.toString(milliseconds)+")", 
            "setWhen");
   }
   
   /**
    * Test whether this task instance occurred.
    */
   public boolean occurred () { return isDefinedSlot("when"); }
      
   /**
    * Get the value of the predefined 'success' slot 
    * @return TRUE, FALSE or null
    */
   public Boolean getSuccess () { return getSlotValueBoolean("success");  }

   /**
    * Set the predefined 'success' output slot of this task.
    * 
    * @param success - note type boolean so converts to JavaScript boolean
    */
   public void setSuccess (boolean success) {
      setSlotValue("success", success);
      engine.clearLive();  // not clearLivedAchieved (infinite loop)
   }
   
   protected Boolean checkAchieved () {
      Boolean success = isAchieved();
      if ( success != null ) {
         // if condition is unknown, then respect existing value of 'success'
         // slot, since it may come from communication
         if ( Utils.isFalse(success) ) setSuccess(false);
         else if ( getType().isSufficient() ) setSuccess(true);
      }
      return getSuccess();
   }
   
   /**
    * Get the value of the predefined 'external' input slot.
    *  
    * @return TRUE, FALSE or null
    */
   public Boolean getExternal () {
      return (Boolean) getSlotValue("external"); 
   }
   
   /**
    * Set the predefined 'external' input slot of this <em>primitive</em> task.
    * 
    * @param external - note type boolean so converts to JavaScript boolean
    */
   public void setExternal (boolean external) {
      if ( !isPrimitive() ) 
         throw new UnsupportedOperationException("Cannot set external slot of non-primitive task: "+this);
      setSlotValue("external", external);
   }

   /**
    * Test whether this task <em>must</em> be executed by user.  It must be
    * be executed by the user iff the 'external' slot is true. 
    * Always returns false for non-primitive tasks.  
    *  <p>
    * Note that
    * both {@link #isUser()} and {@link #isSystem()} may return false, but
    * both may not return true.
    * 
    * @see #canUser()
    */
   public boolean isUser () {  return Utils.isTrue(getExternal()); }

   /**
    * Test whether this task <em>must</em> be executed by system. It must be
    * executed by system iff the 'external' slot is false. 
    * Always returns false for non-primitive tasks.
    * <p> 
    * Note that
    * both {@link #isUser()} and {@link #isSystem()} may return false, but 
    * both may not return true.
    * 
    * @see #canSystem()
    */
   public boolean isSystem () { return Utils.isFalse(getExternal()); } 

   /**
    * Test whether this task <em>can</em> be executed by user.  It can be
    * executed by user iff it is primitive and it is not true that it must 
    * be executed by system.  Always returns false for non-primitive tasks.  
    * <p>
    * Note that both {@link #canUser()} and {@link #canSystem()} 
    * may return true, but both may not return false.
    * 
    * @see #isUser()
    */
   public boolean canUser() { return !isSystem() && isPrimitive(); }
   
   /**
    * Test whether this task <em>can</em> be executed by system. It can be
    * executed by system iff it is primitive and it is not true that it must
    * be executed by user.  Always returns false for non-primitive tasks.    
    * <p> 
    * Note that both 
    * {@link #canUser()} and {@link #canSystem()} may return true, but both may 
    * not return false.
    * 
    * @see #isSystem()
    */
   public boolean canSystem() {  return !isUser() && isPrimitive(); }
   
   private boolean unexplained;
   
   /**
    * Test whether this task occurrence is explained (e.g., contributes) to
    * current discourse state.
    */
   public boolean isUnexplained () { return unexplained; }
   
   /**
    * Set whether this task occurrence is explained (e.g., contributes) to
    * current discourse state.@param unexplained
    */
   public void setUnexplained (boolean unexplained) { this.unexplained = unexplained; }

   /**
    * Return modifiable list of decomposition classes <em>applicable</em> to this task
    * and not rejected.
    * 
    * @see TaskClass#getDecompositions()
    */
   public List<DecompositionClass> getDecompositions () {
      List<DecompositionClass> decomps = new ArrayList<DecompositionClass>(
            getType().getDecompositions());
      for (Iterator<DecompositionClass> i = decomps.iterator(); i.hasNext();) {
         DecompositionClass next = i.next();
         // remove decompositions not applicable to or rejected for this task instance
         if ( rejected.contains(next) || Utils.isFalse(next.isApplicable(this)) )      
            i.remove();
      }
      return decomps; 
   }

   void updateBindings () {
      // 2018-ext bindings on task
      getType().updateBindings(this);
   }
   
   /**
    * Update engine state based on occurrence of this task.
    * 
    * @param contributes plan to which this occurrence has been found to contribute
    *                    or null if no such plan could be found
    * @param continuation true if contributes started before this task matched                   
    * @return false iff this task is unexplained
    */
   public boolean interpret (Plan contributes, boolean continuation) { 
      // continuation unused here but needed in Disco
      if ( contributes != null ) {
         contributes.match(this);
         return true;
      } else return false;
   }
   
   /**
    * Test whether given task instance matches this task instance. Two task
    * instances match (symmetric relationship) iff they are both instances of the
    * same type and the values of each corresponding slot are either equal or
    * one is undefined.
    * 
    * @see #copySlotValues(Task)
    */
   public boolean matches (Task goal) {
      if ( goal == this ) return true;
      if ( !getType().equals(goal.getType()) ) return false;
      for (String name : getType().getInputNames())
         if ( !matchesSlot(goal, name) ) return false;
      for (String name : getType().getOutputNames())
         if ( !matchesSlot(goal, name) ) return false;
      return true;
   }
   
   private boolean matchesSlot (Task goal, String name) {
      return !isDefinedSlot(name) || !goal.isDefinedSlot(name) 
        || Utils.equals(getSlotValue(name), goal.getSlotValue(name));
   }

   /**
    * Test whether this task either matches goal of given plan or could
    * be viewed as a part of the plan for given task, i.e., whether
    * given plan explains this task.  
    * <p>
    * This method is for use in extensions.  In CETask, it is equivalent 
    * to {@link #matches(Task)}.   
    */
   public boolean contributes (Plan plan) {
      return matches(plan.getGoal());
   }
   
   /**
    * Test whether this task can directly contribute to a task of given type.
    * (For extension to plan recognition.)
    */
   public boolean contributes (TaskClass type) {
      return getType() == type;
   }
   
   /**
    * Test whether this task can indirectly contribute to a task of given type.  Returns
    * false if {@link #contributes(TaskClass)} returns true.
    * (For extension to plan recognition.)
    */
   public boolean isPathFrom (TaskClass type) {
      return getType().isPathFrom(type);
   }
   
   /**
    * Copies the values of defined slots <em>from</em> given task to this task. Note this
    * side-effects this task, overwriting existing slot values.
    * 
    * @param from - task of same type
    * @return true if any slots of this task overwritten (were defined)
    * 
    * @see #matches(Task)
    */
   public boolean copySlotValues (Task from) {
      if ( !getType().equals(from.getType()) ) 
         throw new IllegalArgumentException("Cannot copy slot values from "+
               from.getType()+" to "+getType()); 
      boolean overwrite = false;
      for (String name : getType().getInputNames())
         overwrite = copySlotValue(from, name, name, true, false) || overwrite;
      for (String name : getType().getOutputNames())
         overwrite = copySlotValue(from, name, name, true, false) || overwrite;
      return overwrite;
   }
   
   /**
    * If named slot of given task is defined, copy value <em>from</em> 
    * slot name of given task to given slot of this task.
    *
    * @return true if slot of this task is overwritten (was defined)
    */
   public boolean copySlotValue (Task from, String fromSlot, String thisSlot) {
      return copySlotValue(from, fromSlot, thisSlot, true, true);
   }
   
   boolean copySlotValue (Task from, String fromSlot, String thisSlot, 
                          boolean onlyDefined, boolean check) {
      if ( TaskEngine.SCRIPTABLE ) {
         Object value = engine.get(from.bindings.get("$this"), fromSlot);
         checkCircular(thisSlot, value);
         if ( onlyDefined && !engine.isDefined(value) ) return false;
         if ( check ) checkSlotValue(thisSlot, getType().getSlotType(thisSlot), value);
         Object task = bindings.get("$this");
         Object thisValue = engine.get(task, thisSlot);
         boolean overwrite = engine.isDefined(thisValue);
         engine.put(task, thisSlot, value);
         modified = true;
         return overwrite;
      } else {
         if ( onlyDefined && !from.isDefinedSlot(fromSlot) ) return false;
         boolean overwrite = isDefinedSlot(thisSlot);
         setSlotValueScript(thisSlot, "$this."+fromSlot, "copySlotValue", 
               from.bindings);
         return overwrite;
      }
   }

   /**
    * Execute primitive <em>system</em> task. 
    * 
    * @see TaskEngine#done(Task)
    */
   void execute (Plan plan) {
      done(false); // before script executed
      eval(plan);
   }
   
   protected  void evalIf (Plan plan) { if ( isSystem() ) eval(plan); }
   
   protected void eval (Plan plan) {
      TaskClass type = getType();
      // clone and cache modified inputs before grounding script executed
      for (String name : type.getDeclaredInputNames())
         if ( type.getModifiedOutput(name) != null ) cloneInput(name);
      Script script = getScript();
      if ( script != null ) 
         synchronized (bindings) {
            try { 
               bindings.put("$plan", plan);
               script.eval(this);      
            } finally { bindings.remove("$plan"); }
         }
      // set outputs to modified inputs
      if ( type.getPostcondition() != null )
         for (String name : type.getDeclaredInputNames()) {
            String modified = type.getModifiedOutput(name);
            if ( modified != null ) 
               engine.put(clonedInputs, modified, getSlotValue(modified));
      }
   }
   
   public void done (boolean external) {
      synchronized (engine.synchronizer) {
         setExternal(external); 
         done();
      }
   }
   
   public void done () {
      modifiedOutputs();
      synchronized (engine.synchronizer) {
         setWhen(System.currentTimeMillis());
         engine.tick();
      }
   }

   private void modifiedOutputs () {
      TaskClass type = getType();
      for (String name : type.getDeclaredInputNames()){
         String modified = type.getModifiedOutput(name);
         if ( modified != null ) {
            Object input = getSlotValue(name);
            Object output = getSlotValue(modified);
            // propagate modified input object to output
            if ( output == null ) setSlotValue(modified, input); 
            else if ( output != input ) 
               throw new IllegalStateException("Output of modified input not identical "+name);
         }
      }
   }

   // copy of $this with cloned inputs (includes outputs)
   private Object clonedInputs;  
   
   /**
    * Clone and cache copy of named input <em>before</em> modification (for
    * use evaluating postconditions).  Note that for reported actions by
    * user, this method should be called before done method.
    */
   public void cloneInput (String name) {
      if ( getType().getModifiedOutput(name) == null )
         throw new IllegalArgumentException("Not a modified input: "+name);
      if ( clonedInputs == null ) 
         clonedInputs = eval(cloneThis, compiledCloneThis, "cloneInput");
      synchronized (bindings) {
         Object old = bindings.get("$this");
         try {
            bindings.put("$this", clonedInputs);
            bindings.put("$$value", name);
            eval(cloneSlot, compiledCloneSlot, "cloneInput");
         } finally { 
            bindings.put("$this", old); 
            bindings.remove("$$value"); 
         }
      }
   }
   
   /**
    * Return appropriate and applicable script to ground this primitive task, or null if none.
    */
   public Script getScript () {
      Script script = getType().getScript();
      return script == null ? null : 
         Utils.isFalse(script.isApplicable(this)) ? null : script;
   }
   
   public String getProperty (String key) { return getType().getProperty(key); }
   
   public Boolean getProperty (String key, Boolean defaultValue) { 
      return getType().getProperty(key, defaultValue);
   }
      
   protected String toPropertyString (Object value) {
      // property key should not depend on these settings
      boolean debug = TaskEngine.DEBUG, print = TaskEngine.PRINT_TASK,
            verbose = TaskEngine.VERBOSE; 
      try { 
         TaskEngine.DEBUG = TaskEngine.PRINT_TASK = TaskEngine.VERBOSE = false;
         return engine.toString(value); 
      } finally {
         TaskEngine.DEBUG = debug;
         TaskEngine.PRINT_TASK = print;
         TaskEngine.VERBOSE = verbose;
      }
   }
      
   public String getPropertySlot () { return null; }
   
   // following code is for Disco extension--not used in CE Task engine
   
   private Boolean should;
   
   /**
    * Tests whether this task should be executed (default null).  
    * 
    * @return true if task has been accepted, false if rejected, otherwise null 
    */
   public Boolean getShould () { return should; }

   /**
    * Sets belief as to whether this task should be executed (default null).
    * 
    * @see #getShould()
    */
   public void setShould (Boolean should) { this.should = should; }
   
   private List<DecompositionClass> rejected = Collections.emptyList();
   
   /**
    * Return list of decomposition classes rejected for this task.
    * 
    * @see #reject(DecompositionClass)
    */
   public List<DecompositionClass> getRejected () { return rejected; }
   
   /**
    * Add given decomposition class to list of rejects for this task.
    * 
    * @see #getRejected()
    */
   public void reject (DecompositionClass decomp) {
      if ( rejected.isEmpty() ) rejected = new ArrayList<DecompositionClass>();
      rejected.add(decomp);
   }

   // *******************************************************************
   //     All code below here is printing-related stuff 
   // *******************************************************************
   
   @Override
   public String toString () {
      if ( TaskEngine.VERBOSE ) return toStringVerbose();
      StringBuilder buffer = argListBuilder(getDeclaredSlotValues());
      String id = getType().getPropertyId();
      return buffer.length() == 0 ? id : buffer.insert(0, '(').insert(0, id).append(')').toString();
   }
   
   static private final Object modifiedOutput = new Object(),
         // both undefined and null in JavaScript get returned as null in Java
         undefined = new Object();
   
   public List<Object> getDeclaredSlotValues () {
      List<Object> list = new ArrayList<Object>();
      TaskClass type = getType();
      for (String name : type.getDeclaredInputNames())
         list.add(getSlotValueIf(name));
      for (String name : type.getDeclaredOutputNames())
         // suppress modified output objects, since must be identical to input
         list.add(type.getModifiedInput(name) == null ? getSlotValueIf(name) : modifiedOutput);
       return list;
   }

   private Object getSlotValueIf (String name) {
      return isDefinedSlot(name) ? getSlotValue(name) : undefined;
   }
   
   protected StringBuilder argListBuilder (List<Object> args) {
      for (int i = args.size(); i-- > 0;) { 
         Object arg = args.get(i);
         // trim undefined or modified output args from end
         if ( arg == undefined || arg == modifiedOutput ) args.remove(i);
         else break;
      }
      StringBuilder buffer = new StringBuilder();
      boolean first = true;
      for (Object arg : args) {
         if ( first ) first = false;
         else buffer.append(",");
         if ( arg != undefined || arg == modifiedOutput ) buffer.append(engine.toString(arg));
      }
      return buffer;
   }
   
   @Override
   protected String toStringVerbose () { 
      StringBuilder buffer = new StringBuilder(super.toStringVerbose()).append("={ ");
      boolean first = true;
      for (String name : getType().getInputNames()) 
         first = appendSlot(buffer, name, first);
      for (String name : getType().getOutputNames()) 
         if ( !name.equals("when") ) first = appendSlot(buffer, name, first);
      // handle 'when' specially (but not 'success')
      if ( TaskEngine.DEBUG && isDefinedSlot("when") ) {
         long start = engine.getStart();
         long when = getWhen();
         first = appendSlot(buffer, "when", 
               start == 0 ? 
                  DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                     .format(new Date(when)) : 
                  Long.toString((when-start)/100), // tenth second accuracy enough
               first);
      }
      return buffer.append(" }").toString();
   }

   private boolean appendSlot (StringBuilder buffer, String name, boolean first) {
      return isDefinedSlot(name) ? 
         appendSlot(buffer, name, getSlotValueToString(name), first) :
            first;
   }
   
   private boolean appendSlot (StringBuilder buffer, String name, String value, 
         boolean first) {
      if ( !first ) buffer.append(", "); else first = false;
      buffer.append(name).append(':').append(value);
      return first;
   }
   
   /**
    * Return compact human-readable string for this task. 
    */
   public final String format () { return engine.format(this); }
   
   /**
    * Return compact human-readable string for this task.  For overriding
    * by extensions of Task. 
    */
   public String formatTask () { 
      return formatTask(isPrimitive() ? "execute@word" : "achieve@word");
   }
   
   protected String formatTask (String key) { 
      return formatTask(engine.getFormat(this), key);
   }

   protected String formatTask (String format, String key) { 
      TaskClass type = getType();
      List<String> inputs = type.getDeclaredInputNames(),
         outputs = type.getDeclaredOutputNames();
      int inputsSize = inputs.size(), outputsSize = outputs.size();
      if ( format != null ) {
         Object[] slots = new Object[inputsSize+outputsSize];
         int i = 0;
         for (String name : inputs) slots[i++] = formatSlot(name);
         for (String name : outputs) slots[i++] = formatSlot(name);
         return String.format(format, slots);
      } 
      // default formatting
      String and = engine.getProperty("and@word");
      StringBuilder buffer = new StringBuilder();
      if ( key != null ) 
         buffer.append(engine.getProperty(key)).append(' ');
      buffer.append(type)
        .append(formatSlots(inputs, inputsSize, "on@word", and))
        .append(formatSlots(outputs, outputsSize, "producing@word", and));
      return buffer.toString();
   }
   
   private String formatSlot (String name) {
      return formatSlot(name, isDefinedSlot(name));
   }
   
   protected String formatSlot (String name, boolean defined) {
      if ( !defined ) return getType().formatSlot(name, false);      
      Object value = getSlotValue(name);
      return value instanceof Task ? ((Task) value).formatTask() :
         clonedInputs == null ? getSlotValueToString(name) :
            engine.toString(getClonedInputsSlotValue(name));
   }
   
   private Object getClonedInputsSlotValue (String name) {
      synchronized (bindings) {
         Object old = bindings.get("$this");
         try {
            bindings.put("$this", clonedInputs);
            return getSlotValue(name);
         } finally { bindings.put("$this", old); } 
      }
   }
   
   private StringBuilder formatSlots (List<String> slots, int size, 
                                     String key, String and) {
      StringBuilder buffer = new StringBuilder();
      boolean firstDefined = false;
      // need to process slots backwards to drop trailing undefineds
      for (int i = size; i-- > 0;) {
         String name = slots.get(i);
         boolean defined = isDefinedSlot(name);
         if ( defined || firstDefined ) {
            firstDefined = true;
            buffer.insert(0, formatSlot(name));
            if ( i > 0 ) 
               buffer.insert(0, ' ').insert(0, and).insert(0, ' '); 
         }
      }
      if ( buffer.length() > 0 )
         buffer.insert(0, ' ').insert(0, engine.getProperty(key)).insert(0, ' ');
      return buffer;
   }
   
   protected String getFormat () {
      String format = getProperty("@format"); 
      // allow empty string to undo format
      return format != null && format.length() > 0 ? format : null;
   }
   
}