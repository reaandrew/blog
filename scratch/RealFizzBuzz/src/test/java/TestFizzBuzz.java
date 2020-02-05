import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

public class TestFizzBuzz {

    private final FizzBuzzer fizzBuzzer = new FizzBuzzer();

    @Test
    public void returnsTheNumber() {
        Assert.assertThat(fizzBuzzer.fizzBuzz(1), Is.is("1"));
        Assert.assertThat(fizzBuzzer.fizzBuzz(2), Is.is("2"));
    }

    @Test
    public void returnsFizzForMultiplesOf3() {
        Assert.assertThat(fizzBuzzer.fizzBuzz(3), Is.is("lucky"));
        Assert.assertThat(fizzBuzzer.fizzBuzz(6), Is.is("fizz"));
    }

    @Test
    public void returnsBuzzForMultiplesof5() {
        Assert.assertThat(fizzBuzzer.fizzBuzz(5), Is.is("buzz"));
        Assert.assertThat(fizzBuzzer.fizzBuzz(10), Is.is("buzz"));
    }

    @Test
    public void returnsFizzBuzzForMutiplesOf15() {
        Assert.assertThat(fizzBuzzer.fizzBuzz(15), Is.is("fizzbuzz"));
        Assert.assertThat(fizzBuzzer.fizzBuzz(30), Is.is("lucky"));
    }

    @Test
    public void returnsAnAnArrayOfOutcomesBasedOnARange() {
        Assert.assertThat(fizzBuzzer.range(new Range(1, 2)), Is.is("1 2"));
        Assert.assertThat(fizzBuzzer.range(new Range(3, 4)), Is.is("lucky 4"));
        Assert.assertThat(fizzBuzzer.range(new Range(5, 6)), Is.is("buzz fizz"));
        Assert.assertThat(fizzBuzzer.range(new Range(14, 15)), Is.is("14 fizzbuzz"));
    }

    @Test
    public void returnsTheExpectedAssignmentOutcome() {
        String expectedOutcome = "1 2 lucky 4 buzz fizz 7 8 fizz buzz 11 fizz lucky 14 fizzbuzz 16 17 fizz 19 buzz";
        String actualOutcome = fizzBuzzer.range(new Range(1, 20));
        Assert.assertThat(actualOutcome, Is.is(expectedOutcome));
    }

    @Test
    public void returnsTheExpectedAssignmentOutcomeIncludingAReport() {
        String expectedOutcome = "1 2 lucky 4 buzz fizz 7 8 fizz buzz 11 fizz lucky 14 fizzbuzz 16 17 fizz 19 buzz fizz: 4 buzz: 3 fizzbuzz: 1 lucky: 2 integer: 10";
        String actualOutcome = fizzBuzzer.report(new Range(1, 20));
        Assert.assertThat(actualOutcome, Is.is(expectedOutcome));
    }
}
