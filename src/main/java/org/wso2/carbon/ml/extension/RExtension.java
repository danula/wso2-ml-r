package org.wso2.carbon.ml.extension;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.rosuda.REngine.JRI.JRIEngine;
import org.rosuda.REngine.*;
import org.wso2.carbon.ml.extension.exception.EvaluationException;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;
import org.wso2.carbon.ml.extension.model.MLFeature;
import org.wso2.carbon.ml.extension.model.MLWorkflow;
import org.wso2.carbon.ml.extension.util.Constants;
import org.wso2.carbon.ml.extension.util.WorkflowParser;

import java.util.List;
import java.util.Map;

public class RExtension {

	private final static Logger LOGGER = Logger.getLogger(RExtension.class);
	public static REngine re = null;
	private StringBuilder script;

	/**
	 * Default constructor for {@link RExtension}. Creates a REngine instance.
	 * 
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 */
	public RExtension() throws InitializationException{
		PropertyConfigurator.configure("log4j.properties");
		try {
			re = JRIEngine.createEngine();
		} catch (REngineException e) {
			LOGGER.error(e.getMessage());
			throw new InitializationException("Cannot create R Engine", e);
		}
	}

	/**
	 * Destroy the REngine
	 */
	public void destroy(){
		if(RExtension.re != null)
			RExtension.re.close();
	}

	/**
	 * @return the script
	 */
	public StringBuilder getScript() {
		return script;
	}

	/**
	 * Evaluates {@link MLWorkflow}. Exports PMML file to the default location.
	 * 
	 * @param mlWorkflow
	 *            MLWorkflow bean
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(MLWorkflow mlWorkflow) throws EvaluationException {
		runScript(mlWorkflow, Constants.DEFAULT_EXPORT_PATH);
	}

	/**
	 * Evaluates {@link MLWorkflow}. Parses JSON mapped workflow form the given
	 * URL. Exports PMML file to the default location.
	 * 
	 * @param workflowURL
	 *            absolute location of the JSON mapped workflow
	 * @throws org.wso2.carbon.ml.extension.exception.FormattingException
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(String workflowURL) throws FormattingException, InitializationException, EvaluationException {
		evaluate(workflowURL, Constants.DEFAULT_EXPORT_PATH);
	}

	/**
	 * Evaluates {@link MLWorkflow}. Parses JSON mapped workflow form the given
	 * URL.
	 * 
	 * @param workflowURL
	 *            absolute location of the JSON mapped workflow
	 * @param exportPath
	 *            absolute path to the exported PMML file
	 * @throws org.wso2.carbon.ml.extension.exception.FormattingException
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(String workflowURL, String exportPath) throws FormattingException, InitializationException, EvaluationException {
		WorkflowParser parser = new WorkflowParser();
		MLWorkflow mlWorkflow = parser.parseWorkflow(workflowURL);
		runScript(mlWorkflow, exportPath);
	}

	/**
	 * Evaluates {@link MLWorkflow}
	 *
	 * @param mlWorkflow
	 *            MLWorkflow bean
	 * @param exportPath
	 *            absolute path to the exported PMML file
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(MLWorkflow mlWorkflow, String exportPath) throws EvaluationException {
		runScript(mlWorkflow, exportPath);
	}

	private void runScript(MLWorkflow mlWorkflow, String exportPath) throws EvaluationException {
		StringBuilder formula = new StringBuilder();

		try {
			REXP env = re.newEnvironment(null, true);
			REXP bestTune = null;

			LOGGER.debug("#Reading CSV : " + mlWorkflow.getDatasetURL());
			re.parseAndEval("input <- read.csv('" + mlWorkflow.getDatasetURL() + "')", env, false);
			LOGGER.trace("input <- read.csv('/home/madawa/WSO2/Training/Project/workspace/wso2-ml-r/"+mlWorkflow.getDatasetURL()+"')");

			List<MLFeature> features = mlWorkflow.getFeatures();

			if (mlWorkflow.getAlgorithmClass().equals("Classification")) {
				formula.append(mlWorkflow.getResponseVariable());
				formula.append(" ~ ");

				boolean flag = false;

				for (MLFeature feature : features) {
					if (feature.isInclude()) {
						if (!mlWorkflow.getResponseVariable().equals(feature.getName())) {
							if (flag)
								formula.append("+");
							formula.append(feature.getName());
							flag = true;
						}

						if (feature.getType().equals("CATEGORICAL"))
							defineCategoricalData(feature, env);

						impute(feature, env);
					}
				}
				bestTune = trainModel(mlWorkflow, env, formula);
				exportTrainedModel(mlWorkflow, formula, bestTune, env, exportPath);
			} else if (mlWorkflow.getAlgorithmClass().equals("Clustering")) {
				formula.append("x = input$");
				formula.append(mlWorkflow.getResponseVariable());

				for (MLFeature feature : features) {
					if (feature.isInclude()) {

						if (feature.getType().equals("CATEGORICAL"))
							defineCategoricalData(feature, env);

						impute(feature, env);
					}
				}
				clusterData(mlWorkflow, env, formula, exportPath);
			}
		} catch (REngineException e) {
			LOGGER.error(e.getMessage());
			throw new EvaluationException("Operation requested cannot be executed in R", e);
		} catch (REXPMismatchException e) {
			LOGGER.error(e.getMessage());
			throw new EvaluationException("Operation requested is not supported by the given R object type", e);
		}
	}

	private REXP trainModel(MLWorkflow mlWorkflow, REXP env, StringBuilder formula) throws REngineException,
	                                                                  REXPMismatchException {
		re.parseAndEval("library('caret')");
		LOGGER.trace("library('caret')");


		re.parseAndEval("train_control <- trainControl(method='repeatedcv', number=10, repeats=3)");
		/*Should install klaR, MASS and is libraries: ADD to documentation*/

