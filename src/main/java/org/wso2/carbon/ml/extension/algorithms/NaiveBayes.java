package org.wso2.carbon.ml.extension.algorithms;

import org.wso2.carbon.ml.extension.bean.MLRWorkflow;

import java.util.ArrayList;
import java.util.List;

public class NaiveBayes extends RAlgorithm {

    private List<String> rLibraries = new ArrayList<>();

    public NaiveBayes() {
        super("NAIVE_BAYES", null);
    }

    @Override
    public ArrayList<String> generatePMML(StringBuilder parameters, MLRWorkflow mlrWorkflow) {

        ArrayList<String> modelScript = new ArrayList<>();
        modelScript.add("library('e1071')");
        modelScript.add("bestModel<- naiveBayes(" + parameters.toString() + ")");
        modelScript.add("modelpmml <- pmml(bestModel, dataset=input, predictedField=\"" + mlrWorkflow.getResponseVariable() + "\")");

        return modelScript;
    }
}
