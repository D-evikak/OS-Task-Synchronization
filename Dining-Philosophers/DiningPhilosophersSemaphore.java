package osproblems;


import java.io.IOException;
import java.util.concurrent.Semaphore;

public class DiningPhilosophersSemaphore {


    private static final int N = 4;
    private static final int ROUNDS = 3;

    private static final Semaphore[] forks = new Semaphore[N];
    private static final Semaphore room = new Semaphore(N - 1);

    static {
        for (int i = 0; i < N; i++) {
            forks[i] = new Semaphore(1);
        }
    }

    private static void log(int id, String state, String message) {
        TraceRecorder.record("Philosopher-" + id, state, message);
        System.out.println("Philosopher " + id + ": " + message);
    }

    private static void philosopher(int id) {
        int left = id;
        int right = (id + 1) % N;

        for (int round = 1; round <= ROUNDS; round++) {
            log(id, "THINKING", "Thinking, round " + round);

            try {
                Thread.sleep(200);

                log(id, "HUNGRY", "Hungry and waiting for forks");
                room.acquire();

                forks[left].acquire();
                log(id, "HOLDING_FORK", "Picked up fork " + left);

                forks[right].acquire();
                log(id, "EATING", "Eating using forks "
                        + left + " and " + right);

                Thread.sleep(300);

                forks[right].release();
                log(id, "RELEASING_FORK", "Released fork " + right);

                forks[left].release();
                log(id, "THINKING", "Released fork " + left
                        + "; finished eating");

                room.release();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        log(id, "FINISHED", "Completed all rounds");
    }

    public static void main(String[] args) {
        Thread[] philosophers = new Thread[N];

        for (int i = 0; i < N; i++) {
            final int id = i;
            philosophers[i] = new Thread(() -> philosopher(id));
            philosophers[i].start();
        }

        try {
            for (Thread p : philosophers) {
                p.join();
            }

            System.out.println("All philosophers completed.");

            TraceRecorder.saveFiles(
                    "Dining Philosophers Semaphore Simulation",
                    "DiningPhilosophersSimulation.html");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Main thread interrupted.");
        } catch (IOException e) {
            System.out.println("Error generating files: " + e.getMessage());
        }
    }
}
