// Author: Benjamin Dewey
//
// Description: A Resource Management simulation that simulates both optimistic
// and banker algorithms and prints the results to standard output.

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        String fileName = getFileNameFromArgsOrExit(args);
        Scanner sc = getScannerForFileNameOrExit(fileName);
        ResourceManager rm = getResourceManagerFromScanner(sc);

        // run the simulation
        rm.simulate();
    }

    /**
     * Get a file name from the commandline args.
     *
     * @param args an array of arguments
     * @return the first element in args
     */
    private static String getFileNameFromArgsOrExit(String[] args) {
        if (args.length == 0) {
            System.out.println("\n\nNo arguments given.\n\n");
            System.exit(0);
        }

        return args[0];
    }

    /**
     * Get a Scanner that can read a given file.
     *
     * @param fileName a file name string
     * @return a Scanner that can read the file specified by fileName
     */
    private static Scanner getScannerForFileNameOrExit(String fileName) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(fileName));
        } catch(FileNotFoundException ex) {
            System.out.println("\n\nException: Input file not found.\n\n");
            System.exit(0);
        }
        return sc;
    }

    /**
     * Get a ResourceManager based on data read by a Scanner.
     *
     * @param sc a Scanner
     * @return a ResourceManager
     */
    private static ResourceManager getResourceManagerFromScanner(Scanner sc) {
        int numOfTasks = sc.nextInt();
        int numOfResourceTypes = sc.nextInt();

        // get the tasks
        Task[] optimisticTasks = new Task[numOfTasks];
        Task[] bankerTasks = new Task[numOfTasks];
        for (int i = 0; i < numOfTasks; i++) {
            // initialize Tasks
            optimisticTasks[i] = new Task(i+1, numOfResourceTypes);
            bankerTasks[i] = new Task(i+1, numOfResourceTypes);
        }

        // get the resources
        int[] resources = new int[numOfResourceTypes];
        for (int i = 0; i < numOfResourceTypes; i++) {
            resources[i] = sc.nextInt();
        }

        // add activities to each task
        while(sc.hasNext()) {
            String type = sc.next();
            int taskNum = sc.nextInt() - 1;
            int prop1 = sc.nextInt();
            int prop2 = sc.nextInt();

            Activity optimisticAct = new Activity(type, prop1, prop2);
            Activity bankerAct = new Activity(type, prop1, prop2);

            optimisticTasks[taskNum].addNextActivity(optimisticAct);
            bankerTasks[taskNum].addNextActivity(bankerAct);
        }

        // create the Resource Manager
        return new ResourceManager(resources, optimisticTasks, bankerTasks);
    }
}

/**
 * The Activity class represents an activity for a Task
 */
class Activity {

    /**
     * types of Activities
     */
    static final String INITIATE = "initiate";
    static final String REQUEST = "request";
    static final String RELEASE = "release";
    static final String TERMINATE = "terminate";
    private static final String COMPUTE = "compute";

    /**
     * data that defines what the Activity does
     */
    private String type;
    private int prop1;
    private int prop2;

    /**
     * a reference to the next Activity that occurs after this Activity
     */
    private Activity next;

    /**
     * construct an Activity with a type and two props
     */
    Activity(String type, int prop1, int prop2) {
        this.type = type;
        this.prop1 = prop1;
        this.prop2 = prop2;
    }

    /**
     * Get the type
     */
    String getType() { return type; }

    /**
     * Get the prop1
     */
    int getProp1() { return prop1; }

    /**
     * Get the prop2
     */
    int getProp2() { return  prop2; }

    /**
     * return true if the Activity still needs to compute, false otherwise
     */
    boolean needsToCompute() {
        return type.equals(COMPUTE) && prop1 > 0;
    }

    /**
     * Allow the Activity to compute once
     */
    void compute() {
        prop1--;
    }

    /**
     * Get the next Activity
     */
    Activity getNext() { return next; }

    /**
     * Set the next Activity
     */
    void setNext(Activity a) { next = a; }
}

/**
 * The Task class represents a task for a ResourceManager
 */
