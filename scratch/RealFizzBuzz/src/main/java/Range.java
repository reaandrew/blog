/**
 * Range representation a value type to use with the different fizzbuzz methods.
 */
public class Range {
    private final int min;
    private final int max;

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int min() {
        return this.min;
    }

    public int max() {
        return this.max;
    }
}
