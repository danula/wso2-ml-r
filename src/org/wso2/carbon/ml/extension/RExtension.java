package org.wso2.carbon.ml.extension;

import java.util.ArrayList;
import java.util.List;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RList;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.Rengine;
import org.wso2.carbon.ml.extension.util.InitializeWorkflow;
import org.wso2.carbon.ml.model.internal.dto.MLFeature;
import org.wso2.carbon.ml.model.internal.dto.MLWorkflow;

public class RExtension {

	private MLWorkflow mlWorkflow;

	public RExtension() {
		this.mlWorkflow = new MLWorkflow();
	}

	public static void main(String[] args) {

		// new R-engine
		Rengine re = new Rengine(new String[] { "--vanilla" }, false, null);
		if (!re.waitForR()) {
			System.out.println("Cannot load R");
			return;
		}

		RExtension rex = new RExtension();
		InitializeWorkflow ob = new InitializeWorkflow();
		rex.mlWorkflow = ob.parseWorkflow("example_workflow.json");

		// evaluate MLWorkflow
		evaluate(re, rex.mlWorkflow);

		re.end();

	}

	private static void evaluate(Rengine re, MLWorkflow mlWorkflow) {

		// load data from csv
		re.eval("input <- read.csv('" + mlWorkflow.getDatasetURL() + "')",
				false);

		System.out.println("input <- read.csv('" + mlWorkflow.getDatasetURL()
				+ "')");

		StringBuffer script = new StringBuffer();
		script.append("model <- ");
		ArrayList<String> parameters = new ArrayList<>();
		parameters.add("data=input");

		switch (mlWorkflow.getAlgorithmName()) {
		case "LOGISTIC_REGRESSION":
			script.append("glm(");
			parameters.add("family=binomial(link='logit')");
			break;

		case "RANDOM_FOREST":
			re.eval("library(randomForest)", false);
			script.append("randomForest(");
			parameters.add("ntrees=50");
			break;

		case "SVM":
			re.eval("library('e1071')", false);
			script.append("svm(");
			// postfix =
			// ",data=input,type='C',kernel='linear',probability = TRUE)";
			break;

		case "LINEAR_REGRESSION":
			script.append("lm(");
			break;

		case "DECISION_TREES":
			re.eval("library(rpart)");
			script.append("rpart(");
			parameters.add("method='class'");
			break;

		case "K_MEANS":
			break;

		case "NAIVE_BAYES":
			re.eval("library('e1071')", false);
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
					re.eval("input$" + name + "<- factor(input$" + name + ")");
					System.out.println("input$" + name + "<- factor(input$"
							+ name + ")");
				}

				// impute
				if (feature.getImputeOption().equals("REPLACE_WTH_MEAN")) {
					String name = feature.getName();
					// calculate the mean
					re.eval("temp <- mean(input$" + name + ",na.rm=TRUE)");

					System.out.println("temp <- mean(input$" + name
							+ ",na.rm=TRUE)");
					// replace NA with mean
					re.eval("input$" + name + "[input$" + name
							+ "==NA] <- temp");
					System.out.println("input$" + name + "[input$" + name
							+ "==NA] <- temp");
				} else if (feature.getImputeOption().equals("DISCARD")) {
					// remove the rows with NA
					re.eval("input[complete.cases(input$" + feature.getName()
							+ "),]", false);
				}
				// removing a row --- newdata <- na.omit(mydata)
			}
		}
		// appending parameters to the script
		for (int i = 0; i < parameters.size(); i++) {
			script.append(",");
			script.append(parameters.get(i));
		}
		script.append(")");

		REXP x = re.eval(script.toString());

		System.out.println(script);

		// saving model in PMML
		re.eval("library(pmml)");
		re.eval("modelpmml <- pmml(model)");
		re.eval("write(toString(modelpmml),file = 'model.pmml')");

		REXP y = re.eval("coef(model)[['Age']]");
		// RVector x = re.eval("model").asVector();

		System.out.println(x);
		System.out.println(y);

	}

}
