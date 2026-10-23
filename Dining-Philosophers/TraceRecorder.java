package osproblems;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class TraceRecorder {


    private static final List<String> events = new ArrayList<>();
    private static final long startTime = System.nanoTime();

    private static final Path OUTPUT_DIR =
            Path.of(System.getProperty("user.dir"), "generated_html");

    public static synchronized void record(
            String name, String state, String message) {

        long time = (System.nanoTime() - startTime) / 1_000_000;

        String event = "{\"time\":" + time
                + ",\"name\":\"" + escape(name)
                + "\",\"state\":\"" + escape(state)
                + "\",\"message\":\"" + escape(message) + "\"}";

        events.add(event);
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }

    private static synchronized String getEvents() {
        return "[" + String.join(",", events) + "]";
    }

    public static void saveFiles(String title, String htmlFileName)
            throws IOException {

        Files.createDirectories(OUTPUT_DIR);

        String json = getEvents();

        Path jsonFile = OUTPUT_DIR.resolve("dining_events.json");
        Files.writeString(jsonFile, json, StandardCharsets.UTF_8);
        System.out.println("JSON saved at: " + jsonFile.toAbsolutePath());

        String html = createHtml(title, json);

        Path htmlFile = OUTPUT_DIR.resolve(htmlFileName);
        Files.writeString(htmlFile, html, StandardCharsets.UTF_8);
        System.out.println("HTML generated at: " + htmlFile.toAbsolutePath());
    }

    private static String createHtml(String title, String eventsJson) {
        String safeTitle = title.replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;");

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>TITLE_PLACEHOLDER</title>
            <style>
                * { box-sizing: border-box; }
                body { margin: 0; padding: 12px; font-family: Arial, sans-serif;
                       background: #f0f4f9; color: #152b4d; }
                h1 { font-size: 23px; margin: 5px 0; }
                h2 { font-size: 16px; margin-top: 0; }
                .subtitle { font-size: 12px; color: #60718a; }
                .panel { background: white; border: 1px solid #d6e0ee;
                         border-radius: 9px; padding: 12px; margin: 10px 0; }
                button, select { padding: 6px 10px; margin: 3px; cursor: pointer; }
                input[type=range] { width: 100%; }
                #status { font-size: 12px; margin-top: 6px; }
                .scene { position: relative; height: 300px; max-width: 550px; margin: auto; }
                .table { position: absolute; width: 160px; height: 160px;
                         left: 50%; top: 50%; transform: translate(-50%, -50%);
                         border-radius: 50%; border: 5px solid #8b582d;
                         background: radial-gradient(circle, #cfa16b, #b17d47);
                         box-shadow: inset 0 0 0 3px #d9b080, 0 3px 7px #bbb;
                         display: flex; justify-content: center; align-items: center;
                         color: white; font-weight: bold; text-align: center; }
                .person { position: absolute; width: 76px; min-height: 53px;
                          padding: 8px 3px; background: #64758e; color: white;
                          border-radius: 8px; text-align: center; font-size: 10px;
                          font-weight: bold; box-shadow: 0 2px 5px #ccc; z-index: 2; }
                .person small { display: block; margin-top: 4px; }
                #p0 { top: 5px; left: 50%; transform: translateX(-50%); }
                #p1 { right: 2px; top: 50%; transform: translateY(-50%); }
                #p2 { bottom: 5px; left: 50%; transform: translateX(-50%); }
                #p3 { left: 2px; top: 50%; transform: translateY(-50%); }
                .fork { position: absolute; z-index: 3; width: 28px; height: 28px;
                        border-radius: 50%; border: 1px solid #cbd5e1;
                        background: white; text-align: center; padding-top: 5px;
                        font-size: 13px; box-shadow: 0 2px 4px #ccc; }
                #f0 { left: 36%; top: 29%; }
                #f1 { right: 36%; top: 29%; }
                #f2 { right: 36%; bottom: 29%; }
                #f3 { left: 36%; bottom: 29%; }
                .fork-status { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 5px; }
                .fork-row { border: 1px solid #d6e0ee; padding: 7px; border-radius: 5px; font-size: 11px; }
                .thinking { background: #64758e; }
                .eating { background: #27834b; }
                .hungry { background: #bb8b24; }
                .finished { background: #397d8d; }
                #log { font-size: 11px; line-height: 1.7; padding-left: 22px; }
                @media(max-width: 420px) {
                    .scene { height: 260px; }
                    .table { width: 130px; height: 130px; }
                    .person { width: 66px; }
                }
            </style>
            </head>
            <body>
                <h1>TITLE_PLACEHOLDER</h1>
                <p class="subtitle">Semaphore-based solution: four philosophers and four forks.</p>

                <div class="panel">
                    <button onclick="play()">Play</button>
                    <button onclick="pause()">Pause</button>
                    <button onclick="step()">Step</button>
                    <button onclick="resetView()">Reset</button>
                    Speed:
                    <select id="speed">
                        <option value="1000">Slow</option>
                        <option value="500" selected>Normal</option>
                        <option value="200">Fast</option>
                    </select>
                    <input id="progress" type="range" min="0" max="0" value="0"
                           oninput="seek(this.value)">
                    <div id="status">Ready</div>
                </div>

                <div class="panel">
                    <h2>Round Table Simulation</h2>
                    <div class="scene">
                        <div class="table">ROUND<br>TABLE</div>
                        <div class="person" id="p0">Philosopher P0<small>THINKING</small></div>
                        <div class="person" id="p1">Philosopher P1<small>THINKING</small></div>
                        <div class="person" id="p2">Philosopher P2<small>THINKING</small></div>
                        <div class="person" id="p3">Philosopher P3<small>THINKING</small></div>
                        <div class="fork" id="f0">🍴</div>
                        <div class="fork" id="f1">🍴</div>
                        <div class="fork" id="f2">🍴</div>
                        <div class="fork" id="f3">🍴</div>
                    </div>
                </div>

                <div class="panel">
                    <h2>Fork Status</h2>
                    <div class="fork-status" id="forkStatus"></div>
                </div>

                <div class="panel">
                    <h2>Execution Event Log</h2>
                    <ol id="log"></ol>
                </div>

                <script>
                    const events = EVENTS_PLACEHOLDER;
                    let index = 0;
                    let timer = null;
                    const owners = [-1, -1, -1, -1];

                    function numberFromName(name) {
                        const m = String(name).match(/(\\d+)/);
                        return m ? Number(m[1]) : -1;
                    }

                    function renderForks() {
                        const area = document.getElementById('forkStatus');
                        area.innerHTML = '';
                        for (let i = 0; i < 4; i++) {
                            const div = document.createElement('div');
                            div.className = 'fork-row';
                            div.textContent = '🍴 Fork ' + i + ': ' +
                                (owners[i] === -1 ? 'Available' :
                                'Held by Philosopher ' + owners[i]);
                            area.appendChild(div);
                            document.getElementById('f' + i).style.background =
                                owners[i] === -1 ? 'white' : '#ffc77d';
                        }
                    }

                    function draw(e) {
                        const n = numberFromName(e.name);
                        const state = String(e.state).toUpperCase();
                        const message = String(e.message);

                        if (n >= 0 && n < 4) {
                            const p = document.getElementById('p' + n);
                            p.innerHTML = 'Philosopher P' + n +
                                '<small>' + state + '</small>';
                            p.className = 'person ' + state.toLowerCase();
                        }

                        const m = message.match(/fork\\s+(\\d+)/i);
                        if (m) {
                            const f = Number(m[1]);
                            if (f >= 0 && f < 4) {
                                if (/picked up/i.test(message)) owners[f] = n;
                                if (/released/i.test(message)) owners[f] = -1;
                            }
                        }

                        renderForks();

                        const li = document.createElement('li');
                        li.textContent = e.name + ' - ' + e.state + ': ' + e.message;
                        document.getElementById('log').appendChild(li);
                        document.getElementById('status').textContent =
                            'Event ' + index + ' of ' + events.length + ' | ' + e.time + ' ms';
                        document.getElementById('progress').value = index;
                    }

                    function pause() {
                        if (timer !== null) clearInterval(timer);
                        timer = null;
                    }

                    function play() {
                        pause();
                        if (index >= events.length) resetView();
                        timer = setInterval(() => {
                            if (index >= events.length) {
                                pause();
                                document.getElementById('status').textContent = 'Playback complete';
                                return;
                            }
                            draw(events[index++]);
                        }, Number(document.getElementById('speed').value));
                    }

                    function step() {
                        pause();
                        if (index < events.length) draw(events[index++]);
                    }

                    function resetView() {
                        pause();
                        index = 0;
                        owners.fill(-1);
                        document.getElementById('log').innerHTML = '';
                        for (let i = 0; i < 4; i++) {
                            const p = document.getElementById('p' + i);
                            p.innerHTML = 'Philosopher P' + i + '<small>THINKING</small>';
                            p.className = 'person';
                        }
                        document.getElementById('progress').value = 0;
                        document.getElementById('status').textContent = 'Ready';
                        renderForks();
                    }

                    function seek(value) {
                        pause();
                        resetView();
                        while (index < Number(value) && index < events.length) {
                            draw(events[index++]);
                        }
                    }

                    function showAll() {
                        pause();
                        resetView();
                        while (index < events.length) draw(events[index++]);
                    }

                    document.getElementById('progress').max = events.length;
                    renderForks();
                </script>
            </body>
            </html>
            """.replace("TITLE_PLACEHOLDER", safeTitle)
               .replace("EVENTS_PLACEHOLDER", eventsJson);
    }
}
