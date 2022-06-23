package make_entity_data_file;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * We use the lead text from the entity's Wikipedia article as the entity's description.
 * @author Shubham Chatterjee
 * @version 8/30/2021
 */

public class LeadText extends MakeEntityData {

    public Map<String, LinkedHashMap<String, Double>> entityRunMap = new HashMap<>();

    public LeadText(String index,
                    String entityFile,
                    @NotNull String mode,
                    boolean parallel){

        super(index, parallel);

        if (mode.equals("train")) {
            System.out.print("Loading entity file...");
            entities = readEntityFile(entityFile);
        } else {
            System.out.print("Loading entity run...");
            entityRunMap = readRunFile(entityFile);
        }
        System.out.println("[Done].");
    }

    @Override
    public void getEntityData(@NotNull String queryId, @NotNull String mode) {

        Map<String, String> res = new HashMap<>();

        if (mode.equals("train")) {
            Set<String> entitySet = entities.get(queryId);
            for (String entityId : entitySet) {
                getEntityData(entityId, 0.0, res);
            }
        } else {
            Map<String, Double> retEntityMap = entityRunMap.get(queryId);
            for (String entityId : retEntityMap.keySet()) {
                getEntityData(entityId, retEntityMap.get(entityId), res);
            }
        }
        entityDataMap.put(queryId, res);
        if (parallel) {
            count.getAndIncrement();
            System.out.println("Done: " + queryId + " ( " + count + "/" + total + " ).");
        }
    }

    protected void getEntityData(String entityId, double entityScore, Map<String, String> res) {
        String leadText = idToText(entityId, "LeadText", indexSearcher);
        String data = leadText.isEmpty()
                ? ""
                : toJSONString(" ", " ", leadText, entityScore);
        if (! data.isEmpty()) {
            res.put(entityId, data);
        }
    }

    public static void main(@NotNull String[] args) {
        String entityIndex = args[0];
        String entityFile = args[1]; // If mode!=train, then this file is entity run file, else pos/neg entity file.
        String outFile = args[2];
        String mode = args[3];
        boolean parallel = args[4].equals("true");

        LeadText ob = new LeadText(entityIndex, entityFile, mode, parallel);

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
