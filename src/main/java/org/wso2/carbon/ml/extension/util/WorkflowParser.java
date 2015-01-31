package org.wso2.carbon.ml.extension.util;

import com.google.gson.*;
import org.apache.log4j.Logger;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;
import org.wso2.carbon.ml.extension.bean.MLFeature;
import org.wso2.carbon.ml.extension.bean.MLWorkflow;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class WorkflowParser {

    private final static Logger LOGGER = Logger.getLogger(WorkflowParser.class);

    /**
     * Parses Workflow JSON file
     *
     * @param fileURL the URL of the JSON file
     * @return {@link org.wso2.carbon.ml.extension.bean.MLWorkflow}
     * @throws org.wso2.carbon.ml.extension.exception.InitializationException
     * @throws org.wso2.carbon.ml.extension.exception.FormattingException
     */
    public MLWorkflow parseWorkflow(String fileURL) throws InitializationException, FormattingException {

        LOGGER.debug("Parsing Workflow");
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = null;
        FileReader fr = null;

        try {
            fr = new FileReader(fileURL);
            jsonElement = parser.parse(fr);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
            throw new InitializationException("Workflow JSON file does not exist", e);
        }

        if (jsonElement != null) {
            return populateWorkflowBean(jsonElement);
        }

        LOGGER.error("Parse Error : jsonElement is null");
        return null;
    }

    /**
     * Populate the MLWorkflow Bean
     *
     * @param jsonElement the parsed JSON object
     * @return MLWorkflow Bean
     */
    private MLWorkflow populateWorkflowBean(JsonElement jsonElement) {

        MLWorkflow mlWorkflow = new MLWorkflow();
        JsonObject workflow = jsonElement.getAsJsonObject();
        // populating MLWorkfolw data
        mlWorkflow.setAlgorithmClass(workflow.getAsJsonObject().get("algorithmClass").getAsString());
        mlWorkflow.setAlgorithmName(workflow.get("algorithmName").getAsString());
        mlWorkflow.setDatasetURL(workflow.get("datasetURL").getAsString());
        mlWorkflow.setResponseVariable(workflow.get("responseVariable").getAsString());
        mlWorkflow.setTrainDataFraction(workflow.get("trainDataFraction").getAsDouble());
        mlWorkflow.setWorkflowID(workflow.get("workflowID").getAsString());

        // populating features
        JsonArray features = workflow.get("features").getAsJsonArray();
        if (features != null)
            mlWorkflow.setFeatures(populateFeatures(features));

        // populating hyper parameters
        JsonElement hyperParameters = workflow.get("hyperParameters");
        if (hyperParameters != null)
            mlWorkflow.setHyperParameters(populateParameters(hyperParameters));

        JsonElement trainControls = workflow.get("trainControls");
        if(trainControls != null)
            mlWorkflow.setTrainControls(populateParameters(trainControls));

        return mlWorkflow;
    }

    /**
     * Populate the feature set of the MLWorkflow bean
     *
     * @param features the JSON array extracted from the JSON file
     * @return list of features
     */
    private List<MLFeature> populateFeatures(JsonArray features) {

        LOGGER.debug("Parsing Features");
        List<MLFeature> featuresList = new ArrayList<MLFeature>();
        @SuppressWarnings("unchecked")
        Iterator<JsonElement> iterator = features.iterator();
        MLFeature feature = null;

        while (iterator.hasNext()) {
            feature = new MLFeature();

            JsonObject featureElement = iterator.next().getAsJsonObject();

            Long index = featureElement.get("index").getAsLong();

            feature.setIndex(index.intValue());
            feature.setImputeOption(featureElement.get("imputeOption").getAsString());
            feature.setName(featureElement.get("name").getAsString());
            feature.setType(featureElement.get("type").getAsString());
            feature.setInclude(featureElement.get("include").getAsBoolean());

            featuresList.add(feature);
        }

        return featuresList;
    }

    /**
     * @param jsonElement the JSON object extracted from the JSON file
     * @return Map with hyper parameters
     */
    private Map<String, String> populateParameters(JsonElement jsonElement) {

        LOGGER.debug("Parsing Hyper Parameters");
        Map<String, String> map = new HashMap<String, String>();
        JsonObject hyperParams = jsonElement.getAsJsonObject();

        @SuppressWarnings("unchecked")
        Set<Map.Entry<String, JsonElement>> keys = hyperParams.entrySet();

        for (Map.Entry key : keys) {
            String value = jsonElement.getAsJsonObject().get(key.getKey().toString()).getAsString();
            map.put(key.getKey().toString(), value);
        }
        return map;
    }
}
