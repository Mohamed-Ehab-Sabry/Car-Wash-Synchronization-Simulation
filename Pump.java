import java.util.Queue;
import java.util.concurrent.Semaphore;

    public class Pump extends Thread {
    private String name,id;
    private Queue<Car> queue;
    private Semaphore mutex, empty, full, pumps;

    public Pump(String name,String id, Queue<Car> queue, Semaphore mutex, Semaphore empty, Semaphore full, Semaphore pumps) {
        this.name = name;
        this.id=id;
        this.queue = queue;
        this.mutex = mutex;
        this.empty = empty;
        this.full = full;
        this.pumps = pumps;

    }

    public void run() {
        try {
            while (true) {
                full.waitSem();   
                

                mutex.waitSem();
                
                Car car= queue.poll();
                System.out.println(name +": " + car.getName()+ " login");
                mutex.signalSem();

  
                pumps.waitSem();   
                System.out.println(name +": " + car.getName() + "begins service at Bay "+ id);

                Thread.sleep(2000); 

                System.out.println( name +": " + car.getName() + " finishes service at Bay " + id );

                System.out.println( name +": " + "Bay " + id +"is now free");
                pumps.signalSem();  
                empty.signalSem();  
            }
        } catch (InterruptedException e) {
            System.out.println( name +": " + "stopped  " );
        }
    }
}

