import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Driver driver = new Driver(args);
        driver.echoParams();
        driver.simulate();
    }
}

class Driver {

    // indexes for frame arrays
    private final int PID_INDEX = 0;
    private final int PAGE_INDEX = 1;
    private final int RECENT_SCORE_INDEX = 2;

    private int MACHINE_SIZE;
    private int PAGE_SIZE;
    private int PROCESS_SIZE;
    private int JOB_MIX;
    private int NUM_OF_REFERENCES;
    private int NUM_OF_FRAMES;
    private String REPLACEMENT_ALGORITHM;

    // process in circular linked list of processes
    private Process process;

    private int[][] frameTable;
    private int highestFreeFrame;
    private int NUM_OF_PROCESSES;

    private Scanner randScanner;

    Driver(String[] params) {
        try {
            randScanner = new Scanner(new File("random-numbers")); // magic file name
        } catch(FileNotFoundException ex) {
            System.out.println("Failed to find file 'random-numbers'");
            System.exit(0);
        }

        MACHINE_SIZE = Integer.parseInt(params[0]);
        PAGE_SIZE = Integer.parseInt(params[1]);
        PROCESS_SIZE = Integer.parseInt(params[2]);
        JOB_MIX = Integer.parseInt(params[3]);
        NUM_OF_REFERENCES = Integer.parseInt(params[4]);
        REPLACEMENT_ALGORITHM = params[5];

        initFrameTable();
        initProcesses(JOB_MIX);
    }

    private class Process {
        Process next;
        int word;
        int pid;
        double A;
        double B;
        double C;

        int references;

        Process(int pid, double A, double B, double C) {
            this.pid = pid;
            this.A = A;
            this.B = B;
            this.C = C;

            this.word = (111 * pid) % PROCESS_SIZE;
        }

        Process link(Process p) {
            next = p;
            return this;
        }
    }

    private void initFrameTable() {
        NUM_OF_FRAMES = MACHINE_SIZE / PAGE_SIZE;
        highestFreeFrame = NUM_OF_FRAMES - 1;
        frameTable = new int[NUM_OF_FRAMES][3];
    }

    private void initProcesses(int jobMix) {
        NUM_OF_PROCESSES = 4;

        Process p1;
        Process p2;
        Process p3;
        Process p4;

        switch (jobMix) {
            case 1:
                p1 = new Process(1, 1, 0, 0);
                process = p1.link(p1);
                NUM_OF_PROCESSES = 1;
                break;
            case 2:
                p1 = new Process(1, 1, 0, 0);
                p2 = new Process(2, 1, 0, 0);
                p3 = new Process(3, 1, 0, 0);
                p4 = new Process(4, 1, 0, 0);
                process = p1.link(p2.link(p3.link(p4.link(p1))));
                break;
            case 3:
                p1 = new Process(1, 0, 0, 0);
                p2 = new Process(2, 0, 0, 0);
                p3 = new Process(3, 0, 0, 0);
                p4 = new Process(4, 0, 0, 0);
                process = p1.link(p2.link(p3.link(p4.link(p1))));
                break;
            case 4:
                p1 = new Process(1, .75, .25, 0);
                p2 = new Process(2, .75, 0, .25);
                p3 = new Process(3, .75, .125, .125);
                p4 = new Process(4, .5, .125, .125);
                process = p1.link(p2.link(p3.link(p4.link(p1))));
                break;
        }
    }

    private int nextWord() {
        double A = process.A;
        double B = process.B;
        double C = process.C;
        int word = process.word;

        long r = randScanner.nextLong();
        double y = r / (Integer.MAX_VALUE + 1d);

        if (y < A) {
            return (word + 1) % PROCESS_SIZE;
        } else if (y < A + B) {
            return (word - 5 + PROCESS_SIZE) % PROCESS_SIZE;
        } else if (y < A + B + C) {
            return (word + 4) % PROCESS_SIZE;
        } else {
            return (int)randScanner.nextLong() % PROCESS_SIZE;
        }
    }

