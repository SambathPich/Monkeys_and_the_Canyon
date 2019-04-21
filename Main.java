package main;
/**
 *
 * @author sambath
 */

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main (String [] args) {
        System.out.println("=================[ INFORMATION ]=================");
        System.out.println("Total Monkey: " + MainConfiguration.totalMonkey);
        System.out.println("Maximum Monkeys on the Rope: " + MainConfiguration.maxMonkeyOnRope);
        System.out.println("Creation Time: Between 1 to " + MainConfiguration.maxCreationTime + " seconds");
        System.out.println("Get on the rope: " + MainConfiguration.getOnTheRope + " second");
        System.out.println("Crossing Time: " + MainConfiguration.crossingTime + " seconds");
        System.out.println("=================================================");
        
        MonkeyPreviousInfo monkeyPreviousInfo = new MonkeyPreviousInfo();
        MonkeyCurrentInfo monkeyCurrentInfo = new MonkeyCurrentInfo();

        MonkeyCreator monkeyCreator = new MonkeyCreator(monkeyPreviousInfo, monkeyCurrentInfo);
        monkeyCreator.start();
    }
}

//Main Configuration
class MainConfiguration {
    public static int totalMonkey = 60;
    public static int maxMonkeyOnRope = 6;
    public static int maxCreationTime = 8;
    public static int getOnTheRope = 1;
    public static int crossingTime = 6;
}

//Maintain Info for Previous Monkey
class MonkeyPreviousInfo {
    public static int previousName;
    public static String previousDirection;
    public static int remainingPermits;         //Keep track of how many monkeys are already on the rope
    public static int waitingToCross;           //Keep track of how many monkeys are waiting to cross      
}

//Maintain Info for Current Monkey
class MonkeyCurrentInfo {
    public static int currentName;
    public static String currentDirection;
}

//Create Monkeys
class MonkeyCreator extends Thread {
    private int totalMonkeys = 60;
    private MonkeyPreviousInfo monkeyPreviousInfo;
    private MonkeyCurrentInfo monkeyCurrentInfo;
    private String newDirection;
    Random random = new Random();

    public MonkeyCreator(MonkeyPreviousInfo monkeyPreviousInfo, MonkeyCurrentInfo monkeyCurrentInfo) {
        this.monkeyPreviousInfo = monkeyPreviousInfo;
        this.monkeyCurrentInfo = monkeyCurrentInfo;
    }

    @Override
    public void run() {
        for (int i = 1; i <= totalMonkeys; i++) {
            //Default: Sleep Randomly from 1 to 8 seconds
            try {
                Thread.sleep ((int) (Math.random () * (MainConfiguration.maxCreationTime * 1000)));
            } catch (InterruptedException e) {
            }

            //Assign Direction Randomly for new arrival Monkey
            if (random.nextBoolean()) {
                newDirection = "EASTWARD";
            } else {
                newDirection = "WESTWARD";
            }

            //Set Initial Direction and Name for first ever Monkey
            if (i == 1) {
                monkeyCurrentInfo.currentName = i;
                monkeyCurrentInfo.currentDirection = newDirection;

                monkeyPreviousInfo.previousName = i;
                monkeyPreviousInfo.previousDirection = newDirection;
                monkeyPreviousInfo.remainingPermits = MainConfiguration.maxMonkeyOnRope;
                monkeyPreviousInfo.waitingToCross = 0;
                
            } else {
                monkeyPreviousInfo.previousName = i - 1;
                monkeyPreviousInfo.previousDirection = monkeyCurrentInfo.currentDirection;

                monkeyCurrentInfo.currentName = i;
                monkeyCurrentInfo.currentDirection = newDirection;
            }
            System.out.println("\nMonkey " + monkeyCurrentInfo.currentName  + " is trying to cross " + monkeyCurrentInfo.currentDirection + " at time " + LocalDateTime.now());

            MonkeyCrossing monkeyCrossing = new MonkeyCrossing(monkeyCurrentInfo.currentName, monkeyCurrentInfo.currentDirection, monkeyPreviousInfo);
            monkeyCrossing.start();
        }
    }
}

//Take care of The Rope and the turn for Monkeys to cross
class MonkeyCrossing extends Thread {
    private final MonkeyPreviousInfo monkeyPreviousInfo;
    private final int currentName;
    private final String currentDirection;
    private Semaphore semaphoreRope;
    private final CountDownLatch emptyRope = new CountDownLatch(1);
    
