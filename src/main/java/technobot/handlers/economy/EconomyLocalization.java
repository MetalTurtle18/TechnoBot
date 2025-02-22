package technobot.handlers.economy;

import com.google.gson.Gson;
import technobot.data.json.EconomyResponses;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles localized responses to economy commands.
 *
 * @author TechnoVision
 */
public class EconomyLocalization {

    private static final String PATH = "localization/economy.json";

    private final String[] work;
    private final String[] crimeSuccess;
    private final String[] crimeFail;

    /**
     * Reads economy.json responses into local memory
     */
    public EconomyLocalization() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PATH);
        Reader reader = new BufferedReader(new InputStreamReader(inputStream));
        EconomyResponses responses = new Gson().fromJson(reader, EconomyResponses.class);
        work = responses.getWork();
        crimeSuccess = responses.getCrimeSuccess();
        crimeFail = responses.getCrimeFail();
    }

    /**
     * Get a reply from the list of 'work' responses.
     *
     * @param amount the amount of money earned.
     * @return an EconomyReply object with response and ID number.
     */
    public EconomyReply getWorkResponse(long amount, String currency) {
        int index = ThreadLocalRandom.current().nextInt(work.length);
        String value = currency+" "+EconomyHandler.FORMATTER.format(amount);
        String reply = work[index].replace("{amount}", value);
        return new EconomyReply(reply, index+1);
    }

    /**
     * Get a reply from the list of 'crimeSuccess' responses.
     *
     * @param amount the amount of money earned.
     * @return an EconomyReply object with response and ID number.
     */
    public EconomyReply getCrimeSuccessResponse(long amount, String currency) {
        int index = ThreadLocalRandom.current().nextInt(crimeSuccess.length);
        String value = currency+" "+EconomyHandler.FORMATTER.format(amount);
        String reply = crimeSuccess[index].replace("{amount}", value);
        return new EconomyReply(reply, index+1, true);
    }

    /**
     * Get a reply from the list of 'crimeFail' responses.
     *
     * @param amount the amount of money list.
     * @return an EconomyReply object with response and ID number.
     */
    public EconomyReply getCrimeFailResponse(long amount, String currency) {
        int index = ThreadLocalRandom.current().nextInt(crimeFail.length);
        String value = currency+" "+EconomyHandler.FORMATTER.format(amount);
        String reply = crimeFail[index].replace("{amount}", value);
        return new EconomyReply(reply, index+1, false);
    }
}