    void simulate() {
        final String LRU = "lru";
        final String LIFO = "lifo";
        final String RANDOM = "random";
        final int QUANTUM = 3;

        int[] faults = new int[NUM_OF_PROCESSES];
        int[] evictions = new int[NUM_OF_PROCESSES];
        int[] residencies = new int[NUM_OF_PROCESSES];
        int[][] loadTimes = new int[NUM_OF_PROCESSES][PROCESS_SIZE / PAGE_SIZE];

        int lastFrameIndex = 0;

        int totalReferences = NUM_OF_REFERENCES * NUM_OF_PROCESSES;
        int references = 0;

        while (references < totalReferences) {
            int pid = process.pid;
            int page = process.word / PAGE_SIZE;

            // look for a hit
            boolean fault = true;
            for (int i = frameTable.length - 1; i >= 0; i--) {
                int[] frame = frameTable[i];
                int framePid = frame[PID_INDEX];
                int framePage = frame[PAGE_INDEX];

                if (framePid == pid && framePage == page) { // hit
                    frame[RECENT_SCORE_INDEX] = references + 1;
                    fault = false;

                    break;
                }
            }

            if (fault) {
                faults[pid - 1]++;
                int[] entry = {pid, page, references + 1};

                if (highestFreeFrame >= 0) {
                    lastFrameIndex = highestFreeFrame;
                    frameTable[highestFreeFrame] = entry;
                    highestFreeFrame--;
                } else { // evict
                    switch (REPLACEMENT_ALGORITHM) {
                        case LRU:
                            lru(entry, references, evictions, residencies, loadTimes);
                            break;
                        case LIFO:
                            lifo(entry, references, evictions, residencies, loadTimes, lastFrameIndex);
                            break;
                        case RANDOM:
                            random(entry, references, evictions, residencies, loadTimes);
                            break;
                    }
                }

                // save load time of the loaded page
                loadTimes[pid - 1][page] = references + 1;
            }

            process.word = nextWord();
            process.references++;

            if ((process.references % QUANTUM) == 0 || process.references >= NUM_OF_REFERENCES) {
                process = process.next;
            }

            references++;
        }

        printResults(faults, evictions, residencies);
    }

    private void lru(int[] entry, int references, int[] evictions, int[] residencies, int[][] loadTimes) {
        int index = 0;
        int[] leastRecentFrame = frameTable[index];
        for (int i = 0; i < frameTable.length; i++) {
            int[] frame = frameTable[i];
            if (frame[RECENT_SCORE_INDEX] < leastRecentFrame[RECENT_SCORE_INDEX]) {
                leastRecentFrame = frame;
                index = i;
            }
        }

        int lrf = leastRecentFrame[PID_INDEX] - 1;
        int lrfPage = leastRecentFrame[PAGE_INDEX];
        evictions[lrf]++;
        residencies[lrf] += (references + 1) - loadTimes[lrf][lrfPage];

        frameTable[index] = entry;
    }

    private void lifo(int[] entry, int references, int[] evictions, int[] residencies, int[][] loadTimes, int lastFrameIndex) {
        int[] lastFrameIn = frameTable[lastFrameIndex];
        int lfi = lastFrameIn[PID_INDEX] - 1;
        int lfiPage = lastFrameIn[PAGE_INDEX];
        evictions[lfi]++;
        residencies[lfi] += (references + 1) - loadTimes[lfi][lfiPage];

        frameTable[lastFrameIndex] = entry;
    }

    private void random(int[] entry, int references, int[] evictions, int[] residencies, int[][] loadTimes) {
        int randFrameIndex = (int)(randScanner.nextLong() % NUM_OF_FRAMES);
        int[] randFrame = frameTable[randFrameIndex];
        int rf = randFrame[PID_INDEX] - 1;
        int rfPage = randFrame[PAGE_INDEX];
        evictions[rf]++;
        residencies[rf] += (references + 1) - loadTimes[rf][rfPage];

        frameTable[randFrameIndex] = entry;
    }

    private void printResults(int [] faults, int[] evictions, int[] residencies) {
        System.out.println();
        int totalFaults = 0;
        int totalEvictions = 0;
        double totalRes = 0;

        for (int i = 0; i < NUM_OF_PROCESSES; i++) {
            int faultNum = faults[i];
            totalFaults += faultNum;

            int evictionNum = evictions[i];
            totalEvictions += evictionNum;

            String aveRes;
            if (evictionNum != 0) {
                totalRes += residencies[i];
                aveRes = "and " + (residencies[i] / (double)evictionNum) + " average residency";
            } else {
                aveRes = "and average residency is undefined";
            }
            System.out.println("Process " + (i + 1) + " had " + faultNum + " faults " + aveRes);
        }

        String overallAveRes;
        if (totalEvictions == 0) {
            overallAveRes = " and the overall average residence is undefined.";
        } else {
            overallAveRes = " and the overall average residency is " + (totalRes / totalEvictions);
        }

        System.out.println("\nThe total number of faults is " + totalFaults + overallAveRes + "\n");
    }

    private String makeStringForParam(String description, Object param) {
        return description + " " + param + ".\n";
    }

    void echoParams() {
        StringBuilder message = new StringBuilder();
        message.append("\n");
        message.append(makeStringForParam("The machine size is", MACHINE_SIZE));
        message.append(makeStringForParam("The page size is", PAGE_SIZE));
        message.append(makeStringForParam("The process size is", PROCESS_SIZE));
        message.append(makeStringForParam("The job mix number is", JOB_MIX));
        message.append(makeStringForParam("The number of references per process is", NUM_OF_REFERENCES));
        message.append(makeStringForParam("The replacement algorithm is", REPLACEMENT_ALGORITHM));

        System.out.println(message);
    }
}
