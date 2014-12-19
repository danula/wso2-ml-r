package org.wso2.carbon.ml.extension;

import java.util.List;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;
import org.wso2.carbon.ml.extension.util.InitializeWorkflow;
import org.wso2.carbon.ml.model.internal.dto.MLFeature;
import org.wso2.carbon.ml.model.internal.dto.MLWorkflow;


public class RExtension {
	
	private MLWorkflow mlWorkflow;
	
	public RExtension(){
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
		
		//evaluate MLWorkflow
		evaluate(re, rex.mlWorkflow);
		
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
				
				//define categorical data
				if (feature.getType().equals("CATEGORICAL")) {
					String name = feature.getName();
					re.eval("input$" + name + "<- factor(input$" + name + ")");
				}
				
				//impute 
				if(feature.getImputeOption().equals("REPLACE_WTH_MEAN")){
					String name = feature.getName();
					re.eval("temp <- mean(input$"+name+",na.rm=TRUE)");
					re.eval("input$"+name+"[input$"+name+"==NA] <- temp");
				}
				//removing a row --- newdata <- na.omit(mydata)
			}
		}
		script.append(", data=input, family=binomial(link='logit'))");
		re.eval(script.toString(), false);
		System.out.println(script);

		// re.eval("df <- subset(mtcars, select=c(mpg,am,vs))",false);
		REXP x = re.eval("coef(result)[['Age']]");
		System.out.println(x);
	}

}
