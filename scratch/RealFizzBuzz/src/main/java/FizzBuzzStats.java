import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.lang3.math.NumberUtils;


/**
 * FizzBuzzStats is a utility to aggregate the different values including a count of occurences.
 */
public class FizzBuzzStats {
    private final HashMap<String, Integer> values;

    private FizzBuzzStats(HashMap<String, Integer> values) {
        this.values = values;
    }

    /**
     * create returns an instantiated FizzBuzzStats which can get used to extra the information.
     *
     * @param values An array of values to aggregate
     * @return FizzBuzzStats
     */
    public static final FizzBuzzStats create(ArrayList<String> values) {
        HashMap<String, Integer> stats = new HashMap<>();
        for (String element : values) {
            if (NumberUtils.isNumber(element)) {
                int current = stats.getOrDefault("integer", 0);
                stats.put("integer", current + 1);
            } else {
                int current = stats.getOrDefault(element, 0);
                stats.put(element, current + 1);
            }
        }
        return new FizzBuzzStats(stats);
    }

    /**
     * getStat returns a formatted string of the state along with the number of occurrences.
     *
     * @param key The name of the stat to return
     * @return returns a string
     */
    public String getStat(String key) {
        String format = "%s: %d";
        return String.format(format, key, this.values.get(key));
    }
}
