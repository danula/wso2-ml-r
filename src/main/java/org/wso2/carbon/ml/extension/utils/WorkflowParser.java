package org.wso2.carbon.ml.extension.utils;

import com.google.gson.*;
import org.apache.log4j.Logger;
import org.wso2.carbon.ml.extension.bean.MLRWorkflow;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;
import org.wso2.carbon.ml.extension.bean.MLFeature;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class WorkflowParser {

    private final static Logger log = Logger.getLogger(WorkflowParser.class);

    /**
     * Parses Workflow JSON file
     *
     * @param fileURL the URL of the JSON file
     * @return {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
     * @throws org.wso2.carbon.ml.extension.exception.InitializationException
     * @throws org.wso2.carbon.ml.extension.exception.FormattingException
     */
    public MLRWorkflow parseWorkflow(String fileURL) throws InitializationException, FormattingException {

        log.info("Parsing Workflow");
	    log.debug("Workflow Location: " + fileURL);
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = null;
        FileReader fr = null;

        try {
            fr = new FileReader(fileURL);
            jsonElement = parser.parse(fr);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            throw new InitializationException("Workflow JSON file does not exist", e);
        } catch (JsonSyntaxException e) {
            log.error(e.getMessage());
            throw new InitializationException("Workflow syntax error", e);
        } catch (JsonParseException e){
            log.error(e.getMessage());
            throw new InitializationException("Workflow cannot be parsed", e);
        }

        if (jsonElement != null) {
            return populateWorkflowBean(jsonElement);
        }

        log.error("Parse Error : jsonElement is null");
        return null;
    }

    /**
     * Populate the MLRWorkflow Bean
     *
     * @param jsonElement the parsed JSON object
     * @return MLRWorkflow Bean
     */
    private MLRWorkflow populateWorkflowBean(JsonElement jsonElement) {

        MLRWorkflow mlRWorkflow = new MLRWorkflow();
        JsonObject workflow = jsonElement.getAsJsonObject();
        // populating MLWorkfolw data
        mlRWorkflow.setAlgorithmClass(workflow.getAsJsonObject().get("algorithmClass").getAsString());
        mlRWorkflow.setAlgorithmName(workflow.get("algorithmName").getAsString());
        mlRWorkflow.setDatasetURL(workflow.get("datasetURL").getAsString());
        mlRWorkflow.setResponseVariable(workflow.get("responseVariable").getAsString());
        mlRWorkflow.setTrainDataFraction(workflow.get("trainDataFraction").getAsDouble());
        mlRWorkflow.setWorkflowID(workflow.get("workflowID").getAsString());

        // populating features
        JsonArray features = workflow.get("features").getAsJsonArray();
        if (features != null)
            mlRWorkflow.setFeatures(populateFeatures(features));

        // populating hyper parameters
        JsonElement hyperParameters = workflow.get("hyperParameters");
        if (hyperParameters != null)
            mlRWorkflow.setHyperParameters(populateParameters(hyperParameters));

        JsonElement trainControls = workflow.get("trainControls");
        if(trainControls != null) {
            mlRWorkflow.setTrainControls(populateParameters(trainControls));
        } else {
            mlRWorkflow.setTrainControls(CommonConstants.DEFAULT_TRAIN_CONTROLS);
        }

        return mlRWorkflow;
    }

    /**
     * Populate the feature set of the MLRWorkflow bean
     *
     * @param features the JSON array extracted from the JSON file
     * @return list of features
     */
    private List<MLFeature> populateFeatures(JsonArray features) {

        log.debug("Parsing Features");
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

        Map<String, String> map = new HashMap<String, String>();
        JsonObject parameters = jsonElement.getAsJsonObject();

        @SuppressWarnings("unchecked")
        Set<Map.Entry<String, JsonElement>> keys = parameters.entrySet();

        for (Map.Entry key : keys) {
            String value = jsonElement.getAsJsonObject().get(key.getKey().toString()).getAsString();
            map.put(key.getKey().toString(), value);
        }
        return map;
    }
}
