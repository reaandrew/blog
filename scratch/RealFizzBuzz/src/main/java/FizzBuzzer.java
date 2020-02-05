import java.util.ArrayList;


/**
 * FizzBuzzer.
 */
public class FizzBuzzer {
    /**
     * returns ‘fizz’ for numbers that are multiples of 3.
     * returns ‘buzz’ for numbers that are multiples of 5.
     * returns ‘fizzbuzz’ for numbers that are multiples of 15.
     * returns the number if this criteria is not met.
     */
    public String fizzBuzz(int number) {
        String value = String.valueOf(number);
        if (value.contains("3")) {
            return "lucky";
        }
        if (number % 15 == 0) {
            return "fizzbuzz";
        }
        if (number % 5 == 0) {
            return "buzz";
        }
        if (number % 3 == 0) {
            return "fizz";
        }
        return value;
    }

    /**
     * range returns a space separated list of outputs from the fizzbuzz function.
     *
     * @param range A min and max value to generate fizz buzz outcomes from
     * @return String space separated list
     */
    public String range(Range range) {
        ArrayList<String> returnArray = this.generateFizzBuzzList(range);
        return String.join(" ", returnArray);
    }

    private ArrayList<String> generateFizzBuzzList(Range range) {
        ArrayList<String> returnArray = new ArrayList<String>();
        for (int i = range.min(); i <= range.max(); i++) {
            returnArray.add(this.fizzBuzz(i));
        }
        return returnArray;
    }

    /**
     * report returns the output of range but with a report at the end showing the number of occurences.
     *
     * @param range A min and max value to generate fizz buzz outcomes from
     * @return String space separated list
     */
    public String report(Range range) {
        ArrayList<String> returnArray = this.generateFizzBuzzList(range);
        FizzBuzzStats stats = FizzBuzzStats.create(returnArray);
        returnArray.add(stats.getStat("fizz"));
        returnArray.add(stats.getStat("buzz"));
        returnArray.add(stats.getStat("fizzbuzz"));
        returnArray.add(stats.getStat("lucky"));
        returnArray.add(stats.getStat("integer"));
        return String.join(" ", returnArray);
    }
}
