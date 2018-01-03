// Author: Benjamin Dewey

import java.util.Scanner;
import java.util.Arrays;
import java.io.File;
import java.io.FileNotFoundException;

public class Main {

    private static final int FCFS = 0;
    private static final int RR = 1;
    private static final int SJF = 2;
    private static final int HPRN = 3;

    public static void main(String[] args) {

        String fileName = getFileNameFromArgs(args);
        Scanner sc = getScannerForFileName(fileName);
        Process[] processes = getSortedProcessesFromScanner(sc);
        boolean verbose = getVerboseOption(args);

        printSummary(runBatch(copyProcesses(processes), FCFS, verbose), verbose);
        printSummary(runBatch(copyProcesses(processes), RR, verbose), verbose);
        printSummary(runBatch(copyProcesses(processes), SJF, verbose), verbose);
        printSummary(runBatch(copyProcesses(processes), HPRN, verbose), verbose);
    }

    private static Process[] copyProcesses(Process[] processes) {
        Process[] copy = new Process[processes.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = processes[i].copy();
        }
        return copy;
    }

    private static void printSummary(OutputObject output, boolean verbose) {
        // pick off results
        Process[] processes = output.processes;
        int finishingTime = output.finishingTime;
        int cpuTime = output.cpuTime;
        int ioTime = output.ioTime;

        // print process specific data
        int pid = 0;
        double totalTurnaroundTime = 0;
        double totalWaitingTime = 0;
        for (Process p : processes) {
            totalTurnaroundTime += p.getTurnaroundTime();
            totalWaitingTime += p.getTotalReadyTime();

            if (verbose) { System.out.println(); }
            System.out.println("Process " + pid + ":");
            int A = p.getArrivalTime();
            int B = p.getCPUBurstConstant();
            int C = p.getCPUTimeNeeded();
            int M = p.getIOBurstConstant();
            System.out.println("\t(A,B,C,M) = " + "(" + A + "," + B + "," + C + "," + M + ")");
            System.out.println("\tFinishing time: " + p.getFinishingTime());
            System.out.println("\tTurnaround time: " + p.getTurnaroundTime());
            System.out.println("\tI/O time: " + p.getTotalBlockedTime());
            System.out.println("\tWaiting time: " + p.getTotalReadyTime());
            if (!verbose) { System.out.println(); }
            pid++;
        }

        // print summary data
        double cpuUtilization = cpuTime / (finishingTime * 1.0);
        double ioUtilization = ioTime / (finishingTime * 1.0);
        double throughput = processes.length / (finishingTime / 100.0);
        double aveTurnaroundTime = totalTurnaroundTime / processes.length;
        double aveWaitingTime = totalWaitingTime / processes.length;

        if (verbose) { System.out.println(); }
        System.out.println("Summary Data:");
        System.out.println("\tFinishing time: " + finishingTime);
        System.out.println("\tCPU Utilization: " + cpuUtilization);
        System.out.println("\tI/O Utilization: " + ioUtilization);
        System.out.println("\tThroughput: " + throughput + " processes per hundred cycles");
        System.out.println("\tAverage turnaround time: " + aveTurnaroundTime);
        System.out.println("\tAverage waiting time: " + aveWaitingTime);
        System.out.println("##########################################################################\n");
    }

    private static String getFileNameFromArgs(String[] args) {
        if (args.length == 0) {
            System.out.println("\n\nNo arguments given.\n\n");
            System.exit(0);
        }

        if (args.length > 1) { return args[1]; }

        return args[0];
    }

    private static boolean getVerboseOption(String[] args) {
        return args.length > 1 && args[0].equals("--verbose");
    }