		script = new StringBuilder();

		re.parseAndEval("train_control <- trainControl(method='cv', number=10)", env, false);
		LOGGER.trace("train_control <- trainControl(method='cv', number=10)");

		script.append("model <- train(").append(formula).append(", method =");

		script.append("'").append(Constants.ALGORITHM_MAP.get(mlWorkflow.getAlgorithmName())).append("',data=input");

		// appending parameters to the script
		Map<String, String> hyperParameters = mlWorkflow.getHyperParameters();
		if(appendParameters(hyperParameters,env)){
			script.append(",tuneGrid=tuneGrid");
		}
		script.append(",trControl=train_control)");
		LOGGER.trace(script.toString());


		// evaluating the R script
		re.parseAndEval(script.toString(), env, false);
		re.parseAndEval("prediction<-predict(model,input[-match('" + mlWorkflow.getResponseVariable() + "',names(input))])", env, false);
		LOGGER.trace("prediction<-predict(model,input[-match('"+mlWorkflow.getResponseVariable()+"',names(input))])");

		REXP out = re.parseAndEval("confusionMatrix(prediction,input$"+mlWorkflow.getResponseVariable() +")", env, true);
		LOGGER.trace("confusionMatrix(prediction,input$"+mlWorkflow.getResponseVariable() +")");

