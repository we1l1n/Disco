package edu.wpi.car;

import java.io.PrintStream;
import java.util.*;

public class CarWorld {
   
   public static void main (String[] args) { // for testing
      CarWorld world = new CarWorld();
      Nut nut = world.MyCar.LFhub.studA.getNut();
      world.MyCar.LFhub.studA.setNut(null);
      world.LOOSE_NUTS.add(nut);
      world.print(System.out);
      if ( !world.equals(new CarWorld(world)) )
         throw new IllegalStateException("Copy is not equal to original!");
   }
   
   public final Car MyCar;
   
   public final Tire LFtire, RFtire, RRtire, LRtire;
   
   public final List<Nut> LOOSE_NUTS = new ArrayList<Nut>();
   
   public CarWorld () {
      LFtire = new Tire("tire 1", this);
      RFtire = new Tire("tire 2", this);
      LRtire = new Tire("tire 3", this); // note order 
      RRtire = new Tire("tire 4", this);
      // must create tires before calling Car constructor
      MyCar = new Car(this);
   }
   
   /**
    * public copy constuctor
    * 
    * @see edu.wpi.cetask.Utils#copy(Object)
    */
   public CarWorld (CarWorld world) {
      LFtire = new Tire(world.LFtire, this);
      RFtire = new Tire(world.RFtire, this);
      RRtire = new Tire(world.RRtire, this);
      LRtire = new Tire(world.LRtire, this);
      // must copy tires before calling Hub copy constructor 
      MyCar = new Car(world.MyCar, this);
      for (Nut nut : world.LOOSE_NUTS)
         LOOSE_NUTS.add(new Nut(nut, this));
   }
   
   public void print (PrintStream stream) {
      if ( !LOOSE_NUTS.isEmpty() ) {
         stream.append("\nLOOSE_NUTS: ");
         stream.append(LOOSE_NUTS.toString());
         stream.append("\n");
      }
		MyCar.print(stream, "");
	}

   // equals and hashCode generated by Eclipse
   
   @Override
   public boolean equals (Object obj) {
      if ( this == obj )
         return true;
      if ( obj == null )
         return false;
      if ( getClass() != obj.getClass() )
         return false;
      CarWorld other = (CarWorld) obj;
      if ( LFtire == null ) {
         if ( other.LFtire != null )
            return false;
      } else if ( !LFtire.equals(other.LFtire) )
         return false;
      if ( LOOSE_NUTS == null ) {
         if ( other.LOOSE_NUTS != null )
            return false;
      } else if ( !LOOSE_NUTS.equals(other.LOOSE_NUTS) )
         return false;
      if ( LRtire == null ) {
         if ( other.LRtire != null )
            return false;
      } else if ( !LRtire.equals(other.LRtire) )
         return false;
      if ( MyCar == null ) {
         if ( other.MyCar != null )
            return false;
      } else if ( !MyCar.equals(other.MyCar) )
         return false;
      if ( RFtire == null ) {
         if ( other.RFtire != null )
            return false;
      } else if ( !RFtire.equals(other.RFtire) )
         return false;
      if ( RRtire == null ) {
         if ( other.RRtire != null )
            return false;
      } else if ( !RRtire.equals(other.RRtire) )
         return false;
      return true;
   }

   @Override
   public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((LFtire == null) ? 0 : LFtire.hashCode());
      result = prime * result
         + ((LOOSE_NUTS == null) ? 0 : LOOSE_NUTS.hashCode());
      result = prime * result + ((LRtire == null) ? 0 : LRtire.hashCode());
      result = prime * result + ((MyCar == null) ? 0 : MyCar.hashCode());
      result = prime * result + ((RFtire == null) ? 0 : RFtire.hashCode());
      result = prime * result + ((RRtire == null) ? 0 : RRtire.hashCode());
      return result;
   }

}
