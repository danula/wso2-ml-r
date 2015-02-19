package org.wso2.carbon.ml.extension.algorithms;

import org.wso2.carbon.ml.extension.bean.MLRWorkflow;
import org.wso2.carbon.ml.extension.utils.CommonConstants;

import java.util.ArrayList;

public class NaiveBayes extends RAlgorithm {

	@Override
	public ArrayList<String> generatePMML(StringBuilder parameters,
	                                                MLRWorkflow mlrWorkflow) {

		ArrayList<String> modelScript = new ArrayList<>();
		modelScript.add("library('e1071')");
		modelScript
				.add(CommonConstants.TUNED_MODEL + "<- naiveBayes(" + parameters.toString() + ")");
		modelScript.add(CommonConstants.PMML_MODEL + " <- pmml(" + CommonConstants.TUNED_MODEL +
		                ", dataset="+CommonConstants.DATASET + ", predictedField=\"" + mlrWorkflow.getResponseVariable() +
		                "\")");

		return modelScript;
	}

	@Override public ArrayList<String> generateAdditionalScripts(MLRWorkflow mlRWorkflow) {
		return null;
	}
}
