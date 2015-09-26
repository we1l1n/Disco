/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;

/**
 * Builtin utterance with no semantics (see Disco.xml)
 */
public class Say extends Utterance {

   public static TaskClass CLASS;
   
   // for extensions
   public Say (Class<? extends Say> cls, Disco disco, Decomposition decomp, 
         String step, boolean repeat) { 
      super(cls, disco, decomp, step, repeat);
   }

   // for extensions
   public Say (Class<? extends Say> cls, Disco disco,
         Boolean external, String text) {
      super(cls, disco, external);
      if ( text != null ) eval("$this.text=$$value", text, "new Say");
   }

   // for TaskClass.newStep
   public Say (Disco disco, Decomposition decomp, String step, boolean repeat) { 
      this(Say.class, disco, decomp, step, repeat);
   }

   public Say (Disco disco, Boolean external, String text) {
      this(Say.class, disco, external, text);
   }

   public String getText () { return (String) getSlotValue("text"); }

   @Override
   public String formatTask () { 
      String format = getDisco().getFormat(this);
      if ( format != null) return formatTask(format, null);
      String text = getText(); 
      return text == null ? "..." :  
         // use decomposition and step name to identify this point in dialogue tree
         ((Disco) getType().getEngine()).getAlternative(
               getDecomposition()+"."+getName(),
               text, true);
   }

   @Override
   public String toHistoryString (boolean formatTask) {
      StringBuilder buffer = new StringBuilder(
            Utils.capitalize(formatTask ? formatTask() : format()));
      Utils.endSentence(buffer);
      buffer.insert(0, '\"').append('\"');
      buffer.insert(0, ' ');
      buffer.insert(0, engine.getProperty("says@word"));
      return buffer.toString();
   }
      
   // Say.User: utterance by user 
   //           (for convenience -- does not support other options!)

   public static class User extends Say {

      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public User (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(User.class, disco, decomp, step, repeat);
      }

      public User (Disco disco, String text) {
         super(User.class, disco, true, text);
      }
   }

   // Say.Agent: utterance by agent 
   //            (for convenience -- does not support other options!)

   public static class Agent extends Say {

      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Agent (Disco engine, Decomposition decomp, String step, boolean repeat) { 
         super(Agent.class, engine, decomp, step, repeat);
      }

      public Agent (Disco disco, String text) {
         super(Agent.class, disco, false, text);
      }
   }

   // Say.Expression: utterance with text computed by Javascript expression
   //                 (generated by D4g when {..} appears in text to say)

   public static class Expression extends Say {

      public static TaskClass CLASS;
      
      // for extensions
      public Expression (Class<? extends Expression> cls, Disco disco, Decomposition decomp, 
             String step, boolean repeat) { 
         super(cls, disco, decomp, step, repeat);
      }

      // for extensions
      public Expression (Class<? extends Expression> cls, Disco disco,
             Boolean external, String text) {
         super(cls, disco, external, text);
      }

      // for TaskClass.newStep
      public Expression (Disco engine, Decomposition decomp, String step, boolean repeat) { 
         this(Expression.class, engine, decomp, step, repeat);
      }

      public Expression (Disco disco, String text) {
         this(Expression.class, disco, false, text);
      }

      private String frozen; // stop evaluation when utterance occurs

      @Override
      public String formatTask () {
         String text = getText();
         if ( text != null ) {
            if ( frozen == null ) {
               text = (String) eval(text, "Say.Expression");
               if ( isOccurred() ) frozen = text;
            } else text = frozen;
         }
         return text == null ? "..." : text; 
      }

      // Say.Expression.Eval

      public static class Eval extends Expression {

         public static TaskClass CLASS;
         
         // for TaskClass.newStep
         public Eval (Disco engine, Decomposition decomp, String step, boolean repeat) { 
            super(Eval.class, engine, decomp, step, repeat);
         }

         public Eval (Disco disco, String text, String eval) {
            super(Eval.class, disco, false, text);
            if ( eval != null ) eval("$this.eval=$$value", eval, "new Say.Expression.Eval");
         }

         public String getEval () { return (String) getSlotValue("eval"); }

         @Override
         public boolean interpret (Plan contributes, boolean continuation) {
            boolean explained = super.interpret(contributes, continuation);
            if ( isOccurred() ) eval(getEval(), "Say.Expression.Eval");
            return explained;
         }
      } 
   }
   
   // Say.Eval: grounding script provided as input
   //           (avoids need to define new task type for one usage)

   public static class Eval extends Say {

      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Eval (Disco engine, Decomposition decomp, String step, boolean repeat) { 
         super(Eval.class, engine, decomp, step, repeat);
      }

      public Eval (Disco disco, String text, String eval) {
         super(Eval.class, disco, false, text);
         if ( eval != null ) eval("$this.eval=$$value", eval, "new Say.Eval");
      }

      public String getEval () { return (String) getSlotValue("eval"); }

      @Override
      public boolean interpret (Plan contributes, boolean continuation) {
         boolean explained = super.interpret(contributes, continuation);
         if ( isOccurred() ) eval(getEval(), "Say.Eval");
         return explained;
      }
   }
 
}
