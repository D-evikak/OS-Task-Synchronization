package readerswriters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SharedData {

    private int value = 0;

    private final List<String> events =
            Collections.synchronizedList(new ArrayList<>());

    private final long startTime = System.nanoTime();

    public synchronized int readValue() {
        return value;
    }

    public synchronized int updateValue() {
        value++;
        return value;
    }

    public synchronized int getValue() {
        return value;
    }

    public void addEvent(
            String name,
            String state,
            String message,
            int currentValue) {

        long time =
                (System.nanoTime() - startTime) / 1_000_000;

        String event = "{\"time\":" + time
                + ",\"name\":\"" + escape(name)
                + "\",\"state\":\"" + escape(state)
                + "\",\"message\":\"" + escape(message)
                + "\",\"value\":" + currentValue + "}";

        events.add(event);
    }

    public String getEvents() {
        synchronized (events) {
            return "[" + String.join(",", events) + "]";
        }
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }
}
	