/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco;

import org.w3c.dom.Document;
import edu.wpi.cetask.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.Utterance;

import java.util.*;

/**
 * Class for managing a turn-based collaborative discourse interaction between
 * two actors. Note that this class is basically symmetric between the two
 * actors. Furthermore, multiple instances of this class involving overlapping
 * actors can be created (see {@link edu.wpi.disco.game.NWayInteraction}).
 * <p>
 * Interaction with the "environment" is typically handled by scripts associated
 * with tasks.
 * <p>
 * NB: Only methods in this class documented as <em>thread-safe</em> may be used in a
 * multi-threaded environment. Also see thread-safe methods of {@link Actor}.
 */
public class Interaction extends Thread {
  
   // ******* Thread-safe methods *********
   
   private Disco disco;
   
   /**
    * Thread-safe method to return Disco instance for this interaction.
    * NB: It is <em>not</em> thread-safe to call methods on this instance!
    */
   public Disco getDisco () { return disco; }

   private Console console; // optional console
   
   /**
    * Thread-safe method to return console, if any, associated with this interaction.
    */
   public Console getConsole () { return console; }
   
   /**
    * Thread-safe method to set console associated with this interaction.
    */
   public void setConsole (Console console) { this.console = console; }
   
   private final Actor system, external; 
   
   /**
    * Return the actor modeled as external=false in task model.
    * Thread-safe.
    */
   public Actor getSystem () { return system; }
   
   /**
    * Return the actor modeled as external=true in task model.
    * Thread-safe.
    */
   public Actor getExternal () { return external; }

   /**
    * Return other actor in this interaction (may return null).
    * Thread-safe.
    */
   public Actor getOther (Actor who) { return who == system ? external : system; }
      
   private boolean ok = true, guess = true;
   
   public Actor getActor (Task task) {
      return task.isUser() ? getExternal() : 
         task.isSystem() ? getSystem() : null; 
   }

   /**
    * Set default value for interaction@ok property.
    * Thread-safe.
    */
   public void setOk (boolean ok) { this.ok = ok; }

   /**
    * Return default value for interaction@ok property.
    * Thread-safe.
    */
   public boolean isOk () { return ok; }
   
   /**
    * Set default value for interaction@guess property.
    * Thread-safe.
    */
   public void setGuess (boolean guess) { this.guess = guess; }

   /**
    * Return default value for interaction@guess property.
    * Thread-safe.
    */
   public boolean isGuess () { return guess; }
   
   /**
    * Thread-safe method to get current discourse focus.
    * 
    * @see Disco#getFocus()
    * @see #getFocus(boolean)
    * @see #getFocusExhausted(boolean)
    */
   public synchronized Plan getFocus () { return disco.getFocus(); }
    
   /**
    * Thread-safe method to get current discourse focus, possibly ignoring Accept.
    * 
    * @see Disco#getFocus(boolean)
    * @see #getFocus()
    * @see #getFocusExhausted(boolean)
    */
   public synchronized Plan getFocus (boolean ignoreAccept) { return disco.getFocus(ignoreAccept); }
   
   /**
    * Thread-safe method to get current discourse focus ignoring exhausted plans and
    * possibly ignoring Accept.
    * 
    * @see Disco#getFocusExhausted(boolean)
    * @see #getFocus()
    * @see #getFocus(boolean)
    */
   public synchronized Plan getFocusExhausted (boolean ignoreAccept) { 
      return disco.getFocusExhausted(ignoreAccept);
   }
   
   /**  
    * Thread-safe method to test whether given plan is at toplevel in current tree.
    */
   public synchronized boolean isTop (Plan plan) {
      return disco.isTop(plan);
   }
   
   /**
    * Thread-safe method to push plan onto discourse stack (make it the current focus);
    * 
    * @see Disco#push(Plan)
    */
   public synchronized void push (Plan plan) { disco.push(plan); }
   
   /**
    * Thread-safe method to load specified task model.
    * 
    * @see Disco#load(String)
    */
   public synchronized TaskModel load (String from) { return disco.load(from); }

