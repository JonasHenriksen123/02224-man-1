import java.lang.System;
import josx.platform.rcx.*;
import bagsortsim.BagSortSim;

class Feeder extends Thread {
 
  static final int BLOCKED = 70, YELLOW  = 60, BLACK   = 48;

  Motor  myMotor;
  Sensor mySensor;

  public Feeder(Motor m, Sensor s) { myMotor = m;  mySensor = s; }
  
  public void run() {
    try {
      final boolean closeToA = (myMotor == Motor.A);
      final int myMask = closeToA ? Poll.SENSOR1_MASK : Poll.SENSOR2_MASK;

      Poll e = new Poll();
      int done = (int) System.currentTimeMillis(); // When last bag is through
      boolean isMotorCStopped = false;

      myMotor.forward();

      while (true) {
        // Await arrival of a bags
        mySensor.activate();
        while(mySensor.readValue() > BLOCKED) { 
          e.poll(myMask,0); 
          // ##### Green Version Implementation #####
          if ((int) System.currentTimeMillis() > done && !isMotorCStopped) {
            Motor.C.stop();
            isMotorCStopped = true;
          }
          // ########################################
        }
	  
        Thread.sleep(800);           // Wait for colour to be valid

        boolean destA = (mySensor.readValue() > BLACK);   // Determine destination
        mySensor.passivate();

        Thread.sleep(2000);       // Advance beyond sensor
        // ##### Green Version Implementation #####
        if (isMotorCStopped) {
          if (destA) Motor.C.forward();
          else Motor.C.backward();
          isMotorCStopped = false;
        }
        // ########################################
        else if (Motor.C.isForward() != destA) {  // Must stop
          myMotor.stop();
          int now = (int) System.currentTimeMillis();
          if (now < done) Thread.sleep(done-now);
          Motor.C.reverseDirection();
          myMotor.forward();
        }

        done = ((int) System.currentTimeMillis()) + 6000;  
        if (Motor.C.isForward() != closeToA) done = done + 6000;  // Long path

        Thread.sleep(1200);                 // Follow to end of feed belt
       }
    } catch (Exception e) { }
  }
}

public class GreenSort {

  static final int BELT_SPEED = 5;          // Do not change

  public static void main (String[] arg) {
    
    BagSortSim sim = BagSortSim.getSimulator();
		
    /* Set simulation parameters */
    sim.setActiveCheck(true);
    sim.setFeedCheck(false);
    sim.setSeparation(80); 
    sim.start();

    new Travellers(sim).start();
    
      Motor.A.setPower(BELT_SPEED);
      Motor.B.setPower(BELT_SPEED);
      Motor.C.setPower(BELT_SPEED);  Motor.C.forward();

      Thread f1 = new Feeder(Motor.A, Sensor.S1);  f1.start();
      Thread f2 = new Feeder(Motor.B, Sensor.S2);  f2.start();

      try{ Button.RUN.waitForPressAndRelease();} catch (Exception e) {}
      System.exit(0);
  }
}

class Travellers extends Thread {
	
	BagSortSim sim;
	
	public Travellers (BagSortSim sim) {
		this.sim = sim;
	}
	
	public void run() {
		try {
			while (true) {
				sleep(20000);
				int counter = (Math.random() < 0.5 ? 1 : 1);  // Use only Feed 1 for SingleSort
				int color   = (Math.random() < 0.6 ? BagSortSim.YELLOW  : BagSortSim.BLACK);
				sim.checkin(counter,color);
			}
		} catch (InterruptedException e) {}
	}
	
}
