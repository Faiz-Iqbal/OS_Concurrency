# Operating Systems and Concurrency

This project contains: 
 - the stub file for your OS class, 
 - the corresponding Interface,
 - a Main class to allow selective execution of your tests, and
 - a Tests class that contains three example tests.
 
 
# IMPORTANT
1. You MUST fork this project into your remote repository BEFORE cloning it to your local disc space.
2. 'Thread safe' and 'synchronized' classes (e.g. those in java.util.concurrent) other than ReentrantLock and Condition, MUST not be used in OS.java.
3. You MUST not use the keyword 'synchronized', or any other `thread safe` classes or mechanisms   
or any delays or 'busy waiting' (spin lock) methods in OS.java. 
4. You must not use any delays such as Thread.sleep() in OS.java.

You are required to write an “OS” class using Extrinsic Monitors. It will be marked against the User Requirements (UR) defined below using our automatic testing software. In general, the tests used for marking will not be made available to students.

You must define your own tests to assure yourself that your classes meet the UR. Note that this is standard practice in industry.

### The Stub Project Files
Each stub project contains 4 files:

- **OS_sim_interface.java**: This provides the interface that your solution class must implement. It must not be changed.
- **OS.java**: This is where you will develop your solution class. It must implement the `OS_sim_interface` and provide the functionality specified in these User Requirements. You may add inner classes and methods to this file, but no other public methods. Note that the test software will only use the default (no argument) constructor.
    - In addition, any exceptions that you introduce must be handled locally, that is you must not add throws clauses to the public methods – otherwise, compilation with test software will fail and your marks will be reduced.
- **Tests.java**: This contains three example tests. This is where you will develop your own tests to assure yourself that your classes perform as per this specification.
- **Main.java**: This contains the starting point for the JVM to execute your program. It can be used during development to control which tests are being run.

You are required to write and test an OS class that simulates non-pre-emptive scheduling of user-defined processes. Your OS class will simulate a set of ready queues of different priorities and a set of processors. Processes of the same priority will be scheduled onto processors in a FIFO (first in first out) order. Processes on higher priority ready queues will always have precedence over processes of lower priority.

Your OS class will use the default, no argument constructor. An example instantiation to simulate three processors is:
```java
OS os = new OS();
os.set_number_of_processors(3);
```

Each user-defined process will be simulated in the test software by a separate Java thread. They will use the methods defined in OS_sim_interface:
An example of the run method of a test process class is shown below:
```
public void run() {
    pid = os.reg(priority); // Register process and store its ID
    os.start(pid);
    os.schedule(pid);
    os.schedule(pid);
    os.terminate(pid);
}
```
Processes will call your methods in the above order, where `schedule` is called one or more times. These methods are described below:

- `int pid = os.reg(int priority)`: 
  - Called once by each process to register the process with your OS and obtain a unique pid.
  - Each call to `os.reg` must return a unique Process Identifier (pid).
  - `pid` must be numbered consecutively starting from 0 (zero).
  - All subsequent calls to OS methods by the process must use its unique ID to identify itself.
  - The priority argument is used to indicate the priority of the ready queue that should be used with that process.
  - Argument `priority=1` is the highest priority, `priority=2` is the next highest, etc. 
  - Higher values of this argument indicate lower priority processes.

- `os.start(pid)`:
  - Called once by a process to start the process ‘executing’.
  - If a processor is available, then the OS will return control to the calling process.
  - If there is no processor available, then the calling process must be suspended by placing it on the end of its ready queue, and awaiting on a condition variable.

- `os.schedule(pid)`:
  - Called by a process to indicate that a process is willing to give up control of its processor. This method is required as we are simulating a non-pre-emptive OS.
  - If there are no other higher priority processes ready to run, then control will be returned to the calling process.
  - If a higher priority process is ready to run, then:
    - The calling process must be suspended by placing it on the end of its ready queue, and awaiting on a condition variable (as above).
    - The highest priority ready process will be allocated to a processor and signalled to proceed. Note that the “highest priority ready process" is the process at the front of the highest priority FIFO ready queue; the ready queues considered must be of the same or higher priority than the calling process.

