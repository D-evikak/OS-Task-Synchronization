package readerswriters;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;

public class RWSemaphore {



    private static final int READER_COUNT = 4;
    private static final int WRITER_COUNT = 2;
    private static final int ROUNDS = 3;

    // Protects the active reader count.
    private static final Semaphore readerMutex = new Semaphore(1);

    // Allows either one writer or a group of readers.
    private static final Semaphore resourceLock = new Semaphore(1);

    private static int activeReaders = 0;
    private static final SharedData data = new SharedData();

    private static void record(
            String name, String state, String message) {
        data.addEvent(name, state, message, data.getValue());
    }

    private static void readerWork(String name) {
        for (int round = 1; round <= ROUNDS; round++) {
            boolean registered = false;
            boolean mutexHeld = false;

            try {
                record(name, "WAITING",
                        "Waiting to read, round " + round);

                readerMutex.acquire();
                mutexHeld = true;

                if (activeReaders == 0) {
                    record(name, "WAITING",
                            "First reader waiting for resource");
                    resourceLock.acquire();
                }

                activeReaders++;
                registered = true;

                record(name, "ENTER_READ",
                        "Entered reading section, round " + round);
                record(name, "READING",
                        "Reading shared resource, round " + round);

                readerMutex.release();
                mutexHeld = false;

                int value = data.readValue();
                data.addEvent(name, "READING",
                        "Read value " + value + ", round " + round,
                        value);

                System.out.println(name + " read value: " + value);
                Thread.sleep(300);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                if (mutexHeld) {
                    readerMutex.release();
                }

                if (registered) {
                    boolean cleanupLocked = false;

                    try {
                        readerMutex.acquire();
                        cleanupLocked = true;

                        activeReaders--;

                        record(name, "EXIT_READ",
                                "Finished reading; active readers: "
                                        + activeReaders);

                        if (activeReaders == 0) {
                            resourceLock.release();

                            record(name, "RESOURCE_AVAILABLE",
                                    "Last reader released the resource");
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        if (cleanupLocked) {
                            readerMutex.release();
                        }
                    }
                }
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void writerWork(String name) {
        for (int round = 1; round <= ROUNDS; round++) {
            boolean lockHeld = false;

            try {
                record(name, "WAITING",
                        "Waiting for exclusive access, round " + round);

                resourceLock.acquire();
                lockHeld = true;

                record(name, "ENTER_WRITE",
                        "Entered writing section, round " + round);

                int value = data.updateValue();

                data.addEvent(name, "WRITING",
                        "Updated value to " + value
                                + ", round " + round,
                        value);

                System.out.println(
                        name + " updated value to: " + value);

                Thread.sleep(400);

                record(name, "EXIT_WRITE",
                        "Finished writing; releasing resource, round "
                                + round);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                if (lockHeld) {
                    resourceLock.release();

                    record(name, "RESOURCE_AVAILABLE",
                            "Writer released the shared resource");
                }
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static String createHtml(String events) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width, initial-scale=1">
                <title>Readers-Writers Semaphore Simulation</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 20px;
                        background: #f4f7fb;
                    }
                    h1 { color: #244b8f; }
                    button {
                        padding: 9px 16px;
                        margin: 5px;
                        cursor: pointer;
                    }
                    #status, #resource {
                        margin: 12px 0;
                        font-weight: bold;
                    }
                    .table-wrap { overflow-x: auto; }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        background: white;
                    }
                    th, td {
                        padding: 9px;
                        border: 1px solid #ccc;
                        text-align: left;
                    }
                    th { background: #244b8f; color: white; }
                    .reading { background: #e1f5e7; }
                    .writing { background: #fff0d8; }
                    .waiting { background: #fff6c9; }
                    .other { background: #e8eafa; }
                </style>
            </head>
            <body>
                <h1>Readers-Writers Semaphore Simulation</h1>
                <p>Events recorded from the actual Java execution.</p>

                <div id="status">Ready</div>
                <div id="resource">Shared resource: Available</div>

                <button onclick="showAll()">Show All Events</button>
                <button onclick="play()">Play</button>
                <button onclick="pause()">Pause</button>
                <button onclick="resetView()">Reset</button>

                <div class="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>Time (ms)</th>
                                <th>Thread</th>
                                <th>State</th>
                                <th>Event</th>
                                <th>Value</th>
                                <th>Resource Status</th>
                            </tr>
                        </thead>
                        <tbody id="rows"></tbody>
                    </table>
                </div>

                <script>
                    const events = EVENTS_PLACEHOLDER;
                    let index = 0;
                    let timer = null;
                    let activeReaders = 0;
                    let resourceStatus = 'Available';

                    function updateResource(e) {
                        if (e.state === 'ENTER_READ') {
                            activeReaders++;
                            resourceStatus =
                                'Being read (' + activeReaders + ')';

                        } else if (e.state === 'EXIT_READ') {
                            activeReaders = Math.max(
                                0, activeReaders - 1
                            );
                            resourceStatus = activeReaders > 0
                                ? 'Being read (' + activeReaders + ')'
                                : 'Available';

                        } else if (e.state === 'ENTER_WRITE') {
                            resourceStatus = 'Being written';

                        } else if (e.state === 'EXIT_WRITE') {
                            // The writer is leaving its critical section.
                            // The semaphore is released immediately after
                            // this event is recorded by the Java program.
                            resourceStatus = 'Writer exiting';

                        } else if (e.state === 'RESOURCE_AVAILABLE') {
                            resourceStatus = activeReaders > 0
                                ? 'Being read (' + activeReaders + ')'
                                : 'Available';
                        }
                    }

                    function addRow(e) {
                        updateResource(e);

                        const row = document.createElement('tr');

                        if (e.state === 'READING'
                                || e.state === 'ENTER_READ') {
                            row.className = 'reading';
                        } else if (e.state === 'WRITING'
                                || e.state === 'ENTER_WRITE') {
                            row.className = 'writing';
                        } else if (e.state === 'WAITING') {
                            row.className = 'waiting';
                        } else {
                            row.className = 'other';
                        }

                        [
                            e.time, e.name, e.state, e.message,
                            e.value, resourceStatus
                        ].forEach(value => {
                            const cell = document.createElement('td');
                            cell.textContent = value;
                            row.appendChild(cell);
                        });

                        document.getElementById('rows')
                            .appendChild(row);

                        document.getElementById('status').textContent =
                            'Event ' + index + ' of ' + events.length;

                        document.getElementById('resource').textContent =
                            'Shared resource: ' + resourceStatus;
                    }

                    function clearView() {
                        document.getElementById('rows').innerHTML = '';
                        index = 0;
                        activeReaders = 0;
                        resourceStatus = 'Available';
                        document.getElementById('resource').textContent =
                            'Shared resource: Available';
                    }

                    function showAll() {
                        pause();
                        clearView();

                        events.forEach(e => {
                            index++;
                            addRow(e);
                        });

                        document.getElementById('status').textContent =
                            'Showing all ' + events.length + ' events';
                    }

                    function play() {
                        pause();

                        if (index >= events.length) {
                            clearView();
                        }

                        timer = setInterval(() => {
                            if (index >= events.length) {
                                pause();
                                document.getElementById('status').textContent =
                                    'Playback complete';
                                return;
                            }

                            const event = events[index];
                            index++;
                            addRow(event);
                        }, 500);
                    }

                    function pause() {
                        if (timer !== null) {
                            clearInterval(timer);
                        }
                        timer = null;
                    }

                    function resetView() {
                        pause();
                        clearView();
                        document.getElementById('status').textContent =
                            'Ready';
                    }
                </script>
            </body>
            </html>
            """.replace("EVENTS_PLACEHOLDER", events);
    }

    private static void saveEventsToJson() throws IOException {
        Path file = Path.of(
                System.getProperty("user.dir"), "events.json");

        Files.writeString(file, data.getEvents());

        System.out.println(
                "JSON saved at: " + file.toAbsolutePath());
    }

    private static void writeHtml() throws IOException {
        Path file = Path.of(
                System.getProperty("user.dir"),
                "ReadersWritersSimulation.html");

        try (FileWriter writer = new FileWriter(file.toFile())) {
            writer.write(createHtml(data.getEvents()));
        }

        System.out.println(
                "HTML generated at: " + file.toAbsolutePath());
    }

    public static void main(String[] args) {
        Thread[] readers = new Thread[READER_COUNT];
        Thread[] writers = new Thread[WRITER_COUNT];

        for (int i = 0; i < READER_COUNT; i++) {
            String name = "Reader-" + (i + 1);
            readers[i] = new Thread(() -> readerWork(name));
            readers[i].start();
        }

        for (int i = 0; i < WRITER_COUNT; i++) {
            String name = "Writer-" + (i + 1);
            writers[i] = new Thread(() -> writerWork(name));
            writers[i].start();
        }

        try {
            for (Thread reader : readers) {
                reader.join();
            }

            for (Thread writer : writers) {
                writer.join();
            }

            System.out.println("All threads completed.");
            System.out.println("Final value: " + data.getValue());

            saveEventsToJson();
            writeHtml();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Main thread interrupted.");
        } catch (IOException e) {
            System.out.println(
                    "Error generating files: " + e.getMessage());
        }
    }
}



   