class Task {

    /**
     * every possible Task status
     */
    static final int ACTIVE = 1;
    static final int BLOCKED = 2;
    static final int TERMINATED = 3;

    /**
     * data that describe's the Task's history
     */
    private int activeTime;
    private int blockedTime;
    private boolean aborted;

    /**
     * the id of the task
     */
    private int id;

    /**
     * the status of the Task
     */
    private int status;

    /**
     * the resources held by the task
     */
    private int[] resources;

    /**
     * the initial claims of the Task
     */
    private int[] initialClaims;

    /**
     * the head of a linked list of Activities
     */
    private Activity nextActivity;

    /**
     * construct a Task
     */
    Task(int id, int numOfResourceTypes) {
        this.id = id;

        // initialize the size of resources and initialClaims arrays
        resources = new int[numOfResourceTypes];
        initialClaims = new int[numOfResourceTypes];

        // initialize the status to ACTIVE
        status = ACTIVE;
    }

    /**
     * get the Task's id
     */
    int getId() { return id; }

    /**
     * get a clone of the initial claims array
     */
    int[] getInitialClaims() { return initialClaims.clone(); }

    /**
     * get a clone of the resources array
     */
    int[] getResources() { return resources.clone(); }

    /**
     * add resources for a given resource type
     */
    void addResources(int type, int quantity) {
        resources[type - 1] += quantity;
    }

    /**
     * remove resources for a given resource type
     */
    void removeResources(int type, int quantity) {
        resources[type - 1] -= quantity;
    }

    /**
     * get the time spent in the ACTIVE status
     */
    int getActiveTime() { return activeTime; }

    /**
     * get the time spent in the BLOCKED status
     */
    int getBlockedTime() { return blockedTime; }

    /**
     * get the current status
     */
    int getStatus() { return status; }

    /**
     * get the current Activity for the task
     */
    Activity getNextActivity() { return nextActivity; }

    /**
     * add an initiate Activity's claim to the initialClaims array
     */
    private void addInitiateActivity(Activity a) {
        int type = a.getProp1();
        int quantity = a.getProp2();
        initialClaims[type - 1] = quantity;
    }

    /**
     * move the current head Activity one node down the linked list
     */
    private void goToNextActivity() {
        nextActivity = nextActivity.getNext();
    }

    /**
     * add an Activity to the Task by adding it to the linked list of Activities
     */
    void addNextActivity(Activity a) {

        if (a.getType().equals(Activity.INITIATE)) {
            // the Activity is of type INITIATE so save its claim
            addInitiateActivity(a);
        }


        if (nextActivity == null) {
            // the linked list of Activities has no head so initialize the head
            nextActivity = a;
        } else {
            // add the Activity to the tail of the linked list of Activities
            Activity curActivity = nextActivity;
            while (curActivity.getNext() != null) {
                curActivity = curActivity.getNext();
            }

            // curActivity is the tail
            curActivity.setNext(a);
        }
    }

    /**
     * set status to BLOCKED
     */
    void block() { status = BLOCKED; }

    /**
     * set status to ACTIVE
     */
    void activate() { status = ACTIVE; }

    /**
     * set status to TERMINATED
     */
    void terminate() { status = TERMINATED; }

    /**
     * abort the task
     */
    void abort() {
        terminate();

        // record this event
        aborted = true;
    }

    /**
     * determine if the task was ever aborted
     */
    boolean wasAborted() { return aborted; }

    /**
     * allow the task to cycle once
     */
    void cycle() {
        if (status == ACTIVE) {
            if (nextActivity.needsToCompute()) {
                // allow the current Activity to compute once
                nextActivity.compute();
                if (!nextActivity.needsToCompute()) {
                    // the current Activity is done

                    // move on to the next Activity
                    goToNextActivity();
                }
            } else {
                // move on to the next Activity
                goToNextActivity();
            }

            // record time
            activeTime++;
        }

        if (status == BLOCKED) {
            // Task is blocked so do nothing except record time
            blockedTime++;
        }
    }
}

