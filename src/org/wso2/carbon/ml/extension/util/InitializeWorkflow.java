package org.wso2.carbon.ml.extension.util;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wso2.carbon.ml.model.internal.dto.MLFeature;
import org.wso2.carbon.ml.model.internal.dto.MLWorkflow;

public class InitializeWorkflow {
	
	
	/**
	 * Parsing Workflow JSON file
	 * @param fileURL the URL of the json file
	 * @return MLWorkflow Bean
	 */
	public MLWorkflow parseWorkflow(String fileURL){
		
		JSONParser parser = new JSONParser();
		JSONObject workflow = null;
		
		try {
			 
            Object obj = parser.parse(new FileReader(fileURL));
 
            workflow = (JSONObject) obj;
 
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
		
		
		if(workflow != null){
			return populateWorkflowBean(workflow);
		}
		
		return null;
	}
	
	/**
	 * Populate the MLWorkflow Bean
	 * @param workflow, the parsed JSON object
	 * @return MLWorkflow Bean
	 */
	private MLWorkflow populateWorkflowBean(JSONObject workflow){
		
		MLWorkflow mlWorkflow = new MLWorkflow();
		List<MLFeature> featuresList = new ArrayList<MLFeature>();
		Map<String, String> hyperParameters = new HashMap<String, String>();
		
		//populating MLWorkfolw data
		mlWorkflow.setAlgorithmClass((String)workflow.get("algorithmClass"));
		mlWorkflow.setAlgorithmName((String)workflow.get("algorithmName"));
		mlWorkflow.setDatasetURL((String)workflow.get("datasetURL"));
		mlWorkflow.setResponseVariable((String)workflow.get("responseVariable"));
		mlWorkflow.setTrainDataFraction((Double)workflow.get("trainDataFraction"));
		mlWorkflow.setWorkflowID((String)workflow.get("workflowID"));
		
		
		//populating features
		JSONArray features = (JSONArray)workflow.get("features");
		
		Iterator<JSONObject> iterator = features.iterator();
		MLFeature feature = null;
		
		while(iterator.hasNext()){
			feature = new MLFeature();
			
			JSONObject featureObj = (JSONObject)iterator.next();
			
			Long index = (Long)featureObj.get("index");
			
			feature.setIndex(index.intValue());
			feature.setImputeOption((String)featureObj.get("imputeOption"));			
			feature.setName((String)featureObj.get("name"));
			feature.setType((String)featureObj.get("type"));
			feature.setInclude((Boolean)featureObj.get("include"));
			
			featuresList.add(feature);
		}
		
		mlWorkflow.setFeatures(featuresList);
		
		//populating hyper parameters
		JSONObject hyperParamObj = (JSONObject)workflow.get("hyperParameters");
		hyperParameters.put("Iterations", (String) hyperParamObj.get("Iterations"));
		hyperParameters.put("Learning_Rate", (String) hyperParamObj.get("Learning_Rate"));
		hyperParameters.put("Reg_Parameter", (String) hyperParamObj.get("Reg_Parameter"));
		hyperParameters.put("Reg_Type", (String) hyperParamObj.get("Reg_Type"));
		hyperParameters.put("SGD_Data_Fraction", (String) hyperParamObj.get("SGD_Data_Fraction"));
		
		mlWorkflow.setHyperParameters(hyperParameters);
		
		return mlWorkflow;		
	}
}
