/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;

/**
 * Base class for utterances. See edu.wpi.disco.lang package for extensions.  
 */
public abstract class Utterance extends Decomposition.Step {

   // for TaskClass.newStep
   protected Utterance (Class<? extends Utterance> cls, Disco disco,
                        Decomposition decomp, String name, boolean repeat) {
      super(disco.getTaskClass(cls.getName()), disco, decomp, name);
   }

   public Disco getDisco () { return (Disco) engine; }
   
   // for extensions
   protected Utterance (Class<? extends Utterance> cls, Disco disco, 
                        Boolean external) {
      this(cls, disco, null, null, false);
      if ( external != null ) setSlotValue("external", external);
   }

   @Override
   public boolean interpret (Plan contributes, boolean continuation) {
      // do not call super
      boolean explained = interpret(this, contributes, continuation);
      getDisco().getSegment().add(this);
      return explained;
   }

   // see Propose.Interpret
   // NB: this method does not add occurrence to segment!
   static boolean interpret (Utterance occurrence, Plan contributes, boolean continuation) {
      if ( contributes != null
            // relying on fact that no utterance type contributes to itself other 
            // than as a direct match
            && contributes.getType() == occurrence.getType() ) {
         contributes.match(occurrence); 
         occurrence.reconcileStack(contributes, continuation); 
         return true;
      } // else
      return false;
   }
   
   @Override
   // always evaluate utterance scripts for both user and agent 
   protected void evalIf (Plan plan) { eval(plan); }
   
   @Override
   public boolean isStarter (Plan plan) {
      // utterances are only starters if they are a step of decomp
      if ( !plan.isDecomposed() ) return false; 
      Decomposition decomp = plan.getDecomposition();
      if ( decomp != null ) {
         for (Plan step : decomp.getSteps())
            if ( step.getGoal() == this ) return true;
      }
      return false;
   }
   
   @Override
   protected Boolean checkAchieved () {
      // postconditions for utterances, such as proposals, only relevant for goals
      return occurred() ? null : super.checkAchieved();
   }

   protected void reconcileStack (Plan contributes, boolean continuation) {
      getDisco().reconcileStack(this, contributes, continuation);
   }
   
   public String toHistoryString (boolean formatTask) {
      StringBuilder buffer = new StringBuilder(formatTask ? formatTask() : format());
      buffer.insert(0, ' ');
      buffer.insert(0, engine.getProperty("says@word"));
      return buffer.toString();
   }
  
   @Override
   public final String toString () {
      if ( !occurred() ) return toStringUtterance();
      String string = getDisco().getToString(this);
      if ( string != null ) return string;
      string = toStringUtterance();
      getDisco().putToString(this, string);
      return string;
   }
   
   protected String toStringUtterance () { return super.toString(); }
   
   @Override
   public String formatTask () { return formatTask(null); }

   // TODO if can assume single threading, then can reuse a single StringBuilder
   
   // useful methods for formatTask
   
   protected StringBuilder appendSp (StringBuilder buffer, String string) {
      return buffer.append(string).append(' ');
   }
   
   protected StringBuilder appendKey (StringBuilder buffer, String key) {
      return buffer.append(getDisco().getProperty(key));
   }
   
   protected StringBuilder appendKeySp (StringBuilder buffer, String key) {
      return appendKey(buffer, key).append(' ');
   }

   protected StringBuilder appendSpKey (StringBuilder buffer, String key) {
      return buffer.append(' ').append(getDisco().getProperty(key));
   }
   
   protected String getWho (Boolean self, Boolean external) {
      return getDisco().getProperty(
            (self == null || external == null) ? "someone@word" :
               self == external ? "i@word" : "you@word");
   }
}