/**
 * The ResourceManager class represents a resource manager that can simulate
 * optimistic and banker resource management algorithms on a set of Tasks
 */
class ResourceManager {

    /**
     * the number of cycles run
     */
    private int cycleNum;

    /**
     * data that represents resources held by the Resource Manager
     */
    private int numOfResourceTypes;
    private int[] resources;
    private int[] releasedResources;

    /**
     * data that represents Tasks the Resource Manager must serve
     */
    private Task[] tasks;
    private Queue<Task> blockedTaskQueue;
    private int numOfBlockedTasks;
    private int numOfTerminatedTasks;

    /**
     * the resources and Tasks to be used during simulation of the optimistic algorithm
     */
    private int[] optimisticResources;
    private Task[] optimisticTasks;

    /**
     * the resources and Tasks to be used during simulation of the banker algorithm
     */
    private int[] bankerResources;
    private Task[] bankerTasks;

    /**
     * constructs a ResourceManager with given resources and separate Tasks for optimistic and banker simulations
     */
    ResourceManager(int[] resources, Task[] optimisticTasks, Task[] bankerTasks) {
        this.optimisticResources = resources.clone();
        this.optimisticTasks = optimisticTasks;

        this.bankerResources = resources.clone();
        this.bankerTasks = bankerTasks;

        numOfResourceTypes = resources.length;
    }

    /**
     * check if a Task's initiate Activity claim is safe, abort the Task if not safe
     */
    private void handleInitiate(Task t, int resourceType, int resourceClaim) {
        if (resources[resourceType - 1] < resourceClaim) {
            abortTask(t);

            String claim = " (" + resourceClaim + ")";
            String units = " (" + resources[resourceType - 1] + ")";
            int id = t.getId();
            String message = "\nBanker aborts task " + id + " before run begins; claim for resource " + resourceType + claim + " exceeds number of units present" + units;
            System.out.println(message);
        }
    }

    /**
     * activate a task
     */
    private void activateTask(Task t) {
        if (t.getStatus() == Task.BLOCKED) {
            numOfBlockedTasks--;
        }
        t.activate();
    }

    /**
     * block a task
     */
    private void blockTask(Task t) {
        t.block();

        // add the task to the back of the blocked task queue
        blockedTaskQueue.add(t);

        numOfBlockedTasks++;
    }

    /**
     * terminate a task
     */
    private void terminateTask(Task t) {
        t.terminate();
        numOfTerminatedTasks++;
    }

    /**
     * abort a task and reclaim any resources it held
     */
    private void abortTask(Task t) {
        t.abort();
        numOfTerminatedTasks++;

        int[] taskResources = t.getResources();
        for (int i = 0; i < taskResources.length; i++) {
            resources[i] += taskResources[i];
        }
    }

    /**
     * for banker's algorithm; check if the current state is safe; verify that there exists some ordering of
     * max-requests by the Tasks such that all Tasks will eventually terminate
     */
    private boolean safe() {

        // the resources available throughout the safety check
        int[] dummyResources = resources.clone();

        // a queue to store the available tasks throughout the safety check
        Queue<Task> queue = new ArrayDeque<>();

        for (Task task: tasks) {
            if (task.getStatus() != Task.TERMINATED) {
                // the Task has not yet terminated so add it to the queue
                queue.add(task);
            }
        }

        // keeps track of how many Tasks could not have their max-requests
        // satisfied during an attempt at finding a safe ordering
        int tries = 0;

        // try to find a safe ordering; if the queue size falls to zero then a safe ordering exists;
        // if the number of tries rises to the queue size then a safe ordering does not exist
        while (queue.size() > 0) {
            // remove the next Task from the queue
            Task task = queue.remove();

            // make an array maxRequests to store the max requests for each resource type
            int[] initialClaims = task.getInitialClaims();
            int[] taskResources = task.getResources();
            int[] maxRequests = new int[initialClaims.length];
            for (int i = 0; i < initialClaims.length; i++) {
                maxRequests[i] = initialClaims[i] - taskResources[i];
            }

            // determine if all max-requests can be satisfied
            boolean canSatisfy = true;
            for (int i = 0; i < maxRequests.length; i++) {
                if (dummyResources[i] < maxRequests[i]) {
                    canSatisfy = false;
                    break;
                }
            }

            if (canSatisfy) {
                // add the Task's resources to the dummyResources as if the Task had terminated
                for (int i = 0; i < maxRequests.length; i++) {
                    dummyResources[i] += taskResources[i];
                }
                // reset tries
                tries = 0;
            } else {
                // the Task could not be satisfied so add it back into the queue
                queue.add(task);
                tries++;
                if (tries >= queue.size()) {
                    // all available Tasks were tried so a safe ordering does not exist
                    return false;
                }
            }
        }

        // the queue size fell to zero so a safe ordering does exist
        return true;
    }

