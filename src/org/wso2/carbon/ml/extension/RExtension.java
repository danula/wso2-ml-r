package org.wso2.carbon.ml.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPEnvironment;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.JRI.JRIEngine;
import org.wso2.carbon.ml.extension.util.InitializeWorkflow;
import org.wso2.carbon.ml.model.internal.dto.MLFeature;
import org.wso2.carbon.ml.model.internal.dto.MLWorkflow;

public class RExtension {

	private REngine re;

	public RExtension() {

		try {
			re = JRIEngine.createEngine();
		} catch (REngineException e) {
			System.out.println(e.getMessage());
		}
	}

	public static void main(String[] args) {

		RExtension rex = new RExtension();
		// new R-engine

		InitializeWorkflow ob = new InitializeWorkflow();
		MLWorkflow mlWorkflow = ob.parseWorkflow("example_workflow.json");

		// evaluate MLWorkflow
		try {
			rex.evaluate(mlWorkflow);
		} catch (REngineException e) {
			System.out.println(e.getMessage());
		} catch (REXPMismatchException e) {
			System.out.println(e.getMessage());
		}

	}

	private void evaluate(MLWorkflow mlWorkflow) throws REngineException,
			REXPMismatchException {

		REXP env = re.newEnvironment(null, true);

		// load data from csv
		re.parseAndEval("input <- read.csv('" + mlWorkflow.getDatasetURL()
				+ "')", env, false);

		System.out.println("input <- read.csv('" + mlWorkflow.getDatasetURL()
				+ "')");

		StringBuffer script = new StringBuffer();
		script.append("model <- ");

		switch (mlWorkflow.getAlgorithmName()) {
		case "LOGISTIC_REGRESSION":
			script.append("glm(");
			// parameters.add("family=binomial(link='logit')");
			break;

		case "RANDOM_FOREST":
			re.parseAndEval("library(randomForest)", env, false);
			script.append("randomForest(");
			// parameters.add("ntrees=50");
			break;

		case "SVM":
			re.parseAndEval("library('e1071')", env, false);
			script.append("svm(");
			break;

		case "LINEAR_REGRESSION":
			script.append("lm(");
			break;

		case "DECISION_TREES":
			re.parseAndEval("library(rpart)");
			script.append("rpart(");
			// parameters.add("method='class'");
			break;

		case "K_MEANS":
			break;

		case "NAIVE_BAYES":
			re.parseAndEval("library('e1071')", env, false);
			script.append("naiveBayes(");
			break;

		}

		List<MLFeature> features = mlWorkflow.getFeatures();

		script.append(mlWorkflow.getResponseVariable());
		script.append(" ~ ");

		boolean flag = false;

		for (int i = 0; i < features.size(); i++) {
			MLFeature feature = features.get(i);
			if (feature.isInclude()) {
				if (!mlWorkflow.getResponseVariable().equals(feature.getName())) {
					if (flag)
						script.append("+");
					script.append(feature.getName());
					flag = true;
				}

				// define categorical data
				if (feature.getType().equals("CATEGORICAL")) {
					String name = feature.getName();
					re.parseAndEval("input$" + name + "<- factor(input$" + name
							+ ")", env, false);
					System.out.println("input$" + name + "<- factor(input$"
							+ name + ")");
				}

				// impute
				if (feature.getImputeOption().equals("REPLACE_WTH_MEAN")) {
					String name = feature.getName();
					// calculate the mean
					re.parseAndEval("temp <- mean(input$" + name
							+ ",na.rm=TRUE)", env, false);

					System.out.println("temp <- mean(input$" + name
							+ ",na.rm=TRUE)");
					// replace NA with mean
					re.parseAndEval("input$" + name + "[input$" + name
							+ "==NA] <- temp", env, false);
					System.out.println("input$" + name + "[input$" + name
							+ "==NA] <- temp");
				} else if (feature.getImputeOption().equals("DISCARD")) {
					// remove the rows with NA
					re.parseAndEval(
							"input[complete.cases(input$" + feature.getName()
									+ "),]", env, false);
				}
				// removing a row --- newdata <- na.omit(mydata)
			}
		}
		// appending parameters to the script
		// for (int i = 0; i < parameters.size(); i++) {
		// script.append(",");
		// script.append(parameters.get(i));
		// }

		script.append(",data=input");

		// appending parameters to the script
		Map<String, String> hyperParameters = mlWorkflow.getHyperParameters();
		for (Map.Entry<String, String> entry : hyperParameters.entrySet()) {
			script.append(",");
			script.append(entry.getKey());
			script.append("=");
			script.append(entry.getValue());
		}
		script.append(")");

		REXP x = re.parseAndEval(script.toString(), env, true);

		System.out.println(script);

		// saving model in PMML
		re.parseAndEval("library(pmml)", env, false);
		re.parseAndEval("modelpmml <- pmml(model)", env, false);
		re.parseAndEval("write(toString(modelpmml),file = 'model.pmml')", env,
				false);

		REXP y = re.parseAndEval("coef(model)[['Age']]", env, true);
		// RVector x = re.eval("model").asVector();

		System.out.println(x.toDebugString());
		System.out.println(y.toString());

	}

}
