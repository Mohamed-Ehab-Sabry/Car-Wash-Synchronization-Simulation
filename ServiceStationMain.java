import java.util.Scanner;

class Semaphore {

    protected int counter = 0;

    Semaphore(int init_value) {

        counter = init_value;

    }

    public synchronized void check_if_free() {

        counter--;

        if (counter < 0) {

            try {

                wait();

            } catch (Exception e) {

                System.out
                        .println(ServiceStationMain.RED + "The error is " + e.getMessage() + ServiceStationMain.RESET);
            }
        }
    }

    public synchronized void check_if_waiting() {

        counter++;

        if (counter <= 0) {

            try {

                notify();

            } catch (Exception e) {

                System.out
                        .println(ServiceStationMain.RED + "The error is " + e.getMessage() + ServiceStationMain.RESET);
            }
        }
    }

    public synchronized boolean isFull() {

        return counter <= 0;
    }

}

class Car extends Thread {

    shared_queue sq;
    public String car_names[];

    Car(shared_queue sq, String car_names[]) {

        this.sq = sq;
        this.car_names = car_names;

    }

    @Override
    public void run() {

        for (String car : car_names) {

            sq.produce(car);
        }
        for (int i = 0; i < sq.num_of_service_bays; i++) {
            sq.produce("Stop");
        }
    }

}

class Pump extends Thread {

    String name;
    shared_queue sq;

    Pump(shared_queue sq, String name) {

        this.sq = sq;
        this.name = name;

    }

    @Override
    public void run() {
        while (true) {

            Object consumed_item = sq.consume(name);
            if (consumed_item.equals("Stop")) {
                break;
            }
        }
    }

}

class shared_queue {

    public int waiting_area_size = 1;
    public int num_of_service_bays = 1;
    public int service_inptr = 0, waiting_inptr = 0, waiting_outptr = 0;

    private Object waiting_area[];
    private Object service_bays[];
    Semaphore waiting_area_counter;
    Semaphore service_bays_counter = new Semaphore(0);
    Semaphore pumps_Semaphore;

    shared_queue(int waiting_area_size, int num_of_service_bays) {

        this.waiting_area_size = waiting_area_size;
        this.num_of_service_bays = num_of_service_bays;
        this.waiting_area = new Object[waiting_area_size];
        this.service_bays = new Object[num_of_service_bays];
        waiting_area_counter = new Semaphore(waiting_area_size);
        pumps_Semaphore = new Semaphore(num_of_service_bays);

    }

    Object produce_lock = new Object();
    Object consume_lock = new Object();

    public void produce(Object value) {

        try {
            Thread.sleep(300);
        } catch (Exception e) {
            System.out.println(ServiceStationMain.RED + "Can't sleep" + ServiceStationMain.RESET);
        }

        waiting_area_counter.check_if_free();

        synchronized (produce_lock) {

            waiting_area[waiting_inptr] = value;
            waiting_inptr = (waiting_inptr + 1) % waiting_area_size;

            if (!value.equals("Stop")) {

                System.out.println(
                        ServiceStationMain.BRIGHT_GREEN + value + " has arrived" + ServiceStationMain.RESET);

                if (pumps_Semaphore.isFull()) {
                    System.out.println(
                            ServiceStationMain.BRIGHT_YELLOW + value + " arrived and waiting"
                                    + ServiceStationMain.RESET);
                }
            }
        }

        service_bays_counter.check_if_waiting();
    }

    public Object consume(String pump_name) {

        service_bays_counter.check_if_free();
        Object car_name;
        int my_bay_idx = 0;
        synchronized (consume_lock) {

            car_name = waiting_area[waiting_outptr];
            if (!car_name.equals("Stop")) {

                pumps_Semaphore.check_if_free();
                service_bays[service_inptr] = waiting_area[waiting_outptr];

                my_bay_idx = service_inptr;
                System.out.println(
                        ServiceStationMain.CYAN + pump_name + ": " + car_name + " Ocuupied" + ServiceStationMain.RESET);
                service_inptr = (service_inptr + 1) % num_of_service_bays;
            }
            waiting_outptr = (waiting_outptr + 1) % waiting_area_size;
        }

        waiting_area_counter.check_if_waiting();
        if (!car_name.equals("Stop")) {

            System.out.println(
                    ServiceStationMain.BLUE + pump_name + ": " + car_name + " login" + ServiceStationMain.RESET);
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                System.err.println(
                        ServiceStationMain.RED + "Can't sleep, the error: " + e.getMessage()
                                + ServiceStationMain.RESET);
            }
            System.out.println(ServiceStationMain.BRIGHT_GREEN + pump_name + ": " + car_name + " begins service at Bay "
                    + (my_bay_idx + 1) + ServiceStationMain.RESET);

            try {

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.println(
                        ServiceStationMain.RED + "The Service was interrupted " + e.getMessage()
                                + ServiceStationMain.RESET);
            }
            System.out.println(ServiceStationMain.BRIGHT_GREEN + pump_name + ": " + car_name + " finshes service"
                    + ServiceStationMain.RESET);
            System.out.println(ServiceStationMain.GREEN + pump_name + ": " + "Bay " + (my_bay_idx + 1) + " is now free"
                    + ServiceStationMain.RESET);
            pumps_Semaphore.check_if_waiting();
        }

        return car_name;

    }

}

public class ServiceStationMain {

    // ANSI Color codes
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bright colors
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";

    static String car_names[];

    public static void main(String[] args) {

        java.util.Scanner input_scanner = new Scanner(System.in);
        System.out.print("Please enter the waiting area capacity: ");
        int waiting_area_size = input_scanner.nextInt();
        System.out.print("Now please enter the number of service bays (pumps): ");
        int num_of_service_bays = input_scanner.nextInt();
        input_scanner.nextLine();
        System.out.print("Cars arriving (order): ");
        car_names = input_scanner.nextLine().split(" ");

        shared_queue sq = new shared_queue(waiting_area_size, num_of_service_bays);

        Car cars_producer = new Car(sq, car_names);
        Pump pumps_consumer[] = new Pump[num_of_service_bays];
        input_scanner.close();
        for (int i = 0; i < num_of_service_bays; i++) {

            String pump_name = "Pump " + (i + 1);
            pumps_consumer[i] = new Pump(sq, pump_name);
        }
        for (int i = 0; i < num_of_service_bays; i++) {

            pumps_consumer[i].start();
        }

        cars_producer.start();
        try {
            cars_producer.join();
            for (int i = 0; i < pumps_consumer.length; i++) {
                pumps_consumer[i].join();
            }

        } catch (Exception e) {

            System.err.println(ServiceStationMain.RED + "Can't join with other threads, the error is: " + e.getMessage()
                    + ServiceStationMain.RESET);
        }
        System.out
                .println(ServiceStationMain.PURPLE + "All cars processed; simulation ends" + ServiceStationMain.RESET);
    }

}