    /**
     * reclaim all resources staged in the releasedResources array
     */
    private void reclaimReleasedResources() {
        for (int i = 0; i < resources.length; i++) {
            resources[i] += releasedResources[i];
            releasedResources[i] = 0; // reset
        }
    }

    /**
     * Try to satisfy a task's request and return true if successful, false otherwise
     *
     * @param resourceType the type of resource requested
     * @param resourceQuantity the amount of a resource requested
     * @param canDeadlock boolean to indicate if deadlock can occur
     */
    private boolean handleRequest(Task task, int resourceType, int resourceQuantity, boolean canDeadlock) {

        if (!canDeadlock) {
            // check that the request is valid given the initial claim
            int maxRequest = task.getInitialClaims()[resourceType - 1] - task.getResources()[resourceType - 1];
            if (resourceQuantity > maxRequest) {
                abortTask(task);

                String cycle = cycleNum + "-" + (cycleNum + 1);
                String message = "\nDuring cycle " + cycle + " of Banker's algorithm Task " + task.getId() + "'s request exceeds its claim; aborted;";
                System.out.println(message);

                return false;
            }
        }

        if (resources[resourceType - 1] >= resourceQuantity) { // can satisfy optimistically
            resources[resourceType - 1] -= resourceQuantity; // remove resources from manager
            task.addResources(resourceType, resourceQuantity); // add resources to task

            if (!canDeadlock && !safe()) {
                // request is not safe so rollback
                resources[resourceType - 1] += resourceQuantity; // add resources from manager
                task.removeResources(resourceType, resourceQuantity); // remove resources from task

                if (task.getStatus() == Task.ACTIVE) { blockTask(task); }
                return false;
            } else if (task.getStatus() == Task.BLOCKED) {
                activateTask(task);
            }

            return true;
        } else { // cannot satisfy
            if (task.getStatus() == Task.ACTIVE) { blockTask(task); }
            return false;
        }
    }

    /**
     * allow a task to release resources and store them in the releasedResources array
     */
    private void handleRelease(Task task, int resourceType, int resourceQuantity) {
        task.removeResources(resourceType, resourceQuantity); // remove resources from the task
        releasedResources[resourceType - 1] += resourceQuantity; // store the released resources
    }

    /**
     * Try to satisfy the pending requests of any and all blocked tasks
     *
     * @param canDeadlock boolean to indicate if deadlock can occur
     */
    private void serveBlockedTasks(boolean canDeadlock) {
        // get the current size of the blockedTaskQueue because it may change over time
        int initialSizeOfQueue = blockedTaskQueue.size();

        for (int i = 0; i < initialSizeOfQueue; i++) {

            // get the next blocked Task from the queue
            Task task = blockedTaskQueue.remove();

            if (task.getStatus() == Task.BLOCKED) { // check here because reactivated tasks don't get auto removed

                // get data that represents the request
                Activity activity = task.getNextActivity();
                int resourceType = activity.getProp1();
                int resourceQuantity = activity.getProp2();

                // try the request
                boolean success = handleRequest(task, resourceType, resourceQuantity, canDeadlock);

                if (!success) {
                    // could not satisfy the request so add it back into the queue
                    blockedTaskQueue.add(task);
                }
            }
        }
    }

