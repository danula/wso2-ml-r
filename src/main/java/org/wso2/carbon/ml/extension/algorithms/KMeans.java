package org.wso2.carbon.ml.extension.algorithms;

import org.wso2.carbon.ml.extension.bean.MLRWorkflow;
import org.wso2.carbon.ml.extension.utils.CommonConstants;

import java.util.ArrayList;
import java.util.Map;

public class KMeans extends RAlgorithm {

	@Override public ArrayList<String> generateScript(MLRWorkflow mlRWorkflow, StringBuilder formula){
		ArrayList<String> script = new ArrayList<>();
		StringBuilder clusterScript = new StringBuilder();
		clusterScript.append(CommonConstants.MODEL).append(" <- ");
		clusterScript.append(CommonConstants.ALGORITHM_MAP.get(mlRWorkflow.getAlgorithmName())).append("(").append(formula.toString());

		Map<String, String> hyperParameters = mlRWorkflow.getHyperParameters();
		for(Map.Entry<String, String> entry : hyperParameters.entrySet()){
			clusterScript.append(",").append(entry.getKey()).append("=").append(entry.getValue());
		}

		script.add(clusterScript.toString());
		return script;
	}
	@Override public ArrayList<String> generatePMML(StringBuilder parameters,
	                                                MLRWorkflow mlrWorkflow) {
		ArrayList<String> modelScript = new ArrayList<>();
		modelScript.add(CommonConstants.LIBRARY_PMML);
		modelScript.add(CommonConstants.PMML_MODEL + " <- pmml(" + CommonConstants.MODEL + ")");

		return modelScript;
	}

	@Override public ArrayList<String> generateAdditionalScripts(MLRWorkflow mlRWorkflow) {
		return null;
	}
}
