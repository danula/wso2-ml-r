package org.wso2.carbon.ml.extension;

import java.util.ArrayList;
import java.util.List;

import org.rosuda.JRI.*;
import org.wso2.carbon.ml.model.internal.dto.MLFeature;
import org.wso2.carbon.ml.model.internal.dto.MLWorkflow;

public class r_extension {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// new R-engine
		Rengine re = new Rengine(new String[] { "--vanilla" }, false, null);
		if (!re.waitForR()) {
			System.out.println("Cannot load R");
			return;
		}

		// creating a sample ML workflow
		MLWorkflow mlWorkflow = new MLWorkflow();
		List<MLFeature> featuresList = new ArrayList<MLFeature>();

		MLFeature mlFeature1 = new MLFeature();
		mlFeature1.setInclude(true);
		mlFeature1.setName("Age");
		mlFeature1.setType("NUMERICAL");
		mlFeature1.setImputeOption("REPLACE_WTH_MEAN");
		featuresList.add(mlFeature1);

		MLFeature mlFeature2 = new MLFeature();
		mlFeature2.setInclude(true);
		mlFeature2.setName("Class");
		mlFeature2.setType("CATEGORICAL");
		mlFeature2.setImputeOption("REPLACE_WTH_MEAN");
		featuresList.add(mlFeature2);

		MLFeature mlFeature3 = new MLFeature();
		mlFeature3.setInclude(true);
		mlFeature3.setName("NumPregnancies");
		mlFeature3.setType("CATEGORICAL");
		mlFeature3.setImputeOption("DISCARD");
		featuresList.add(mlFeature3);

		MLFeature mlFeature4 = new MLFeature();
		mlFeature4.setInclude(true);
		mlFeature4.setName("BMI");
		mlFeature4.setType("NUMERICAL");
		mlFeature4.setImputeOption("DISCARD");
		featuresList.add(mlFeature4);

		mlWorkflow.setAlgorithmName("LOGISTIC_REGRESSION");
		mlWorkflow
				.setDatasetURL("/home/danula/Downloads/pIndiansDiabetes_Missing.csv");
		mlWorkflow.setFeatures(featuresList);
		mlWorkflow.setResponseVariable("Class");

		// evaluate MLWorkflow
		evaluate(re, mlWorkflow);

		// print a random number from uniform distribution
		// System.out.println (re.eval ("8+5").asDouble ());

		// done
		re.end();

	}

	private static void evaluate(Rengine re, MLWorkflow mlWorkflow) {
		switch (mlWorkflow.getAlgorithmName()) {
		case "LOGISTIC_REGRESSION":
			logisticRegression(re, mlWorkflow);
		}

	}

	public static void logisticRegression(Rengine re, MLWorkflow mlWorkflow) {
		// load data from csv
		re.eval("input <- read.csv('" + mlWorkflow.getDatasetURL() + "')",
				false);

		List<MLFeature> features = mlWorkflow.getFeatures();

		StringBuffer script = new StringBuffer();
		script.append("result <- glm(" + mlWorkflow.getResponseVariable()
				+ " ~ ");
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
				}

				// impute
				if (feature.getImputeOption().equals("REPLACE_WTH_MEAN")) {
					String name = feature.getName();
					re.eval("temp <- mean(input$" + name + ",na.rm=TRUE)");
					re.eval("input$" + name + "[input$" + name
							+ "==NA] <- temp");
				}
				// removing a row --- newdata <- na.omit(mydata)
			}
		}
		script.append(", data=input, family=binomial(link='logit'))");
		re.eval(script.toString(), false);
		System.out.println(script);

		// re.eval("df <- subset(mtcars, select=c(mpg,am,vs))",false);
		// REXP x = re.eval("coef(result)[['Age']]");
		REXP x = re.eval("coef(result)");
		System.out.println(x);
	}

}