    /**
     * Serve an array of provided Tasks
     *
     * @param activeTasks an array of Tasks in the ACTIVE status
     * @param canDeadlock boolean to indicate if deadlock can occur
     */
    private void serveActiveTasks(Task[] activeTasks, boolean canDeadlock) {
        for (Task task : activeTasks) {
            Activity activity = task.getNextActivity();
            String activityType = activity.getType();

            int resourceType = activity.getProp1();
            int resourceQuantity = activity.getProp2();

            switch (activityType) {
                case Activity.REQUEST: // try to satisfy the task's request
                    handleRequest(task, resourceType, resourceQuantity, canDeadlock);
                    break;
                case Activity.RELEASE: // handle the task's release
                    handleRelease(task, resourceType, resourceQuantity);
                    break;
                case Activity.TERMINATE:
                    terminateTask(task);
                    break;
                case Activity.INITIATE:
                    if (!canDeadlock) {
                        // not allowed to ever deadlock so must check the initiate claim
                        handleInitiate(task, resourceType, resourceQuantity);
                    }
                    break;
            }
        }
    }

    /**
     * determine if no pending requests can be satisfied
     */
    private boolean noPendingRequestsCanBeSatisfied() {
        for (Task task : tasks) {
            if (task.getStatus() == Task.BLOCKED) {

                // get data the represents the request
                Activity activity = task.getNextActivity();
                int resourceType = activity.getProp1();
                int resourceQuantity = activity.getProp2();

                if (resources[resourceType - 1] >= resourceQuantity) {
                    // the pending request can be satisfied
                    return false;
                }
            }
        }
        // no pending requests can be satisfied
        return true;
    }

    /**
     * unlock a deadlocked state by aborting Tasks
     */
    private void handleDeadlock() {
        if (numOfBlockedTasks > 0 && numOfBlockedTasks == tasks.length - numOfTerminatedTasks) {
            // the state is deadlocked

            for (Task task : tasks) {
                if (task.getStatus() == Task.BLOCKED) { // check to ignore terminated tasks

                    abortTask(task);
                    numOfBlockedTasks--;

                    if (!noPendingRequestsCanBeSatisfied()) {
                        // deadlock has been unlocked
                        break;
                    }
                }
            }
        }
    }

    /**
     * cause every Task to call their cycle method
     */
    private void cycleTasks() {
        for (Task task : tasks) {
            task.cycle();
        }
    }

    /**
     * get all Tasks with a status of ACTIVE
     */
    private Task[] getActiveTasks() {
        int length = tasks.length - numOfBlockedTasks - numOfTerminatedTasks;
        Task[] activeTasks = new Task[length];

        int index = 0;
        for (Task task : tasks) {
            if (task.getStatus() == Task.ACTIVE) {
                activeTasks[index] = task;
                index++;
            }
        }

        return activeTasks;
    }

    /**
     * simulate a cycle of the Tasks being served by the ResourceManager
     */
    private void cycle(boolean canDeadlock) {
        // get the active tasks before serving blocked tasks
        Task[] activeTasks = getActiveTasks();

        // serve the blocked tasks
        serveBlockedTasks(canDeadlock);

        // serve the active tasks
        serveActiveTasks(activeTasks, canDeadlock);

        if (canDeadlock) {
            // handle deadlock
            handleDeadlock();
        }

        // reclaim released resources
        reclaimReleasedResources();

        // cycle each task
        cycleTasks();

        // increment the cycle number
        cycleNum++;
    }

    /**
     * prepare the ResourceManager's data for a simulation run
     */
    private void prepareRun(int[] resources, Task[] tasks) {
        this.resources = resources;
        this.tasks = tasks;

        releasedResources = new int[numOfResourceTypes];
        blockedTaskQueue = new ArrayDeque<>();
        numOfTerminatedTasks = 0;
        numOfBlockedTasks = 0;
        cycleNum = 0;
    }

    /**
     * run a simulation of the ResourceManager serving its Tasks
     */
    private void run(boolean canDeadlock) {
        while (numOfTerminatedTasks < tasks.length) {
            cycle(canDeadlock);
        }
    }