    public MonkeyCrossing(int currentName, String currentDirection, MonkeyPreviousInfo monkeyPreviousInfo) {
        this.currentName = currentName;
        this.currentDirection = currentDirection;
        this.monkeyPreviousInfo = monkeyPreviousInfo;
        semaphoreRope = new Semaphore(monkeyPreviousInfo.remainingPermits);
        monkeyPreviousInfo.waitingToCross ++;
        
        System.out.print("\tMonkey " + currentName + " is waiting to cross the canyon at time " + LocalDateTime.now());
        System.out.print(" +++++ [Allowed Direction: " + monkeyPreviousInfo.previousDirection);
        try {
            //If Monkeys are going the to the same direction
            if (monkeyPreviousInfo.previousDirection.equals(currentDirection)) {
                if(monkeyPreviousInfo.remainingPermits < 0) {
                    System.out.println(", Available Permits: 0 ] +++++");
                } else {
                    System.out.println(", Available Permits: " + monkeyPreviousInfo.remainingPermits + "] +++++");
                }
            } else {
                monkeyPreviousInfo.previousDirection = currentDirection;                        //Reset Direction
                semaphoreRope = new Semaphore(MainConfiguration.maxMonkeyOnRope);               //Reset Semaphore
                monkeyPreviousInfo.remainingPermits = MainConfiguration.maxMonkeyOnRope;        //Reset Remaining Permits 
               
                //Show more Info
                System.out.println(", Permits Reset To: " + monkeyPreviousInfo.remainingPermits + "] +++++");
                
                //Calculate waiting time and wait until the rope is empty
                int waitingTime = (monkeyPreviousInfo.waitingToCross - 1);
                waitingTime = (MainConfiguration.crossingTime + MainConfiguration.getOnTheRope) * waitingTime;

                //Show more Info
                System.out.println("\tMonkey " + currentName + " is from OPPOSITE direction and must wait " + waitingTime + " seconds until [ MONKEY " + (currentName - 1) + " ] has crossed");
                while(emptyRope.await(waitingTime, TimeUnit.SECONDS)) {}  
                System.out.println("\tThe Rope is CLEAR, so Monkey " + currentName + " is now allowed to cross");
            }
        } catch (InterruptedException e) {
        }
    }
    
    @Override
    public void run() {
        try {
                if(semaphoreRope.tryAcquire()) {
                    monkeyPreviousInfo.remainingPermits --;
                    Thread.sleep(MainConfiguration.getOnTheRope * 1000);             //Get on the Rope: Default => 1 Second
                    System.out.println("\tMonkey " + currentName + " is now crossing the canyon at time " + LocalDateTime.now());
                    Thread.sleep(MainConfiguration.crossingTime * 1000);             //Crossing Time: Default => 6 Seconds
                    System.out.println("\t==> MONKEY " + currentName + " HAS CROSSED THE CANYON AT TIME " + LocalDateTime.now());
                } else {
                    //System.out.println("\n\n\n\t\t\t\t\tFull, Monkey " + currentName + " has to wait until the Rope is clear\n\n\n");
                    System.out.println("\tThe Rope is FULL. Monkey " + currentName + " must wait for its turn");
                    
                    //Wait for its turn
                    int waitingTime = (MainConfiguration.crossingTime + MainConfiguration.getOnTheRope);
                    while(emptyRope.await(waitingTime, TimeUnit.SECONDS)) {}
                    System.out.println("\tThe Rope is AVAILABLE, so Monkey " + currentName + " is now allowed to cross");
                    monkeyPreviousInfo.remainingPermits --;
                    Thread.sleep(MainConfiguration.getOnTheRope * 1000);             //Get on the Rope: Default => 1 Second
                    System.out.println("\tMonkey " + currentName + " is now crossing the canyon at time " + LocalDateTime.now());
                    Thread.sleep(MainConfiguration.crossingTime * 1000);             //Crossing Time: Default => 6 Seconds
                    System.out.println("\t==> MONKEY " + currentName + " HAS CROSSED THE CANYON AT TIME " + LocalDateTime.now());
                }
                    
        } catch (InterruptedException e) {
        } finally {
            //If direction has changed, no need to release because it has already reset.
            if (monkeyPreviousInfo.previousDirection.equals(currentDirection)) {
                //Release one semaphore
                semaphoreRope.release();
                monkeyPreviousInfo.remainingPermits ++;
            } 
            monkeyPreviousInfo.waitingToCross --;
        }
    }
}