    private static Scanner getScannerForFileName(String fileName) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(fileName));
        } catch(FileNotFoundException ex) {
            System.out.println("\n\nException: Input file not found.\n\n");
            System.exit(0);
        }
        return sc;
    }

    private static Process[] getSortedProcessesFromScanner(Scanner sc) {
        int numOfProcesses = sc.nextInt();
        Process[] processes = new Process[numOfProcesses];

        for (int i = 0; i < numOfProcesses; i++) {
            int A = sc.nextInt();
            int B = sc.nextInt();
            int C = sc.nextInt();
            int M = sc.nextInt();

            processes[i] = new Process(A, B, C, M);
        }

        System.out.print("\nThe original input was: " + numOfProcesses + " ");
        printInputProcesses(processes);

        Arrays.sort(processes);

        System.out.print("The (sorted) input is:  " + numOfProcesses + " ");
        printInputProcesses(processes);

        return processes;
    }

    private static void printInputProcesses(Process[] processes) {
        for (Process p : processes) {
            int A = p.getArrivalTime();
            int B = p.getCPUBurstConstant();
            int C = p.getCPUTimeNeeded();
            int M = p.getIOBurstConstant();
            System.out.print("(" + A + " " + B + " " + C + " " + M + ") ");
        }
        System.out.println();
    }

    private static void printVerboseOutput(Process[] processes, int curSysTime, int schedulingAlgorithm) {
        String extraSpace = "";
        extraSpace += curSysTime > 999 ? "       " : curSysTime > 99 ? "        " : curSysTime > 9 ? "         " : "          ";

        System.out.print("Before cycle" + extraSpace + curSysTime + ":");
        for (Process p : processes) {
            switch (p.getState()) {
                case Process.NONE:
                    System.out.print("   unstarted  0");
                    break;
                case Process.READY:
                    System.out.print("       ready  0");
                    break;
                case Process.RUNNING:
                    int remainingCPUBurst;
                    if (schedulingAlgorithm == RR && p.getCPUBurstTime() > 2) {
                        remainingCPUBurst = 2 - (p.getElapsedCPUBurstTime() % 2);
                    } else {
                        remainingCPUBurst = p.getCPUBurstTime() - p.getElapsedCPUBurstTime();
                    }
                    extraSpace = remainingCPUBurst > 9 ? " " : "  ";
                    System.out.print("     running" + extraSpace + remainingCPUBurst);
                    break;
                case Process.BLOCKED:
                    int remainingIOBurst = p.getIOBurstTime() - p.getElapsedIOBurstTime();
                    extraSpace = remainingIOBurst > 9 ? " " : "  ";
                    System.out.print("     blocked" + extraSpace + remainingIOBurst);
                    break;
                case Process.TERMINATED:
                    System.out.print("  terminated  0");
                    break;
            }
        }
        System.out.println(".");
    }

    private static void printHeader(int schedulingAlgorithm) {
        System.out.println("\n##########################################################################");
        switch (schedulingAlgorithm) {
            case FCFS:
                System.out.println("The scheduling algorithm used was First Come First Served\n");
                break;
            case RR:
                System.out.println("The scheduling algorithm used was Round Robbin\n");
                break;
            case SJF:
                System.out.println("The scheduling algorithm used was Shortest Job First\n");
                break;
            case HPRN:
                System.out.println("The scheduling algorithm used was Highest Penalty Ratio Next\n");
                break;
        }
    }

    private static OutputObject runBatch(Process[] processes, int schedulingAlgorithm, boolean verbose) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File("random-numbers")); // magic file name
        } catch(FileNotFoundException ex) {
            System.out.println("\n\nException: File random-numbers not found.\n\n");
            System.exit(0);
        }

        printHeader(schedulingAlgorithm);

        int curSysTime = 0;
        int ioTime = 0;
        int cpuTime = 0;

        if (verbose) {
            System.out.println("This detailed printout gives the state and remaining burst for each process\n");
            printVerboseOutput(processes, curSysTime, schedulingAlgorithm);
        }

        boolean allTerminated = false;
        while(!allTerminated) {

            // ---------------------------------------- before cycle ---------------------------------------------------

            // put new arrivals into ready state
            for (Process p : processes) { if (p.getArrivalTime() == curSysTime) { p.setStateToReady(); } }

            // schedule the next process to run
            switch (schedulingAlgorithm) {
                case FCFS:
                    runFCFS(processes, sc);
                    break;
                case RR:
                    runRR(processes, sc);
                    break;
                case SJF:
                    runSJF(processes, sc);
                    break;
                case HPRN:
                    runHPRN(processes, sc);
                    break;
            }

            // ----------------------------------------- during cycle --------------------------------------------------

            curSysTime++;

            if (verbose) { printVerboseOutput(processes, curSysTime, schedulingAlgorithm); }

            // cycle each process
            for (Process p : processes) { p.cycle(); }

            // mutate return data
            for (Process p : processes) { if (p.getState() == Process.RUNNING) { cpuTime++; break; } }
            for (Process p : processes) { if (p.getState() == Process.BLOCKED) { ioTime++; break; } }

            // ---------------------------------------- after cycle ----------------------------------------------------

            // block, unblock, or terminate
            for (Process p : processes) {
                int state = p.getState();
                if (state != Process.NONE && state != Process.TERMINATED) {
                    switch (state) {
                        case Process.BLOCKED:
                            if (p.getElapsedIOBurstTime() == p.getIOBurstTime()) { p.setStateToReady(); }
                            break;
                        case Process.RUNNING:
                            if (p.getTotalCPUTime() == p.getCPUTimeNeeded()) { p.setStateToTerminate(); }
                            else if (p.getElapsedCPUBurstTime() == p.getCPUBurstTime()) { p.setStateToBlock(); }
                            break;
                    }
                }
            }

            allTerminated = true;
            for (Process p : processes) { if (p.getState() != Process.TERMINATED) { allTerminated = false; break; } }
        }

        return new OutputObject(processes, curSysTime, cpuTime, ioTime);
    }

    private static void runFCFS(Process[] processes, Scanner sc) {
        for (Process p : processes) { if (p.getState() == Process.RUNNING) { return; } } // cannot schedule

        int i;
        for (i = processes.length - 1; i > 0; i--) { if (processes[i].getState() == Process.READY) { break; } }
        Process selectedProcess = processes[i];

        if (selectedProcess.getState() != Process.READY) { return; } // cannot schedule

        int nextIndex = i - 1;
        while (nextIndex >= 0) {
            if (processes[nextIndex].getState() == Process.READY) {
                if (processes[nextIndex].getReadyStateEntryTime() <= selectedProcess.getReadyStateEntryTime()) {
                    selectedProcess = processes[nextIndex];
                }
            }
            nextIndex--;
        }

        if (selectedProcess.wasPreempted()) { // matters when FCFS is a subroutine of RR
            selectedProcess.setStateToRun();
        } else {
            long random = sc.nextLong();
            long cpuBurstTime = 1 + (random % selectedProcess.getCPUBurstConstant());
            selectedProcess.setStateToRun((int)cpuBurstTime);
        }
    }

    private static void runRR(Process[] processes, Scanner sc) {
        int quantum = 2;

        // preempt
        for (Process p : processes) {
            if (p.getState() == Process.RUNNING && (p.getElapsedCPUBurstTime() % quantum) == 0) {
                p.setStateToReady();
                break;
            }
        }

        // use FCFS as subroutine
        runFCFS(processes, sc);
    }

    private static void runSJF(Process[] processes, Scanner sc) {
        for (Process p : processes) { if (p.getState() == Process.RUNNING) { return; } } // cannot schedule

        int i;
        for (i = processes.length - 1; i > 0; i--) { if (processes[i].getState() == Process.READY) { break; } }
        Process selectedProcess = processes[i];

        if (selectedProcess.getState() != Process.READY) { return; } // cannot schedule

        int nextIndex = i - 1;
        while (nextIndex >= 0) {
            int selectedProcessPriority = selectedProcess.getCPUTimeNeeded() - selectedProcess.getTotalCPUTime();

            if (processes[nextIndex].getState() == Process.READY) {
                int nextProcessPriority = processes[nextIndex].getCPUTimeNeeded() - processes[nextIndex].getTotalCPUTime();

                if (nextProcessPriority <= selectedProcessPriority) {
                    selectedProcess = processes[nextIndex];
                }
            }
            nextIndex--;
        }
        long random = sc.nextLong();
        long cpuBurstTime = 1 + (random % selectedProcess.getCPUBurstConstant());
        selectedProcess.setStateToRun((int)cpuBurstTime);
    }

    private static void runHPRN(Process[] processes, Scanner sc) {
        for (Process p : processes) { if (p.getState() == Process.RUNNING) { return; } } // cannot schedule

        int i;
        for (i = processes.length - 1; i > 0; i--) { if (processes[i].getState() == Process.READY) { break; } }
        Process selectedProcess = processes[i];

        if (selectedProcess.getState() != Process.READY) { return; } // cannot schedule

        int nextIndex = i - 1;
        while (nextIndex >= 0) {
            if (processes[nextIndex].getState() == Process.READY) {
                if (processes[nextIndex].getPenalty() >= selectedProcess.getPenalty()) {
                    selectedProcess = processes[nextIndex];
                }
            }
            nextIndex--;
        }
        long random = sc.nextLong();
        long cpuBurstTime = 1 + (random % selectedProcess.getCPUBurstConstant());
        selectedProcess.setStateToRun((int)cpuBurstTime);
    }
}