    /**
     * run the optimistic algorithm simulation on the tasks
     */
    private void runOptimistic() {
        prepareRun(optimisticResources, optimisticTasks);
        run(true);
    }

    /**
     * run the banker's algorithm simulation on the tasks
     */
    private void runBanker() {
        prepareRun(bankerResources, bankerTasks);
        run(false);
    }

    /**
     * start the ResourceManager's simulation of the tasks using optimistic and banker algorithms
     */
    void simulate() {
        runOptimistic();
        runBanker();
        printResults();
    }



    // ----------------- METHODS BELOW ARE FOR PRETTY PRINTING THE RESULTS OF A SIMULATION -----------------

    /**
     * get a String that represents the history of a Task
     */
    private String getHistoryStringForTask(Task task) {
        String data;
        if (task.wasAborted()) {
            data = "aborted";
        } else {
            int waitingTime = task.getBlockedTime();
            int time = task.getActiveTime() + waitingTime;
            long percentWaitingTime = Math.round((100.0 * waitingTime) / time);
            data = time + "   " + waitingTime + "   " + percentWaitingTime + "%";
        }
        return data;
    }

    /**
     * get the overall amount of time spent by Tasks
     */
    private int getOverallTimeForTasks(Task[] tasks) {
        int overallTime = 0;
        for (Task t : tasks) {
            if (!t.wasAborted()) {
                overallTime += t.getActiveTime() + t.getBlockedTime();
            }
        }
        return overallTime;
    }

    /**
     * get the overall amount of time Tasks spent waiting
     */
    private int getOverallWaitingTimeForTasks(Task[] tsks) {
        int overallWaitingTime = 0;
        for (Task t : tsks) {
            if (!t.wasAborted()) {
                overallWaitingTime += t.getBlockedTime();
            }
        }
        return overallWaitingTime;
    }

    /**
     * print the results of both optimistic and banker simulations
     */
    private void printResults() {
        System.out.println();

        System.out.println("                      FIFO                    BANKER'S");
        for (int i = 0; i < tasks.length; i++) {
            Task optTask = optimisticTasks[i];
            Task bankTask = bankerTasks[i];

            String optTaskHistory = getHistoryStringForTask(optTask);
            String bankTaskHistory = getHistoryStringForTask(bankTask);
            String taskName = "Task " + (i + 1);
            String optTaskString = "       " + taskName + "      " + optTaskHistory;
            String bankTaskString = "       " + taskName + "      " + bankTaskHistory;

            int numOfSpacesBetween = 32 - optTaskString.length();
            StringBuilder spacesBetween = new StringBuilder();
            while (numOfSpacesBetween > 0) {
                spacesBetween.append(" ");
                numOfSpacesBetween--;
            }
            System.out.println(optTaskString + spacesBetween + bankTaskString);
        }

        int overallTime = getOverallTimeForTasks(optimisticTasks);
        int overallWaitingTime = getOverallWaitingTimeForTasks(optimisticTasks);
        long overallPercentWaitingTime = Math.round((100.0 * overallWaitingTime) / overallTime);
        String space = overallTime > 9 ? "" : " ";
        String optTotal = "       total      " + space + overallTime + "   " + overallWaitingTime + "   " + overallPercentWaitingTime + "%";

        overallTime = getOverallTimeForTasks(bankerTasks);
        overallWaitingTime = getOverallWaitingTimeForTasks(bankerTasks);
        overallPercentWaitingTime = Math.round((100.0 * overallWaitingTime) / overallTime);
        space = overallTime > 9 ? "" : " ";
        String bankTotal = "       total      "+ space + overallTime + "   " + overallWaitingTime + "   " + overallPercentWaitingTime + "%";

        int numOfSpacesBetween = 32 - optTotal.length();
        StringBuilder spacesBetween = new StringBuilder();
        while (numOfSpacesBetween > 0) {
            spacesBetween.append(" ");
            numOfSpacesBetween--;
        }
        System.out.println(optTotal + spacesBetween + bankTotal);

        System.out.println();
    }
}