   /**
    * Thread-safe method to load specified task model.
    * 
    * @see Disco#load(String,Document,Properties,Properties)
    */
   public synchronized TaskModel load (String from, Document model, Properties properties, Properties translate) { 
      return disco.load(from, model, properties, translate); 
   }
   
   /**
    * Thread-safe method to evaluate given script.
    * 
    * @see Disco#eval(String,String)
    */
   public synchronized Object eval (String script, String where) { 
      return disco.eval(script, where); 
   }
   
   /**
    * Thread-safe method to get global JavaScript variable.
    * 
    * @see Disco#getGlobal(String)
    */
   public synchronized Object getGlobal (String variable) {
      return disco.getGlobal(variable);
   }
   
   /**
    * Thread-safe method to set global JavaScript variable.
    * 
    * @see Disco#setGlobal(String,Object)
    */
   public synchronized void setGlobal (String variable, Object value) {
      disco.setGlobal(variable, value);
   }
   
   /**
    * Thread-safe method to produce human-readable string for given utterance.
    * 
    * @see Disco#formatUtterance(Utterance,boolean)
    */
   public synchronized String format (Utterance utterance, boolean endSentence) {
      return disco.formatUtterance(utterance, endSentence);
   }
   
   /**
    * Thread-safe method to produce human-readable string for given agenda plugin item.
    * 
    * @see Disco#formatUtterance(Utterance,boolean)
    */
   public synchronized String format (Plugin.Item item, boolean endSentence) {
      return item.formatted != null ? item.formatted : 
         format((Utterance) item.task, endSentence);
   }
   
    /**
    * Thread-safe method to get specified property.
    * 
    * @see Disco#getProperty(String,boolean)
    */
   public synchronized boolean getProperty (String key, boolean defaultValue) {
      return disco.getProperty(key, defaultValue);
   }
   
   /**
    * Thread-safe method to find task class with given id.
    * 
    * @see Disco#getTaskClass(String)
    */
   public synchronized TaskClass getTaskClass (String id) { 
      return disco.getTaskClass(id); 
   }
   
   /**
    * Thread-safe method to add new toplevel task. 
    * 
    * @see Disco#addTop(String)
    */
   public synchronized Plan addTop (String id) { return disco.addTop(id); }
 
   /**
    * Thread-safe method to notify interaction that given <em>primitive</em>
    * task has occurred. Typically used in dialogue loop.
    * 
    * @param external true if performed by user, false if by system
    * @param occurrence task that has occurred
    * @param contributes plan to which this task contributes, or null
    * 
    * @see #doneUtterance(Utterance,Plan,String)
    */
   public synchronized void done (boolean external, Task occurrence, Plan contributes) {
      doneSilent(external, occurrence, contributes);
      if ( console != null ) console.done(occurrence);
   }   
   
   /**
    * Variant of {@link #done(boolean,Task,Plan)}, used in {@link
    * #choose(List,int,String)}, to notify interaction that given
    * utterance has occurred. Thread-safe.
    * 
    * @param formatted corresponding formatted string, or null
    */
   public synchronized void doneUtterance (Utterance utterance, Plan contributes,
                                          String formatted) {
      disco.putUtterance(utterance, formatted);
      done(true, utterance, contributes);
   }
   
   /**
    * Variant of {@link #done(boolean,Task,Plan)}, used in
    * {@link edu.wpi.disco.game.NWayInteraction}, that does not print to
    * console. Thread-safe.
    */
   public synchronized Plan doneSilent (boolean external, Task occurrence, Plan contributes) {
      responded = true;
      return disco.done(external, occurrence, contributes);
   }
   
   /**
    * Thread-safe method to call when world state has changed due to reasons
    * other than actions of system or user.
    */
   public synchronized void tick () { disco.tick(); }
   
   /**
    * Thread-safe method to choose given item number (starting with 1) in 
    * given list of plugin items for utterances and execute it.
    */
   public void choose (List<Plugin.Item> items, int i, String formatted) {
      if ( i > 0 && i <= items.size() ) {
         Plugin.Item item = items.get(i-1);
         // make sure history shows same alternative as menu selection
         doneUtterance((Utterance) item.task, item.contributes, formatted);
      } else throw new IndexOutOfBoundsException();
   }
   
   // ******* End of thread-safe methods ***********
   