- `os.terminate(pid)`:
  - Called by a process when it no longer requires a processor.

***Requirements***
- **UR1 – os.reg**
  - Instances of your OS class must be able to return consecutive int pids when `pid = os.reg()` is repeatedly called. The first pid returned must be 0 (zero).
  - See `ur1_example_test()` in `Tests.java`.

- **UR2 – Single Process, Single Processor, Single Priority Queue**
  - After instantiation, `set_number_of_processors(1)` is called by the main thread to set the number of simulated processors to one.
  - One thread is then created to imitate a process. This new thread calls the OS methods in the order shown in the example run method as indicated in 6.2.1. Note that the `schedule()` method can be called zero, or more times.
  - The result of any test should be that all methods return to the caller.

- **UR3 – Multiple Processes, Single Processor, Single Priority Queue**
  - As UR2, except that two or more threads each simulate different processes. Hence, if processes p0-p1 register with a priority of 1, and these processes invoke the OS methods as indicated below, then the following behaviour will occur:
    - p0 calls `OS.start` – p0 is allocated the processor, and the function returns to the caller (p0)
    - p1 calls `OS.start` – p1 is suspended and added to the empty ready queue
    - p2 calls `OS.start` – p2 is suspended, and added to the end of the ready queue
    - p0 calls `OS.schedule` – p0 is suspended and added to the end of the ready queue; p1 is removed from the head of the ready queue, allocated to the processor, scheduled in, and control is returned to p1
    - p1 calls `OS.schedule` – p1 is suspended and added to the end of the ready queue; p2 is removed from the head of the ready queue, allocated to the processor, scheduled in, and control is returned to p2
    - p2 calls `OS.schedule` – p2 is suspended and added to the end of the ready queue; p0 is removed from the head of the ready queue, allocated to the processor, scheduled in, and control is returned to p0
    - p0 calls `OS.schedule` – p0 is suspended and added to the end of the ready queue; p1 is removed from the head of the ready queue, allocated to the processor, scheduled in, and control is returned to p1
    - And so on... in order p1, p2, p0, p1 ... etc
  - See `ur3_example_test()`.
  - Note: `nProcessors = 1` and `priority = 1`.

- **UR4 – Multiple Processes, Multiple Processors, Single Priority Queue**
  - As UR3 except that `OS(nProcessors)` is called with `nProcessors` set to 2 or more. Hence, if `nProcessors = 2` and processes p0-p2 register, then the following behaviour will occur if the processes invoke the OS methods as indicated below:
    - p0 calls `OS.start` – p0 is allocated a processor, and keeps it until p1 and p2 have terminated
    - p1 calls `OS.start` – p1 is allocated the other processor, the function returns to the caller (p1)
    - p2 calls `OS.start` – p2 is suspended, and added to the end of the ready queue
    - p1 calls `OS.schedule` – p1 is suspended and added to the end of the ready queue; p2 is removed from the head of the ready queue, allocated to the now free processor, scheduled in, and control is returned to p2
    - etc. etc. for p1, p2, p1, p2 until both p1 and p2 call `terminate`, at which point p0 will go through its scheduling `p0, p0 ...`
    - See `ur4_example_test()`.

- **UR5 – Multiple Processes, Single Processor, Multiple Priority Queues**
  - As UR3 except that multiple priorities are used in calls to `os.reg(priority)`. Argument `priority=1` is the highest priority, `priority=2` is the next highest etc. Thus, higher values of this argument indicate lower priority processes.
  - Hence, if p0 and p1 call `os.reg(priority=10)`, and p2 calls `os.reg(priority=20)`, then p0 and p1 will be repeatedly scheduled in FIFO order until both call `OS.terminate`, at which point p2 will be scheduled in.

- **UR6 – Multiple Processes, Multiple Processors, Multiple Priority Queues**
  - Combines requirements of UR4 and UR5.

