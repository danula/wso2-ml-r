package org.wso2.carbon.ml.extension.algorithms;

import org.wso2.carbon.ml.extension.bean.MLRWorkflow;
import org.wso2.carbon.ml.extension.utils.CommonConstants;

import java.util.ArrayList;

public class LogisticRegression extends RAlgorithm{

	@Override public ArrayList<String> generatePMML(StringBuilder parameters,
	                                                MLRWorkflow mlrWorkflow) {
		ArrayList<String> modelScript = new ArrayList<>();
		modelScript.add(CommonConstants.LIBRARY_PMML);
		modelScript.add(CommonConstants.PMML_MODEL + " <- pmml(" + CommonConstants.FINAL_MODEL + ")");

		return modelScript;
	}

	@Override public void runAdditionalScripts(MLRWorkflow mlRWorkflow) {

	}
}
