package org.wso2.carbon.ml.extension.algorithms;

import org.wso2.carbon.ml.extension.bean.MLRWorkflow;
import org.wso2.carbon.ml.extension.utils.CommonConstants;

import java.util.ArrayList;

public class RandomForest extends RAlgorithm{

	public static final String LIB_RF = "library('randomForest')";

	@Override public ArrayList<String> generatePMML(StringBuilder parameters,
	                                                MLRWorkflow mlrWorkflow) {

		ArrayList<String> modelScript = new ArrayList<>();
		modelScript.add(LIB_RF);
		modelScript.add(CommonConstants.LIBRARY_PMML);
		modelScript
				.add(CommonConstants.TUNED_MODEL + "<- randomForest(" + parameters.toString() + ")");
		modelScript.add(CommonConstants.PMML_MODEL + " <- pmml(" + CommonConstants.TUNED_MODEL + ")");

		return modelScript;
	}

	@Override public void runAdditionalScripts(MLRWorkflow mlRWorkflow) {

	}
}
