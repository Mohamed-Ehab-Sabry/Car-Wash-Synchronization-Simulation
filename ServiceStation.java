import javax.swing.*;
import java.awt.*;

// ===================================================================
// CLASS 1: ServiceStation (The Main Entry Point)
// Its only job is to launch the GUI on the correct thread.
// ===================================================================
public class ServiceStation {

    public static void main(String[] args) {
        // This is the best-practice way to start a Swing application.
        // It ensures the GUI is built and shown on the Event Dispatch Thread (EDT).
        SwingUtilities.invokeLater(() -> {
            new CarWashGUI(); // This creates and shows our window
        });
    }
}

// ===================================================================
// CLASS 2: CarWashGUI (The Main Window and Controller)
// This class builds the GUI and provides thread-safe methods
// for the simulation to call.
// ===================================================================
class CarWashGUI extends JFrame {

    // --- GUI Components ---
    private JLabel[] pumpLabels;        // Array of labels to show pump status
    private JTextArea logArea;          // The log at the bottom
    private JLabel waitingCarsLabel;   // Shows number of cars in queue
    
    private JPanel inputPanel;
    private JTextField waitingField, pumpsField, carsField;
    private JButton startButton;

    // --- Simulation Components ---
    private shared_queue sq;
    private Car cars_producer;
    private Pump[] pumps_consumer;

    public CarWashGUI() {
        // 1. --- Basic Window Setup ---
        setTitle("Car Wash Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5)); // Add 5px gaps
        
        // Use a nice modern look if available
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Could not set LookAndFeel");
        }

        // 2. --- Input Panel (Top) ---
        inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        waitingField = new JTextField("5", 3); // 5 = default, 3 = width
        pumpsField = new JTextField("3", 3);
        carsField = new JTextField("C1 C2 C3 C4 C5 C6 C7", 20);
        startButton = new JButton("Start Simulation");

        inputPanel.add(new JLabel("Waiting Area:"));
        inputPanel.add(waitingField);
        inputPanel.add(new JLabel("Pumps:"));
        inputPanel.add(pumpsField);
        inputPanel.add(new JLabel("Cars:"));
        inputPanel.add(carsField);
        inputPanel.add(startButton);
        add(inputPanel, BorderLayout.NORTH);

        // 3. --- Log Panel (Bottom) ---
        logArea = new JTextArea(15, 50); // 15 rows high
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        add(logScrollPane, BorderLayout.SOUTH);

        // 4. --- Waiting Label (West) ---
        waitingCarsLabel = new JLabel("<html>Waiting<br>Cars: 0</html>"); // Use HTML for line break
        waitingCarsLabel.setFont(new Font("Arial", Font.BOLD, 16));
        waitingCarsLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(waitingCarsLabel, BorderLayout.WEST);

        // 5. --- Add the "Start" button logic ---
        startButton.addActionListener(e -> startSimulation());

        // 6. --- Finalize ---
        pack(); // Smartly resize window to fit components
        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
    }
    
    /**
     * This is called by the "Start" button.
     */
    private void startSimulation() {
        // 1. Disable button to prevent re-clicks
        startButton.setEnabled(false);
        
        // 2. Get user input
        int waitingSize = Integer.parseInt(waitingField.getText());
        int numPumps = Integer.parseInt(pumpsField.getText());
        String[] carNames = carsField.getText().split(" ");
        
        // 3. Create the GUI for the Pumps (Center)
        JPanel pumpsPanel = new JPanel(new GridLayout(numPumps, 1, 5, 5)); // Grid with gaps
        pumpsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pumpLabels = new JLabel[numPumps];
        
        for (int i = 0; i < numPumps; i++) {
            pumpLabels[i] = new JLabel("Pump " + (i + 1) + ": FREE");
            pumpLabels[i].setFont(new Font("Arial", Font.BOLD, 14));
            pumpLabels[i].setOpaque(true); // Needed to show background color
            pumpLabels[i].setBackground(Color.GREEN);
            pumpLabels[i].setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            pumpsPanel.add(pumpLabels[i]);
        }
        add(pumpsPanel, BorderLayout.CENTER);
        
        // 4. Create the simulation objects, passing "this" (the GUI)
        sq = new shared_queue(waitingSize, numPumps, this); // Pass the GUI to the queue
        
        // 5. Create and start all threads
        cars_producer = new Car(sq, carNames);
        pumps_consumer = new Pump[numPumps];
        
        for (int i = 0; i < numPumps; i++) {
            String pumpName = "Pump " + (i + 1);
            // Give the Pump its index (i) so it can update the correct label
            pumps_consumer[i] = new Pump(sq, pumpName, i); 
            pumps_consumer[i].start();
        }
        cars_producer.start();
        
        // 6. Create a simple "watcher" thread to re-enable the button
        new Thread(() -> {
            try {
                cars_producer.join();
                for (Pump p : pumps_consumer) {
                    p.join();
                }
                logMessage("All cars processed; simulation ends");
                // Re-enable the button on the EDT
                SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logMessage("Watcher thread interrupted; simulation may be incomplete.");
            }
        }).start();
        
        // 7. Re-draw the window to show the new pumps panel
        revalidate();
        pack();
    }

    // -----------------------------------------------------------------
    //  THREAD-SAFE HELPER METHODS
    //  This is the *only* way the simulation should talk to the GUI.
    // -----------------------------------------------------------------

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updatePumpStatus(int pumpIndex, String text, boolean isBusy) {
        SwingUtilities.invokeLater(() -> {
            pumpLabels[pumpIndex].setText(text);
            pumpLabels[pumpIndex].setBackground(isBusy ? Color.RED : Color.GREEN);
        });
    }

