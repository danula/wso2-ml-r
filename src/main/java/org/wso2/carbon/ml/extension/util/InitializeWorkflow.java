package org.wso2.carbon.ml.extension.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.ml.model.internal.dto.MLFeature;
import org.wso2.carbon.ml.model.internal.dto.MLWorkflow;

public class InitializeWorkflow {

	/**
	 * Parses Workflow JSON file
	 * 
	 * @param fileURL
	 *            the URL of the JSON file
	 * @return MLWorkflow Bean
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public MLWorkflow parseWorkflow(String fileURL) throws FileNotFoundException, IOException, ParseException {

		JSONParser parser = new JSONParser();
		JSONObject workflow = null;

		Object obj = parser.parse(new FileReader(fileURL));

		workflow = (JSONObject) obj;

		if (workflow != null) {
			return populateWorkflowBean(workflow);
		}

		return null;
	}

	/**
	 * Populate the MLWorkflow Bean
	 * 
	 * @param workflow
	 *            the parsed JSON object
	 * @return MLWorkflow Bean
	 */
	private MLWorkflow populateWorkflowBean(JSONObject workflow) {

		MLWorkflow mlWorkflow = new MLWorkflow();

		// populating MLWorkfolw data
		mlWorkflow.setAlgorithmClass((String) workflow.get("algorithmClass"));
		mlWorkflow.setAlgorithmName((String) workflow.get("algorithmName"));
		mlWorkflow.setDatasetURL((String) workflow.get("datasetURL"));
		mlWorkflow.setResponseVariable((String) workflow.get("responseVariable"));
		mlWorkflow.setTrainDataFraction((Double) workflow.get("trainDataFraction"));
		mlWorkflow.setWorkflowID((String) workflow.get("workflowID"));

		// populating features
		JSONArray features = (JSONArray) workflow.get("features");
		mlWorkflow.setFeatures(populateFeatures(features));

		// populating hyper parameters
		JSONObject hyperParamObj = (JSONObject) workflow.get("hyperParameters");
		mlWorkflow.setHyperParameters(populateHyperParameters(hyperParamObj));

		return mlWorkflow;
	}

	/**
	 * Populate the feature set of the MLWorkflow bean
	 * 
	 * @param features
	 *            the JSON array extracted from the JSON file
	 * @return list of features
	 */
	private List<MLFeature> populateFeatures(JSONArray features) {

		List<MLFeature> featuresList = new ArrayList<MLFeature>();
		@SuppressWarnings("unchecked")
		Iterator<JSONObject> iterator = features.iterator();
		MLFeature feature = null;

		while (iterator.hasNext()) {
			feature = new MLFeature();

			JSONObject featureObj = (JSONObject) iterator.next();

			Long index = (Long) featureObj.get("index");

			feature.setIndex(index.intValue());
			feature.setImputeOption((String) featureObj.get("imputeOption"));
			feature.setName((String) featureObj.get("name"));
			feature.setType((String) featureObj.get("type"));
			feature.setInclude((Boolean) featureObj.get("include"));

			featuresList.add(feature);
		}

		return featuresList;
	}

	/**
	 * 
	 * @param object
	 *            the JSON object extracted from the JSON file
	 * @return Map with hyper parameters
	 */
	private Map<String, String> populateHyperParameters(JSONObject object) {
		Map<String, String> map = new HashMap<String, String>();

		@SuppressWarnings("unchecked")
		Set<String> keys = object.keySet();

		for (String key : keys) {
			String value = (String) object.get(key);
			map.put(key, value);
		}
		return map;
	}
}