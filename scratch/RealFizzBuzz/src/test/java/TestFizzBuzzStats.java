import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

public class TestFizzBuzzStats {
    @Test
    public void aggregatesTheValuesForIntegers() {
        List<String> list = Arrays.asList("1", "1", "1", "1");
        ArrayList<String> input = new ArrayList<>(list);
        String expectedOutcome = "integer: 4";
        String actualOutput = FizzBuzzStats.create(input).getStat("integer");
        Assert.assertThat(actualOutput, Is.is(expectedOutcome));
    }

    @Test
    public void aggregatesTheValuesForAnythingOtherThanIntegers() {
        List<String> list = Arrays.asList("A", "A", "B", "B");
        ArrayList<String> input = new ArrayList<>(list);
        Assert.assertThat(FizzBuzzStats.create(input).getStat("A"), Is.is("A: 2"));
        Assert.assertThat(FizzBuzzStats.create(input).getStat("B"), Is.is("B: 2"));
    }

}
