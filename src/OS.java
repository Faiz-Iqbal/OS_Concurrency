import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class OS implements OS_sim_interface {
	private Map<Integer, Integer> pidPriority;
	private int numberOfProcessors;
	private int nextPID = 0;
	private LinkedList<Integer> readyQueue;
	private boolean[] processorStatus;
	private ReentrantLock lock;
	private Condition condition;
	private int[] currentRunningProcesses;

	/**
	 * Constructs a new OS instance. Initializes the process ID to priority mapping,
	 * the ready queue for processes awaiting execution, and the synchronization
	 * mechanisms (lock and condition) used for thread-safe operation.
	 */
	public OS() {
		pidPriority = new HashMap<>(); // Map to hold process IDs and their priorities.
		readyQueue = new LinkedList<>(); // Queue for processes waiting to be executed.
		lock = new ReentrantLock(); // Lock for thread-safe operations.
		condition = lock.newCondition(); // Condition for managing process wait/notify.
	}

	//Helper Functions
	/**
	 * Sets the number of processors available in the system and initializes their status and
	 * current running processes. Each processor is initially marked as idle and not running any process.
	 *
	 * @param numOfProcessors the total number of processors to be set for the system.
	 */
	@Override
	public void set_number_of_processors(int numOfProcessors) {
		// Set the total number of processors available for processing.
		this.numberOfProcessors = numOfProcessors;

		// Initialize the processor availability status array, indicating if each processor is available (false) or busy (true).
		processorStatus = new boolean[numOfProcessors];

		// Initialize the array to keep track of which process is running on each processor. A value of -1 indicates no process is currently assigned.
		currentRunningProcesses = new int[numOfProcessors];

		// Loop through each processor to set its initial status to available (false) and assign no process to it (-1).
		for (int i = 0; i < numOfProcessors; i++) {
			processorStatus[i] = false; // Mark processor as available.
			currentRunningProcesses[i] = -1; // No process is currently running on this processor.
		}

	}

	/**
	 * Registers a new process with the operating system, assigning it a unique process ID (PID) and
	 * setting its priority. The process ID is auto-incremented to ensure uniqueness. The priority
	 * of the process determines its scheduling preference relative to other processes, with higher
	 * values indicating higher priority.
	 *
	 * @param processPriority The priority of the new process, where a higher value indicates a higher priority.
	 * @return The unique process ID (PID) assigned to the newly registered process.
	 */
	@Override
	public int reg(int processPriority) {
		// Increment the next process ID and assign it to the new process.
		int processID = nextPID++;

		// Map the new process ID to its given priority in the pidPriority map.
		pidPriority.put(processID, processPriority);

		// Return the unique process ID assigned to the new process.
		return processID;

	}


	/**
	 * Attempts to start the execution of a process by allocating it to an available processor. If all processors
	 * are currently busy, the process is added to the end of the ready queue and waits until a processor becomes available.
	 * The method ensures thread-safe operation through the use of a lock, and processes in the ready queue are
	 * made to wait using a condition variable. If a processor is successfully allocated, the method updates the
	 * processor's status and records the process ID as currently running on that processor.
	 *
	 * @param processID The unique identifier of the process to be started. This ID should correspond to a process
	 *                  previously registered with the system.
	 * @throws InterruptedException if the current thread is interrupted while waiting for a processor to become available.
	 */
	@Override
	public void start(int processID) {
		// Acquire the lock to ensure thread-safe access to shared resources.
		lock.lock();
		try {
			// Flag to track if a processor has been allocated to the process.
			boolean processorAllocated = false;

			// Iterate over all processors to find an available one.
			for (int i = 0; i < numberOfProcessors; i++) {
				// Check if the current processor is available.
				if (!processorStatus[i]) {
					// Mark the processor as busy and assign the process to it.
					processorStatus[i] = true;
					currentRunningProcesses[i] = processID;
					processorAllocated = true; // Update the flag as the process is now allocated.
					break; // Exit the loop as we've successfully allocated the processor.
				}
			}

			// If no processor was available, add the process to the ready queue and wait.
			if (!processorAllocated) {
				readyQueue.addLast(processID); // Add process to the end of the ready queue.
				condition.await(); // Wait until a processor becomes available.
			}
		} catch (InterruptedException e) {
			// Handle any interruption during waiting.
			e.printStackTrace();
		} finally {
			// Always release the lock to ensure other threads can access shared resources.
			lock.unlock();
		}

	}

	/**
	 * Schedules the specified process for execution by potentially preempting it in favor of another process from
	 * the ready queue. If the process is currently running on a processor and there are other processes waiting in the
	 * ready queue, it is replaced with the next process in the queue. The preempted process is then placed back into the
	 * ready queue. This method employs a locking mechanism to ensure that the scheduling operation is thread-safe.
	 * Processes waiting for a processor are managed using a condition variable, allowing for efficient context switching.
	 *
	 * @param processID The unique identifier of the process to be scheduled. This ID must correspond to a process
	 *                  that is currently running or previously registered.
	 * @throws InterruptedException If the thread is interrupted while waiting due to the use of a condition variable.
	 *                              This exception must be handled by the caller.
	 */
	@Override
	public void schedule(int processID) {
		// Acquire the lock to ensure exclusive access to modify processor and queue states.
		lock.lock();
		try {
			// Iterate through all processors to find the one running the specified process.
			for (int i = 0; i < numberOfProcessors; i++) {
				// Check if the current processor is running the specified process.
				if (processID == currentRunningProcesses[i]) {
					// Check if there are processes waiting in the ready queue.
					if (!readyQueue.isEmpty()) {
						// Remove the first process from the ready queue to run next.
						int nextProcess = readyQueue.removeFirst();
						// Assign the next process to the current processor.
						currentRunningProcesses[i] = nextProcess;
						// Signal any waiting threads that a processor status change has occurred.
						condition.signal();
						// Add the current process back to the end of the ready queue.
						readyQueue.addLast(processID);
						// Wait until the process can be reassigned to a processor.
						condition.await();
					}
					// Break from the loop once the specified process is found and handled.
					break;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			// Release the lock to allow other threads to access the shared resources.
			lock.unlock();
		}
	}

	/**
	 * Terminates the execution of the specified process. If the process is currently running on a processor,
	 * it checks if there are other processes in the ready queue. If the queue is not empty, the next process in
	 * the queue is assigned to the now-available processor. Otherwise, the processor is marked as idle. This method
	 * uses a lock to ensure thread-safe modification of shared resources. A condition signal is sent if a new
	 * process is started from the ready queue to potentially wake up threads waiting for processor availability.
	 *
	 * @param processID The unique identifier of the process to be terminated. This ID must correspond to a process
	 *                  that is currently running.
	 */
	@Override
	public void terminate(int processID) {
		// Acquire the lock to ensure thread-safe modifications to the process and processor states.
		lock.lock();
		try {
			// Iterate through all processors to find and terminate the specified process.
			for (int i = 0; i < numberOfProcessors; i++) {
				// Check if the current processor is running the process to be terminated.
				if (processID == currentRunningProcesses[i]) {
					// If there are processes waiting in the queue, allocate the next process to this processor.
					if (!readyQueue.isEmpty()) {
						// Remove the first process from the ready queue to run it next on this processor.
						int nextProcess = readyQueue.removeFirst();
						currentRunningProcesses[i] = nextProcess; // Assign the new process.
						condition.signal(); // Signal a waiting process that it can start.
					} else {
						// If the ready queue is empty, mark the processor as idle.
						processorStatus[i] = false; // Mark processor as available.
						currentRunningProcesses[i] = -1; // No process is currently running on this processor.
					}
					// Process found and handled, no need to check further processors.
					break;
				}
			}
		} finally {
			//release the lock
			lock.unlock();
		}

	}
}