class OutputObject {
    Process[] processes;
    int finishingTime, cpuTime, ioTime;

    OutputObject(Process[] processes, int finishingTime, int cpuTime, int ioTime) {
        this.processes = processes;
        this.finishingTime = finishingTime;
        this.cpuTime = cpuTime;
        this.ioTime = ioTime;
    }

}

class Process implements Comparable<Process> {
    static final int NONE = 0;
    static final int READY = 1;
    static final int RUNNING = 2;
    static final int BLOCKED = 3;
    static final int TERMINATED = 4;

    // mutable
    private int curSysTime, totalReadyTime, totalBlockedTime, totalCPUTime;
    private int cpuBurstTime, ioBurstTime, elapsedCPUBurstTime, elapsedIOBurstTime, readyStateEntryTime;
    private int state;
    private boolean wasPreempted;

    // immutable
    private int arrivalTime, cpuTimeNeeded, cpuBurstConstant, ioBurstConstant, finishingTime;

    Process(int arrivalTime, int cpuBurstConstant, int cpuTimeNeeded, int ioBurstConstant) {
        this.arrivalTime = arrivalTime;
        this.cpuTimeNeeded = cpuTimeNeeded;
        this.cpuBurstConstant = cpuBurstConstant;
        this.ioBurstConstant = ioBurstConstant;
        state = NONE;
    }

