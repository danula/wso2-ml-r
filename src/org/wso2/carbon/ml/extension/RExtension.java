package org.wso2.carbon.ml.extension;

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
		String postfix = null;
		switch (mlWorkflow.getAlgorithmName()) {
		case "LOGISTIC_REGRESSION":
			script.append("glm(");
			postfix = ", data=input, family=binomial(link='logit'))";
			break;
		case "RANDOM_FOREST":
			re.eval("library(randomForest)", false);
			script.append("randomForest(");
			postfix = ",data=input, ntree=50)";
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
					re.eval("temp <- mean(input$" + name + ",na.rm=TRUE)");

					System.out.println("temp <- mean(input$" + name
							+ ",na.rm=TRUE)");

					re.eval("input$" + name + "[input$" + name
							+ "==NA] <- temp");
					System.out.println("input$" + name + "[input$" + name
							+ "==NA] <- temp");
				} else if (feature.getImputeOption().equals("DISCARD")) {
					re.eval("input[complete.cases(input$" + feature.getName()
							+ "),]", false);
				}
				// removing a row --- newdata <- na.omit(mydata)
			}
		}
		script.append(postfix);

		REXP x = re.eval(script.toString());

		System.out.println(script);

		REXP y = re.eval("coef(model)[['Age']]");
		// RVector x = re.eval("model").asVector();

		System.out.println(x);
		System.out.println(y);

	}

}
