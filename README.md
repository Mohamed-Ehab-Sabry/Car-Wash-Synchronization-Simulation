# Car Wash Synchronization Simulation

This project is a Java-based simulation of a car wash service station, developed to demonstrate a solution to the classic **Producer-Consumer Problem**. It uses core operating system synchronization primitives like semaphores and mutexes to manage concurrent access to shared resources, preventing race conditions and deadlocks.

The simulation models a service station with a limited number of service bays (pumps) and a fixed-size waiting queue for incoming cars.

## Key Concepts Demonstrated
This project implements and explores the following fundamental Operating Systems concepts:
- **Producer-Consumer Problem**: `Car` objects act as producers arriving at the station, and `Pump` objects act as consumers servicing the cars.
- **Bounded Buffer**: The car waiting area is a fixed-size queue that acts as a bounded buffer between producers and consumers.
- **Semaphores**: Used to manage the state of the waiting queue (empty and full slots) and to control access to the limited number of concurrent service bays.
- **Mutex (Mutual Exclusion)**: Ensures that only one thread can modify the shared waiting queue at any given time, preventing data corruption.
- **Concurrency & Multithreading**: The entire simulation runs on multiple threads, with cars arriving and pumps working concurrently.

## How It Works

1.  **Cars (Producers)**: A continuous stream of `Car` threads is generated. Each car attempts to enter a fixed-size waiting queue. If the queue is full, the car thread will wait until a space becomes available.
2.  **Pumps (Consumers)**: A fixed number of `Pump` threads run in a background thread pool. Each pump waits for a car to enter the queue. Once a car is available, a pump will take it from the queue for servicing.
3.  **Synchronization**:
    - An `empty` semaphore tracks the number of available slots in the waiting queue. Producers wait on this if the queue is full.
    - A `full` semaphore tracks the number of cars waiting in the queue. Consumers wait on this if the queue is empty.
    - A `pumps` semaphore limits the number of cars that can be serviced simultaneously to the number of available bays.
    - A `mutex` lock provides exclusive access to the queue for add/remove operations.

## Features
- **Dynamic Configuration**: The capacity of the waiting area and the number of service bays can be configured at the start of the simulation.
- **Concurrent Processing**: Simulates multiple service bays operating in parallel.
- **Detailed Event Logging**: The console output clearly indicates all system activities, including when a car arrives, enters the queue, is serviced by a pump, and when it finishes.
- **Synchronization Management**: A robust implementation of semaphores to handle complex multi-threaded interactions safely.
- **GUI (Bonus)**: Includes a graphical user interface to visualize the state of the pumps (occupied or free) and the cars being serviced in real-time.

## Technologies Used
- **Java**

## Getting Started

### Prerequisites
- Java Development Kit (JDK) 8 or higher must be installed.

### Compilation & Execution
1.  Clone the repository:
    ```sh
    git clone [https://github.com/your-username/Car-Wash-Synchronization-Simulation.git](https://github.com/your-username/Car-Wash-Synchronization-Simulation.git)
    cd Car-Wash-Synchronization-Simulation
    ```
2.  Compile the Java source files. If all classes are in one file as per submission rules (`ID1_ID2_ID3_Group.java`):
    ```sh
    javac ID1_ID2_ID3_Group.java
    ```
    If they are in separate files:
    ```sh
    javac ServiceStation.java Car.java Pump.java Semaphore.java
    ```
3.  Run the main class to start the simulation:
    ```sh
    java ServiceStation
    ```
    You may need to provide the waiting area capacity and the number of pumps as command-line arguments, depending on your implementation. For example:
    ```sh
    java ServiceStation 5 3
    ```

## Sample Output
Here is an example of the simulation's log output with a waiting area of 5 and 3 service bays:
```log
C1 arrived
C2 arrived
C3 arrived
C4 arrived
Pump 1: C1 Occupied
Pump 2: C2 Occupied
Pump 3: C3 Occupied
C4 arrived and waiting
C5 arrived
C5 arrived and waiting
Pump 1: C1 login
Pump 1: C1 begins service at Bay 1
Pump 2: C2 login
Pump 2: C2 begins service at Bay 2
Pump 3: C3 login
Pump 3: C3 begins service at Bay 3
Pump 1: C1 finishes service
Pump 1: Bay 1 is now free
Pump 2: C2 finishes service
Pump 2: Bay 2 is now free
Pump 1: C4 login
Pump 1: C4 begins service at Bay 1
Pump 3: C3 finishes service
Pump 3: Bay 3 is now free
Pump 2: C5 login
Pump 2: C5 begins service at Bay 2
Pump 1: C4 finishes service
Pump 1: Bay 1 is now free
Pump 2: C5 finishes service
Pump 2: Bay 2 is now free
All cars processed; simulation ends
```

## Project Structure
The project consists of the following main classes:
- `ServiceStation.java`: The main class that initializes shared resources (semaphores, queue), and creates the Car and Pump threads.
- `Car.java`: Represents the producer thread. Each instance is a car arriving at the station.
- `Pump.java`: Represents the consumer thread. Each instance is a service bay that services a car.
- `Semaphore.java`: A custom implementation of the semaphore signaling mechanism.

---
*This project was developed for the CS241: Operating System - 1 course at Cairo University.*