   private boolean responded;  // see doTurn
 
   private boolean externalFloor; // true iff external has floor
   
   /**
    * Return the actor who currently has the turn (may be null ).
    */
   public Actor getFloor () { return externalFloor ? external : system; }
   
   // NB all read/write to discourse state on this single thread

   protected boolean running;
   
   @Override
   public void run () {
      if ( (system == null || external == null) && console == null )
         throw new IllegalStateException("Need console for missing user or agent!");
      boolean first = true;
      // this is the turn-taking loop
      while (running) {
         try { if ( !doTurn(first) ) break; }
         catch (Throwable e) {
            if ( console != null ) { 
               console.exception(e); // will throw if debug
               console.respond(this);
               externalFloor = false;
            } else throw e;
         }
         first = false;
      }
      cleanup();
   }

   /**
    * Perform one turn of interaction.
    * 
    * @param first true if this is first turn
    * @return true iff interaction should continue running
    */
   public boolean doTurn (boolean first) {
      Actor floor = getFloor();
      responded = false;
      boolean ok = disco.getProperty("interaction@ok", this.ok);
      if ( floor != null ) 
         floor.respond(this, ok, disco.getProperty("interaction@guess", guess));
      if ( !running ) return false;
      if ( console != null && (first || responded || (!ok && !externalFloor)) ) 
         console.respond(this);
      externalFloor = !externalFloor;
      return true;
   }
   
   /**
    * Create an interaction and associated discourse model between given actors.
    * Note this creates a new instance of Disco for this interaction.
    * 
    * @see #Interaction(Actor,Actor,String,boolean,Disco,String)
    */
   public Interaction (Actor system, Actor external) {
      this(system, external, null, false, null, "edu.wpi.disco.Interaction");
   }
   
   /**
    * Create an interaction with a console for debugging.  Note this creates a new
    * instance of Disco for this interaction.
    *
    * @see #Interaction(Actor,Actor,String,boolean,Disco,String)
    */
   public Interaction (Actor system, Actor external, String from) {
      this(system, external, from, true, null, "edu.wpi.disco.Interaction");
   }
 
   /**
    * Constructor to support extension of Disco class.
    * 
    * @param system actor which is modeled as external=false in task model 
    * @param external actor which is modeled as external=true in task model
    * @param from url or filename from which to read console commands (for testing),
    *        or null (ignored if console false)
    * @param console flag to control whether a console is provided
    * @param disco associated instance of Disco (or extension)
    * @param title for interaction thread 
    */
   protected Interaction (Actor system, Actor external, String from, boolean console, Disco disco, String title) {
      super(title);
      this.system = system;
      this.external = external;
      this.disco = disco == null ? new Disco(this) : disco;
      if ( system != null ) system.init(this.disco); 
      if ( external != null ) external.init(this.disco); 
      setUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler() {
               @Override
               public void uncaughtException (Thread t, Throwable e) {
                  Interaction.this.cleanup();
                  t.getThreadGroup().uncaughtException(t, e);
               }
            });
      if ( console ) {
         this.console = new Console(from, this); 
         this.console.init(this.disco);
      }
   }
   
   /**
    * Start the interaction with specified actor's turn.  Runs on separate thread.
    * 
    * @param externalFloor true iff user gets floor first.
    */
   public void start (boolean externalFloor) {
      this.externalFloor = externalFloor;
      running = true;
      start();
   }

   /**
    * Clear any discourse state information stored in this interaction.  
    */
   public void clear () { 
      disco.clear();
      system.clear(this); 
      external.clear(this); 
   }
   
   /**
    * Completely replace the instance of Disco used by this interaction.
    */
   public void reset () {
      disco = new Disco(this);
   }
   
   /**
    * Stop this interaction thread.
    */
   public void exit () { 
      running = false; 
      interrupt(); // in case blocked 
   }
   
   /**
    * Method to cleanup (free resources, etc.) when this interaction thread exits.
    * Calls cleanup on two actors.
    */
   public void cleanup () {
      disco.clear();
      if ( system != null ) system.cleanup(this); 
      if ( external!= null ) external.cleanup(this);
      if ( console != null ) console.cleanup();
   }
}