    public void updateWaitingCount(int count) {
        SwingUtilities.invokeLater(() -> {
            waitingCarsLabel.setText("<html>Waiting<br>Cars: " + count + "</html>");
        });
    }
}

// ===================================================================
// CLASS 3: shared_queue (The Simulation Core)
// This is the updated class that uses the GUI controller.
// ===================================================================
class shared_queue {

    public int waiting_area_size = 1;
    public int num_of_service_bays = 1;
    public int service_inptr = 0, waiting_inptr = 0, waiting_outptr = 0;

    private Object waiting_area[];
    private Object service_bays[];
    Semaphore waiting_area_counter;
    Semaphore service_bays_counter = new Semaphore(0);
    Semaphore pumps_Semaphore;
    
    private CarWashGUI gui; // The GUI controller

    shared_queue(int waiting_area_size, int num_of_service_bays, CarWashGUI gui) {
        this.waiting_area_size = waiting_area_size;
        this.num_of_service_bays = num_of_service_bays;
        this.waiting_area = new Object[waiting_area_size];
        this.service_bays = new Object[num_of_service_bays];
        waiting_area_counter = new Semaphore(waiting_area_size);
        pumps_Semaphore = new Semaphore(num_of_service_bays);
        this.gui = gui; // Store the GUI reference
    }

    Object produce_lock = new Object();
    Object consume_lock = new Object();

    public void produce(Object value) {
        try {
            Thread.sleep(300); // Simulate car arrival time
        } catch (Exception e) {
            gui.logMessage("Error: Car arrival sleep interrupted");
        }
        
        if (!value.equals("Stop")) {
            gui.logMessage(value + " has arrived");
        }

        if (pumps_Semaphore.isFull() && !value.equals("Stop")) {
            gui.logMessage(value + " arrived and waiting");
        }
        
        // Update waiting count (this is just an estimate before the lock)
        // A more accurate way might be to add/subtract a shared counter
        gui.updateWaitingCount(service_bays_counter.get_counter() + 1);

        waiting_area_counter.check_if_free(); // Wait for a spot
        
        // Simplified: just one synchronized block
        synchronized (produce_lock) {
            waiting_area[waiting_inptr] = value;
            waiting_inptr = (waiting_inptr + 1) % waiting_area_size;
        }

        service_bays_counter.check_if_waiting(); // Signal car is ready
    }

    public Object consume(String pump_name, int pumpIndex) { // Accept pumpIndex
        
        service_bays_counter.check_if_free(); // Wait for a car
        
        // Update waiting count
        gui.updateWaitingCount(service_bays_counter.get_counter() - 1);

        Object car_name;
        int my_bay_idx = 0;
        
        synchronized (consume_lock) {
            car_name = waiting_area[waiting_outptr];
            
            if (!car_name.equals("Stop")) {
                pumps_Semaphore.check_if_free(); // Acquire a pump
                service_bays[service_inptr] = waiting_area[waiting_outptr];
                my_bay_idx = service_inptr;
                
                // Update GUI
                gui.logMessage(pump_name + ": " + car_name + " Occupied");
                gui.updatePumpStatus(pumpIndex, pump_name + ": " + car_name, true);
                
                service_inptr = (service_inptr + 1) % num_of_service_bays;
            }
            // This MUST be updated for both cars and "Stop"
            waiting_outptr = (waiting_outptr + 1) % waiting_area_size;
        }

        waiting_area_counter.check_if_waiting(); // Signal queue spot is free
        
        if (!car_name.equals("Stop")) {
            // --- Simulate Service ---
            try {
                gui.logMessage(pump_name + ": " + car_name + " login");
                Thread.sleep(200); // Login time
                
                gui.logMessage(pump_name + ": " + car_name + " begins service at Bay " + (my_bay_idx + 1));
                Thread.sleep(1000); // Service time
                
                gui.logMessage(pump_name + ": " + car_name + " finished service");
                gui.logMessage(pump_name + ": " + "Bay " + (my_bay_idx + 1) + " is now free");

            } catch (InterruptedException e) {
                gui.logMessage("Error: Service for " + car_name + " interrupted");
            }
            
            // --- Update GUI ---
            gui.updatePumpStatus(pumpIndex, pump_name + ": FREE", false);
            
            pumps_Semaphore.check_if_waiting(); // Release the pump
        }

        return car_name;
    }
}

// ===================================================================
// CLASS 4: Pump (The Consumer)
// Updated to store its index and pass it to consume.
// ===================================================================
class Pump extends Thread {

    String name;
    shared_queue sq;
    int pumpIndex; // The pump's ID (0, 1, 2...)

    Pump(shared_queue sq, String name, int pumpIndex) {
        this.sq = sq;
        this.name = name;
        this.pumpIndex = pumpIndex; // Store the index
    }

    @Override
    public void run() {
        while (true) {
            // Pass the pump's index to the consume method
            Object consumed_item = sq.consume(name, pumpIndex); 
            
            if (consumed_item.equals("Stop")) {
                break; // Stop this pump thread
            }
        }
    }
}

// ===================================================================
// CLASS 5: Car (The Producer)
// No changes needed from your version.
// ===================================================================
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

// ===================================================================
// CLASS 6: Semaphore
// No changes needed from your version.
// ===================================================================
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
                System.out.println("Error in semaphore wait: " + e.getMessage());
            }
        }
    }

    public synchronized void check_if_waiting() {
        counter++;
        if (counter <= 0) {
            try {
                notify();
            } catch (Exception e) {
                System.out.println("Error in semaphore notify: " + e.getMessage());
            }
        }
    }

    public synchronized boolean isFull() {
        return counter <= 0;
    }
    
    // This is a thread-safe way to get the count for display
    public synchronized int get_counter() {
        return counter;
    }
}