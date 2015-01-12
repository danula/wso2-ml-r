package org.wso2.carbon.ml.extension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.parser.ParseException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.JRI.JRIEngine;
import org.wso2.carbon.ml.extension.util.InitializeWorkflow;
import org.wso2.carbon.ml.model.internal.dto.MLFeature;
import org.wso2.carbon.ml.model.internal.dto.MLWorkflow;

public class RExtension {

	private REngine re;

	/**
	 * Default constructor for {@link RExtension}. Creates a REngine instance.
	 * 
	 * @throws REngineException
	 */
	public RExtension() throws REngineException {
		this.re = JRIEngine.createEngine();
	}

	/**
	 * Evaluates {@link MLWorkflow}
	 * 
	 * @param workflowURL
	 *            absolute location of the JSON mapped workflow
	 * @param exportToPMML
	 *            export as a PMML
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 * @throws REngineException
	 * @throws REXPMismatchException
	 */
	public void evaluate(String workflowURL, boolean exportToPMML)
			throws FileNotFoundException, IOException, ParseException,
			REngineException, REXPMismatchException {
		InitializeWorkflow init = new InitializeWorkflow();
		MLWorkflow mlWorkflow = init.parseWorkflow(workflowURL);
		runScript(mlWorkflow, exportToPMML);
	}

	/**
	 * Evaluates {@link MLWorkflow}
	 * 
	 * @param mlWorkflow
	 *            MLWorkflow bean
	 * @param exportToPMML
	 *            export as a PMML
	 * @throws REngineException
	 * @throws REXPMismatchException
	 */

	public void runScript(MLWorkflow mlWorkflow, boolean exportToPMML)
			throws REngineException, REXPMismatchException {

		StringBuffer script = new StringBuffer();
		REXP env = re.newEnvironment(null, true);

		re.parseAndEval("input <- read.csv('" + mlWorkflow.getDatasetURL()
				+ "')", env, false);

		script.append("model <- ");

		switch (mlWorkflow.getAlgorithmName()) {
		case "LOGISTIC_REGRESSION":
			script.append("glm(");
			break;

		case "RANDOM_FOREST":
			re.parseAndEval("library(randomForest)", env, false);
			script.append("randomForest(");
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
			break;

		case "K_MEANS":
			script.append("kmeans(");
			break;

		case "NAIVE_BAYES":
			re.parseAndEval("library('e1071')", env, false);
			script.append("naiveBayes(");
			break;

		}

		List<MLFeature> features = mlWorkflow.getFeatures();

		if (mlWorkflow.getAlgorithmClass().equals("Classification")) {
			// for classification
			script.append(mlWorkflow.getResponseVariable());
			script.append(" ~ ");

			boolean flag = false;

			for (int i = 0; i < features.size(); i++) {
				MLFeature feature = features.get(i);
				if (feature.isInclude()) {
					if (!mlWorkflow.getResponseVariable().equals(
							feature.getName())) {
						if (flag)
							script.append("+");
						script.append(feature.getName());
						flag = true;
					}

					// define categorical data
					defineCategoricalData(feature, env);

					// impute
					impute(feature, env);
				}
			}

			script.append(",data=input");

		} else if (mlWorkflow.getAlgorithmClass().equals("Clustering")) {
			// for clustering
			script.append("x = input$");
			script.append(mlWorkflow.getResponseVariable());

			for (int i = 0; i < features.size(); i++) {
				MLFeature feature = features.get(i);
				if (feature.isInclude()) {
					
					// define categorical data
					defineCategoricalData(feature, env);

					// impute
					impute(feature, env);
				}
			}

		}

		// appending parameters to the script
		Map<String, String> hyperParameters = mlWorkflow.getHyperParameters();
		script = appendParameters(hyperParameters, script);

		REXP x = re.parseAndEval(script.toString(), env, true);

		if (exportToPMML) {
			exportToPMML(env);
		}

	}

	private StringBuffer appendParameters(Map<String, String> hyperParameters,
			StringBuffer script) {

		for (Map.Entry<String, String> entry : hyperParameters.entrySet()) {
			script.append(",");
			script.append(entry.getKey());
			script.append("=");
			script.append(entry.getValue());
		}
		script.append(")");

		return script;
	}

	private void exportToPMML(REXP env) throws REngineException,
			REXPMismatchException {
		re.parseAndEval("library(pmml)", env, false);
		re.parseAndEval("modelpmml <- pmml(model)", env, false);
		re.parseAndEval("write(toString(modelpmml),file = 'model.pmml')", env,
				false);

		REXP x = re.parseAndEval("model", env, true);

	}

	private void impute(MLFeature feature, REXP env) throws REngineException,
			REXPMismatchException {
		if (feature.getImputeOption().equals("REPLACE_WTH_MEAN")) {
			String name = feature.getName();
			// calculate the mean
			re.parseAndEval("temp <- mean(input$" + name + ",na.rm=TRUE)", env,
					false);

			System.out.println("temp <- mean(input$" + name + ",na.rm=TRUE)");
			// replace NA with mean
			re.parseAndEval("input$" + name + "[input$" + name
					+ "==NA] <- temp", env, false);
			System.out.println("input$" + name + "[input$" + name
					+ "==NA] <- temp");
		} else if (feature.getImputeOption().equals("DISCARD")) {
			// remove the rows with NA
			re.parseAndEval("input[complete.cases(input$" + feature.getName()
					+ "),]", env, false);

		}
		// removing a row --- newdata <- na.omit(mydata)
	}

	private void defineCategoricalData(MLFeature feature, REXP env)
			throws REngineException, REXPMismatchException {
		if (feature.getType().equals("CATEGORICAL")) {
			String name = feature.getName();
			re.parseAndEval("input$" + name + "<- factor(input$" + name + ")",
					env, false);
			System.out.println("input$" + name + "<- factor(input$" + name
					+ ")");

		}
	}
}
