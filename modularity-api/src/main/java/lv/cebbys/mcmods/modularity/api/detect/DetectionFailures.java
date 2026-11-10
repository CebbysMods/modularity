package lv.cebbys.mcmods.modularity.api.detect;

import java.util.ArrayList;
import java.util.List;

/** Formats a failure chain for the detection exceptions. */
final class DetectionFailures {
    private DetectionFailures() {
    }

    /** {@code Type: message -> CauseType: message}, without repeating cycles. */
    static String describe(Throwable error) {
        List<Throwable> chain = new ArrayList<>();
        for (Throwable t = error; t != null && !chain.contains(t); t = t.getCause()) {
            chain.add(t);
        }
        List<String> parts = new ArrayList<>();
        for (Throwable t : chain) {
            parts.add(t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        return String.join(" -> ", parts);
    }
}