		return re.parseAndEval("model$bestTune", env, true);
	}

	private boolean appendParameters(Map<String, String> hyperParameters,REXP env) throws REXPMismatchException, REngineException {
		StringBuilder script = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> entry : hyperParameters.entrySet()) {
			if(first){
				script.append("tuneGrid <-  expand.grid(");
				first = false;
			}else {
				script.append(",");
			}
			script.append(entry.getKey());
			script.append("=");
			script.append(entry.getValue());
		}
		if(!first) script.append(")");
		re.parseAndEval(script.toString(), env, false);
		return !first;
	}

	private void impute(MLFeature feature, REXP env) throws REngineException, REXPMismatchException {
		String name = feature.getName();
		if (feature.getImputeOption().equals("REPLACE_WTH_MEAN")) {

			LOGGER.debug("#Impute - Replacing with mean " + name);
			re.parseAndEval("temp <- mean(input$" + name + ",na.rm=TRUE)", env, false);
			LOGGER.trace("temp <- mean(input$" + name + ",na.rm=TRUE)");
			re.parseAndEval("input$" + name + "[is.na(input$" + name + ")] <- temp", env, false);
			LOGGER.trace("input$" + name + "[is.na(input$" + name + ")] <- temp");
		} else if (feature.getImputeOption().equals("DISCARD")) {
			LOGGER.debug("#Impute - discard " + name);
			re.parseAndEval("input <- input[complete.cases(input$" + name + "),]", env, false);
			LOGGER.trace("input <- input[complete.cases(input$" + name + "),]");
		}
	}

	private void defineCategoricalData(MLFeature feature, REXP env) throws REngineException,
	                                                               REXPMismatchException {
		String name = feature.getName();
		LOGGER.debug("#Define as categorical : " + name);
		re.parseAndEval("input$" + name + "<- factor(input$" + name + ")", env, false);
		LOGGER.trace("input$" + name + "<- factor(input$" + name + ")");

	}

	private void clusterData(MLWorkflow mlWorkflow, REXP env, StringBuilder formula, String exportPath) throws REXPMismatchException, REngineException {
		StringBuilder clusterScript = new StringBuilder("model <- ");
		clusterScript.append(Constants.ALGORITHM_MAP.get(mlWorkflow.getAlgorithmName())).append("(").append(formula.toString());

		Map<String, String> hyperParameters = mlWorkflow.getHyperParameters();
		for(Map.Entry<String, String> entry : hyperParameters.entrySet()){
			clusterScript.append(",").append(entry.getKey()).append("=").append(entry.getValue());
		}

		clusterScript.append(")");
		LOGGER.trace(clusterScript.toString());
		re.parseAndEval(clusterScript.toString(), env, false);
		LOGGER.trace("library('pmml')");
		re.parseAndEval("library('pmml')", env, false);
		LOGGER.trace("modelPmml <- pmml(model)");
		re.parseAndEval("modelPmml <- pmml(model)", env, false);

		LOGGER.trace("write(toString(modelpmml),file = '"+exportPath+"')");
		re.parseAndEval("write(toString(modelPmml),file = '"+exportPath+"')", env, false);
		LOGGER.debug("#Export Success - Path: " + exportPath);
	}

	private void exportTrainedModel(MLWorkflow mlWorkflow, StringBuilder formula, REXP bestTune, REXP env, String exportPath) throws REXPMismatchException, REngineException {

		StringBuilder parameters = new StringBuilder();

		parameters.append(formula).append(",data=input");

		String[] names = bestTune._attr().asList().at("names").asStrings();
		RList values = bestTune.asList();

		for(int i = 0; i < names.length; ++i) {
			parameters.append(",").append(names[i]).append("=").append(JSONConverter.getDataElement(values.at(i)));
		}

		LOGGER.debug("#Exporting to PMML. Using library pmml");
		LOGGER.trace("library('pmml')");
		re.parseAndEval("library('pmml')");
		switch(mlWorkflow.getAlgorithmName()){
			case "NAIVE_BAYES":
				LOGGER.trace("library('e1071')");
				re.parseAndEval("library('e1071')");
				LOGGER.trace("bestModel<- naiveBayes("+parameters.toString()+")");
				re.parseAndEval("bestModel<- naiveBayes("+parameters.toString()+")",env,false);
				LOGGER.trace("modelpmml <- pmml(bestModel, dataset=input, predictedField=\""+mlWorkflow.getResponseVariable()+"\")");
				re.parseAndEval("modelpmml <- pmml(bestModel, dataset=input, predictedField=\""+mlWorkflow.getResponseVariable()+"\")",env,false);
				return;
			case "LOGISTIC_REGRESSION":
				LOGGER.trace("bestModel <- model$finalModel");
				re.parseAndEval("modelPmml <- pmml(model$finalModel)",env,false);
				break;
			case "LINEAR_REGRESSION":
				LOGGER.trace("bestModel <- lm("+formula.toString()+",data=input)");
				re.parseAndEval("bestModel <- lm(" + formula.toString() + ",data=input)", env, false);
				LOGGER.trace("modelPmml <- pmml(bestModel)");
				re.parseAndEval("modelPmml <- pmml(bestModel)", env, false);
				break;
			case "RANDOM_FOREST":
				LOGGER.trace("library('randomForest')");
				re.parseAndEval("library('randomForest')");
				LOGGER.trace("bestModel<- randomForest("+parameters.toString()+")");
				re.parseAndEval("bestModel<- randomForest("+parameters.toString()+")");
				LOGGER.trace("modelPmml <- pmml(bestModel)");
				re.parseAndEval("modelPmml <- pmml(bestModel)", env, false);
				break;
			case "DECISION_TREES":
                LOGGER.trace("bestModel <- model$finalModel");
                re.parseAndEval("modelPmml <- pmml(model$finalModel)",env,false);
                break;
			case "SVM":
                LOGGER.trace("library('e1071')");
                re.parseAndEval("library('e1071')");
                LOGGER.trace("bestModel<- svm("+parameters.toString()+")");
                re.parseAndEval("bestModel<- svm("+parameters.toString()+")");
                LOGGER.trace("modelPmml <- pmml(bestModel)");
                re.parseAndEval("modelPmml <- pmml(bestModel)", env, false);
				break;

		}

		LOGGER.trace("write(toString(modelPmml),file = '"+exportPath+"')");
		re.parseAndEval("write(toString(modelPmml),file = '"+exportPath+"')", env, false);
		LOGGER.debug("#Export Success - Path: " + exportPath);
	}
}
