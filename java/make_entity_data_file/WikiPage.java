package make_entity_data_file;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * We use the entire Wikipedia page of the entity to represent the entity.
 * Since the Wikipedia page is very long, we only use the top 100 TF-IDF words from the page.
 * @author Shubham Chatterjee
 * @version 9/9/2021
 */

public class WikiPage extends LeadText{

    private final Map<String, String> tfidf;
    private final List<String> stopWords;


    public WikiPage(String index,
                    String entityFile,
                    String tfIdfFile,
                    String stopWordsFile,
                    @NotNull String mode,
                    boolean parallel){

        super(index, entityFile, mode, parallel);

        System.out.print("Loading TF-IDF values...");
        tfidf = readTsvFile(tfIdfFile);
        System.out.println("[Done].");

        System.out.print("Loading stop words....");
        stopWords = getStopWords(stopWordsFile);
        System.out.println("[Done].");
    }

    protected void getEntityData(String entityId, double entityScore, @NotNull Map<String, String> res) {

        // Get the text of the Wikipedia page
        String pageText = idToText(entityId, "Text", indexSearcher);

        // Get the TF-IDF of words on the page
        // Sorted in descending order
        Map<String, Double> tfIdfMap = sortByValueDescending(getTfIdfOfWordsInText(pageText));

        if (! tfIdfMap.isEmpty()) {

            String topTfIdfWords = String.join(" ", tfIdfMap.keySet());

            String data = toJSONString(" ", " ", topTfIdfWords, entityScore);

            res.put(entityId, data);
        }
    }

    @NotNull
    private Map<String, Double> getTfIdfOfWordsInText(String pageText) {
        Set<String> pageWords = preProcess(pageText);
        Map<String, Double> tfIdfMap = new HashMap<>();
        for (String pageWord : pageWords) {
            if (tfidf.containsKey(pageWord)) {
                double tfIdf = Double.parseDouble(tfidf.get(pageWord));
                if (tfIdf != 0.0) {
                    tfIdfMap.put(pageWord, tfIdf);
                }
            }
        }
        return tfIdfMap;
    }

    /**
     * Pre-process the text.
     * (1) Lowercase words.
     * (2) Remove all spaces.
     * (3) Remove special characters.
     * (4) Remove stop words.
     * @param text String Text to pre-process
     * @return List of words from the text after pre-processing.
     */

    @NotNull
    private Set<String> preProcess(String text) {

        // Convert all words to lowercase
        text = text.toLowerCase();

        // Remove all spaces
        text = text.replace("\n", " ").replace("\r", " ");

        // Remove all special characters such as - + ^ . : , ( )
        text = text.replaceAll("[\\-+.^*:,;=(){}\\[\\]\"]","");

        // Get all words
        Set<String> words = new HashSet<>(Arrays.asList(text.split(" ")));
        words.removeAll(Collections.singleton(null));
        words.removeAll(Collections.singleton(""));

        // Remove all stop words
        words.removeIf(stopWords::contains);

        return words;
    }

    @Contract(pure = true)
    public static void main(@NotNull String[] args) {
        String index = args[0];
        String entityFile = args[1];
        String tfIdfFile = args[2];
        String stopWordsFile = args[3];
        String outFile = args[4];
        String mode = args[5];
        boolean parallel = args[6].equals("true");

        WikiPage ob = new WikiPage(index, entityFile, tfIdfFile, stopWordsFile, mode, parallel);

        if (mode.equals("train")) {
            ob.doTask(ob.entities.keySet(), mode);
        } else {
            ob.doTask(ob.entityRunMap.keySet(), mode);
        }
        System.out.print("Writing to file....");
        ob.writeToFile(outFile, ob.entityDataMap);
        System.out.println("[Done].");
    }
}
