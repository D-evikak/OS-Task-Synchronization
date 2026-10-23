
package osproblems;

public class DiningPhilosophersMonitor {




    private static final int N = 4;
    private static final int ROUNDS = 3;

    private static final int THINKING = 0;
    private static final int HUNGRY = 1;
    private static final int EATING = 2;

    private static final int[] state = new int[N];

    private static final Object monitor = new Object();

    private static void record(int id, String stateName, String message) {
        String name = "Philosopher " + id;
        TraceRecorder.record(name, stateName, message);
    }

    private static int leftFork(int id) {
        return id;
    }

    private static int rightFork(int id) {
        return (id + 1) % N;
    }

    private static int leftNeighbor(int id) {
        return (id + N - 1) % N;
    }

    private static int rightNeighbor(int id) {
        return (id + 1) % N;
    }

    private static void test(int id) {
        if (state[id] == HUNGRY
                && state[leftNeighbor(id)] != EATING
                && state[rightNeighbor(id)] != EATING) {

            state[id] = EATING;

            monitor.notifyAll();
        }
    }

    private static void takeForks(int id) throws InterruptedException {
        synchronized (monitor) {
            state[id] = HUNGRY;

            record(id, "HUNGRY",
                    "Hungry and waiting for forks");

            test(id);

            while (state[id] != EATING) {
                monitor.wait();
                test(id);
            }

            record(id, "HOLDING_FORK",
                    "Picked up fork " + leftFork(id));

            record(id, "HOLDING_FORK",
                    "Picked up fork " + rightFork(id));

            record(id, "EATING",
                    "Eating using forks " + leftFork(id)
                            + " and " + rightFork(id));

            System.out.println("Philosopher " + id
                    + ": Picked up fork " + leftFork(id));

            System.out.println("Philosopher " + id
                    + ": Picked up fork " + rightFork(id));

            System.out.println("Philosopher " + id
                    + ": Eating using forks " + leftFork(id)
                    + " and " + rightFork(id));
        }
    }

    private static void putForks(int id) {
        synchronized (monitor) {
            record(id, "RELEASING_FORK",
                    "Released fork " + rightFork(id));

            record(id, "RELEASING_FORK",
                    "Released fork " + leftFork(id));

            System.out.println("Philosopher " + id
                    + ": Released fork " + rightFork(id));

            System.out.println("Philosopher " + id
                    + ": Released fork " + leftFork(id)
                    + "; finished eating");

            state[id] = THINKING;

            record(id, "THINKING",
                    "Released both forks; finished eating");

            test(leftNeighbor(id));
            test(rightNeighbor(id));

            monitor.notifyAll();
        }
    }

    private static void philosopherWork(int id) {
        try {
            for (int round = 1; round <= ROUNDS; round++) {

                record(id, "THINKING",
                        "Thinking, round " + round);

                System.out.println("Philosopher " + id
                        + ": Thinking, round " + round);

                Thread.sleep(300);

                takeForks(id);

                Thread.sleep(400);

                putForks(id);

                Thread.sleep(200);
            }

            record(id, "COMPLETED", "Completed all rounds");

            System.out.println("Philosopher " + id
                    + ": Completed all rounds");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            record(id, "INTERRUPTED",
                    "Philosopher interrupted");
        }
    }

    public static void main(String[] args) {
        Thread[] philosophers = new Thread[N];

        for (int i = 0; i < N; i++) {
            state[i] = THINKING;
        }

        for (int i = 0; i < N; i++) {
            final int id = i;

            philosophers[i] = new Thread(
                    () -> philosopherWork(id),
                    "Philosopher-" + id);

            philosophers[i].start();
        }

        try {
            for (Thread philosopher : philosophers) {
                philosopher.join();
            }

            System.out.println("All philosophers completed.");

            TraceRecorder.saveFiles(
                    "Dining Philosophers Monitor Simulation",
                    "DiningPhilosophersMonitorSimulation.html");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Main thread interrupted.");

        } catch (java.io.IOException e) {
            System.out.println(
                    "Error generating animation: " + e.getMessage());
        }
    }
}