    int getState() { return state; }
    int getArrivalTime() { return arrivalTime; }
    int getCPUTimeNeeded() { return cpuTimeNeeded; }
    int getCPUBurstConstant() { return cpuBurstConstant; }
    int getIOBurstConstant() { return ioBurstConstant; }
    int getReadyStateEntryTime() { return readyStateEntryTime; }
    int getFinishingTime() { return finishingTime; }
    int getTurnaroundTime() { return finishingTime - arrivalTime; }
    int getTotalReadyTime() { return totalReadyTime; }
    int getTotalBlockedTime() { return totalBlockedTime; }
    int getCPUBurstTime() { return cpuBurstTime; }
    int getElapsedCPUBurstTime() { return elapsedCPUBurstTime; }
    int getIOBurstTime() { return ioBurstTime; }
    int getElapsedIOBurstTime() { return elapsedIOBurstTime; }
    int getTotalCPUTime() { return totalCPUTime; }
    double getPenalty() { return (curSysTime - arrivalTime) / (1.0 >= totalCPUTime ? 1.0 : (double)totalCPUTime); }
    boolean wasPreempted() { return wasPreempted; }

    void setStateToBlock() {
        ioBurstTime = cpuBurstTime * ioBurstConstant;
        elapsedIOBurstTime = 0;
        state = BLOCKED;
    }

    void setStateToReady() {
        readyStateEntryTime = curSysTime;
        wasPreempted = state == RUNNING;
        state = READY;
    }

    void setStateToRun(int cpuBurstTime) {
        if (cpuBurstTime > (cpuTimeNeeded - totalCPUTime)) { cpuBurstTime = (cpuTimeNeeded - totalCPUTime); }
        this.cpuBurstTime = cpuBurstTime;
        elapsedCPUBurstTime = 0;
        state = RUNNING;
    }

    void setStateToRun() {
        state = RUNNING;
    }

    void setStateToTerminate() {
        state = TERMINATED;
        finishingTime = curSysTime;
    }

    void cycle() {

        curSysTime++;

        if (state != NONE && state != TERMINATED) {
            switch (state) {
                case READY:
                    totalReadyTime++;
                    break;
                case BLOCKED:
                    totalBlockedTime++;
                    elapsedIOBurstTime++;
                    break;
                case RUNNING:
                    totalCPUTime++;
                    elapsedCPUBurstTime++;
                    break;
            }
        }
    }

    Process copy() {
        return new Process(arrivalTime, cpuBurstConstant, cpuTimeNeeded, ioBurstConstant);
    }

    @Override
    public int compareTo(Process p) { return Integer.compare(arrivalTime, p.arrivalTime); }
}
