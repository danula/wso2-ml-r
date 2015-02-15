package org.wso2.carbon.ml.extension;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.rosuda.REngine.JRI.JRIEngine;
import org.rosuda.REngine.*;
import org.wso2.carbon.ml.extension.bean.MLFeature;
import org.wso2.carbon.ml.extension.bean.MLRWorkflow;
import org.wso2.carbon.ml.extension.exception.EvaluationException;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;
import org.wso2.carbon.ml.extension.utils.CommonConstants;
import org.wso2.carbon.ml.extension.utils.WorkflowParser;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class RExtension {

	private final static Logger LOGGER = Logger.getLogger(RExtension.class);
	private static REngine rEngine = null;
    private REXP rEnvironment = null;

	/**
	 * Default constructor for {@link RExtension}. Creates a REngine instance
	 * and a R Environment.
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 */
	public RExtension() throws InitializationException{
		PropertyConfigurator.configure("log4j.properties");
		try {
            LOGGER.info("Initializing R Engine");
			rEngine = JRIEngine.createEngine();
            rEnvironment = rEngine.newEnvironment(null, true);
		} catch (REngineException e) {
			LOGGER.error(e.getMessage());
			throw new InitializationException("Cannot create R Engine", e);
		} catch (REXPMismatchException e) {
            LOGGER.error(e.getMessage());
            throw new InitializationException("Cannot create R Environment", e);
        }
    }

	/**
	 * Destroy the REngine
	 */
	public void destroy(){
        LOGGER.info("Destroying R Engine");
		if(RExtension.rEngine != null)
			RExtension.rEngine.close();
	}

	/**
	 * Evaluates {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}. Exports PMML file to the default location.
	 * 
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow} bean
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(MLRWorkflow mlRWorkflow) throws EvaluationException {
		runScript(mlRWorkflow, CommonConstants.DEFAULT_EXPORT_PATH.toString());
	}

	/**
	 * Evaluates {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}. Parses JSON mapped workflow form the given
	 * URL. Exports PMML file to the default location.
	 * 
	 * @param workflowURL
	 *            absolute location of the JSON mapped workflow
	 * @throws org.wso2.carbon.ml.extension.exception.FormattingException
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(String workflowURL) throws FormattingException, InitializationException, EvaluationException {
		evaluate(workflowURL, CommonConstants.DEFAULT_EXPORT_PATH.toString());
	}

	/**
	 * Evaluates {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}. Parses JSON mapped workflow form the given
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
		MLRWorkflow MLRWorkflow = parser.parseWorkflow(workflowURL);
		runScript(MLRWorkflow, exportPath);
	}

	/**
	 * Evaluates {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
	 *
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow} bean
	 * @param exportPath
	 *            absolute path to export PMML
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(MLRWorkflow mlRWorkflow, String exportPath) throws EvaluationException {
		runScript(mlRWorkflow, exportPath);
	}

    /**]
     * Manages script generation process.
     * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
     * @param exportPath absolute path to export PMML
     * @throws EvaluationException
     */
	private void runScript(MLRWorkflow mlRWorkflow, String exportPath) throws EvaluationException {
		StringBuilder formula = new StringBuilder();

		try {
			REXP bestTune = null;

			LOGGER.debug("#Reading CSV : " + mlRWorkflow.getDatasetURL());
			rEngine.parseAndEval("input <- read.csv('" + mlRWorkflow.getDatasetURL() + "')", rEnvironment, false);
			LOGGER.trace("input <- read.csv('/home/madawa/WSO2/Training/Project/workspace/wso2-ml-r/"+ mlRWorkflow.getDatasetURL()+"')");

			List<MLFeature> features = mlRWorkflow.getFeatures();

			if (mlRWorkflow.getAlgorithmClass().equals("Classification")) {
				formula.append(mlRWorkflow.getResponseVariable());
				formula.append(" ~ ");

				boolean flag = false;

				for (MLFeature feature : features) {
					if (feature.isInclude()) {
						if (!mlRWorkflow.getResponseVariable().equals(feature.getName())) {
							if (flag)
								formula.append("+");
							formula.append(feature.getName());
							flag = true;
						}

						if (feature.getType().equals("CATEGORICAL"))
							defineCategoricalData(feature);

						impute(feature);
					}
				}
				bestTune = trainModel(mlRWorkflow, formula);
				exportTrainedModel(mlRWorkflow, formula, bestTune, exportPath);
			} else if (mlRWorkflow.getAlgorithmClass().equals("Clustering")) {
				formula.append("x = input$");
				formula.append(mlRWorkflow.getResponseVariable());

				for (MLFeature feature : features) {
					if (feature.isInclude()) {

						if (feature.getType().equals("CATEGORICAL"))
							defineCategoricalData(feature);

						impute(feature);
					}
				}
				clusterData(mlRWorkflow, formula, exportPath);
			}
		} catch (REngineException e) {
			LOGGER.error(e.getMessage());
			throw new EvaluationException("Operation requested cannot be executed in R", e);
		} catch (REXPMismatchException e) {
			LOGGER.error(e.getMessage());
			throw new EvaluationException("Operation requested is not supported by the given R object type", e);
		}
	}

    /**
     * Trains the model and choose optimized parameters.
     * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
     * @param formula formula of the model
     * @return the {@link org.rosuda.REngine.REXP} that contains the best parameters
     * @throws REngineException
     * @throws REXPMismatchException
     */
    private REXP trainModel(MLRWorkflow mlRWorkflow, StringBuilder formula) throws REngineException,
	                                                                  REXPMismatchException {
		rEngine.parseAndEval("library('caret')");
		LOGGER.trace("library('caret')");

        StringBuilder trainControl = appendControlParameters(mlRWorkflow.getTrainControls());
        LOGGER.trace(trainControl.toString());
		rEngine.parseAndEval(trainControl.toString());
		/*Should install klaR, MASS and is libraries: ADD to documentation*/

		StringBuilder script = new StringBuilder();

		script.append("model <- train(").append(formula).append(", method =");

		script.append("'").append(CommonConstants.ALGORITHM_MAP.get(mlRWorkflow.getAlgorithmName())).append("',data=input");

		// appending parameters to the script
		Map<String, String> hyperParameters = mlRWorkflow.getHyperParameters();
		if(appendHyperParameters(hyperParameters)){
			script.append(",tuneGrid=tuneGrid");
		}
		script.append(",trControl=train_control)");
		LOGGER.trace(script.toString());


		// evaluating the R script
		rEngine.parseAndEval(script.toString(), rEnvironment, false);
		rEngine.parseAndEval("prediction<-predict(model,input[-match('" + mlRWorkflow.getResponseVariable() + "',names(input))])", rEnvironment, false);
		LOGGER.trace("prediction<-predict(model,input[-match('" + mlRWorkflow.getResponseVariable() + "',names(input))])");

		REXP out = rEngine.parseAndEval("confusionMatrix(prediction,input$"+ mlRWorkflow.getResponseVariable() +")", rEnvironment, true);
		LOGGER.trace("confusionMatrix(prediction,input$" + mlRWorkflow.getResponseVariable() + ")");
		return rEngine.parseAndEval("model$bestTune", rEnvironment, true);
	}

    private StringBuilder appendControlParameters(Map<String, String> trainControls){
        StringBuilder trainControl = new StringBuilder("train_control <- trainControl(");
        boolean first = true;
        for(Map.Entry<String, String> entry : trainControls.entrySet()){
            if(first)
                first = false;
            else
                trainControl.append(", ");

            if(entry.getKey().equals("method")){
                trainControl.append(entry.getKey()).append("='").append(entry.getValue()).append("'");
                continue;
            }

            trainControl.append(entry.getKey()).append("=").append(entry.getValue());
        }
        trainControl.append(")");

        return  trainControl;
    }

	private boolean appendHyperParameters(Map<String, String> hyperParameters) throws REXPMismatchException, REngineException {
		StringBuilder tuneGrid = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> entry : hyperParameters.entrySet()) {
			if(first){
				tuneGrid.append("tuneGrid <-  expand.grid(");
				first = false;
			}else {
				tuneGrid.append(",");
			}
			tuneGrid.append(entry.getKey()).append("=").append(entry.getValue());
		}
		if(!first) tuneGrid.append(")");
		rEngine.parseAndEval(tuneGrid.toString(), rEnvironment, false);
		return !first;
	}

	private void impute(MLFeature feature) throws REngineException, REXPMismatchException {
		String name = feature.getName();
		if (feature.getImputeOption().equals("REPLACE_WTH_MEAN")) {

			LOGGER.debug("#Impute - Replacing with mean " + name);
			rEngine.parseAndEval("temp <- mean(input$" + name + ",na.rm=TRUE)", rEnvironment, false);
			LOGGER.trace("temp <- mean(input$" + name + ",na.rm=TRUE)");
			rEngine.parseAndEval("input$" + name + "[is.na(input$" + name + ")] <- temp", rEnvironment, false);
			LOGGER.trace("input$" + name + "[is.na(input$" + name + ")] <- temp");
		} else if (feature.getImputeOption().equals("DISCARD")) {
			LOGGER.debug("#Impute - discard " + name);
			rEngine.parseAndEval("input <- input[complete.cases(input$" + name + "),]", rEnvironment, false);
			LOGGER.trace("input <- input[complete.cases(input$" + name + "),]");
		}
	}

	private void defineCategoricalData(MLFeature feature) throws REngineException,
	                                                               REXPMismatchException {
		String name = feature.getName();
		LOGGER.debug("#Define as categorical : " + name);
		rEngine.parseAndEval("input$" + name + "<- factor(input$" + name + ")", rEnvironment, false);
		LOGGER.trace("input$" + name + "<- factor(input$" + name + ")");

	}

    /**
     * Generates cluster model using kmeans algorithm
     * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
     * @param formula formula of the model
     * @param exportPath absolute path to export
     * @throws REXPMismatchException
     * @throws REngineException
     */
    private void clusterData(MLRWorkflow mlRWorkflow, StringBuilder formula, String exportPath) throws REXPMismatchException, REngineException {
		StringBuilder clusterScript = new StringBuilder("model <- ");
		clusterScript.append(CommonConstants.ALGORITHM_MAP.get(mlRWorkflow.getAlgorithmName())).append("(").append(formula.toString());

		Map<String, String> hyperParameters = mlRWorkflow.getHyperParameters();
		for(Map.Entry<String, String> entry : hyperParameters.entrySet()){
			clusterScript.append(",").append(entry.getKey()).append("=").append(entry.getValue());
		}

		clusterScript.append(")");
		LOGGER.trace(clusterScript.toString());
		rEngine.parseAndEval(clusterScript.toString(), rEnvironment, false);
		LOGGER.trace("library('pmml')");
		rEngine.parseAndEval("library('pmml')", rEnvironment, false);
		LOGGER.trace("modelPmml <- pmml(model)");
		rEngine.parseAndEval("modelPmml <- pmml(model)", rEnvironment, false);

		LOGGER.trace("write(toString(modelpmml),file = '"+exportPath+"')");
		rEngine.parseAndEval("write(toString(modelPmml),file = '" + exportPath + "')", rEnvironment, false);
		LOGGER.debug("#Export Success - Path: " + exportPath);
	}

    /**
     * Returns an element of a R generated object. {@link org.rosuda.REngine.REXP}
     * @param rexp {@link org.rosuda.REngine.REXP}
     * @return data element
     */
    public String getDataElement(REXP rexp){
        if(rexp instanceof REXPInteger){
            return Integer.toString(((REXPInteger) rexp).asIntegers()[0]);
        }else if(rexp instanceof REXPDouble){
            return Double.toString(((REXPDouble)rexp).asDoubles()[0]);
        }else if(rexp instanceof REXPString){
            return ((REXPString)rexp).asStrings()[0];
        }else if(rexp instanceof REXPLogical){
            return ((REXPLogical)rexp).asStrings()[0];
        }else if(rexp instanceof REXPSymbol){
            return ((REXPSymbol)rexp).asStrings()[0];
        }
        return null;
    }

    /**
     * Exports the trained model to PMML
     * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
     * @param formula formula of the model
     * @param bestTune tuned parameters
     * @param exportPath absolute path to export
     * @throws REXPMismatchException
     * @throws REngineException
     */
	private void exportTrainedModel(MLRWorkflow mlRWorkflow, StringBuilder formula, REXP bestTune, String exportPath) throws REXPMismatchException, REngineException {

		StringBuilder parameters = new StringBuilder();

		parameters.append(formula).append(",data=input");

		String[] names = bestTune._attr().asList().at("names").asStrings();
		RList values = bestTune.asList();

		for(int i = 0; i < names.length; ++i) {
			parameters.append(",").append(names[i]).append("=").append(getDataElement(values.at(i)));
		}

		LOGGER.debug("#Exporting to PMML. Using library pmml");
		LOGGER.trace("library('pmml')");
		rEngine.parseAndEval("library('pmml')");
		switch(mlRWorkflow.getAlgorithmName()){
			case "NAIVE_BAYES":
				LOGGER.trace("library('e1071')");
				rEngine.parseAndEval("library('e1071')");
				LOGGER.trace("bestModel<- naiveBayes("+parameters.toString()+")");
				rEngine.parseAndEval("bestModel<- naiveBayes(" + parameters.toString() + ")", rEnvironment, false);
				LOGGER.trace("modelpmml <- pmml(bestModel, dataset=input, predictedField=\""+ mlRWorkflow.getResponseVariable()+"\")");
				rEngine.parseAndEval("modelpmml <- pmml(bestModel, dataset=input, predictedField=\"" + mlRWorkflow.getResponseVariable() + "\")", rEnvironment, false);
				return;
			case "LOGISTIC_REGRESSION":
				LOGGER.trace("bestModel <- model$finalModel");
				rEngine.parseAndEval("modelPmml <- pmml(model$finalModel)", rEnvironment, false);
				break;
			case "LINEAR_REGRESSION":
				LOGGER.trace("bestModel <- lm("+formula.toString()+",data=input)");
				rEngine.parseAndEval("bestModel <- lm(" + formula.toString() + ",data=input)", rEnvironment, false);
				LOGGER.trace("modelPmml <- pmml(bestModel)");
				rEngine.parseAndEval("modelPmml <- pmml(bestModel)", rEnvironment, false);
				break;
			case "RANDOM_FOREST":
				LOGGER.trace("library('randomForest')");
				rEngine.parseAndEval("library('randomForest')");
				LOGGER.trace("bestModel<- randomForest("+parameters.toString()+")");
				rEngine.parseAndEval("bestModel<- randomForest(" + parameters.toString() + ")");
				LOGGER.trace("modelPmml <- pmml(bestModel)");
				rEngine.parseAndEval("modelPmml <- pmml(bestModel)", rEnvironment, false);
				break;
			case "DECISION_TREES":
                LOGGER.trace("bestModel <- model$finalModel");
                rEngine.parseAndEval("modelPmml <- pmml(model$finalModel)", rEnvironment, false);
                break;
			case "SVM":
                LOGGER.trace("library('e1071')");
                rEngine.parseAndEval("library('e1071')");
                LOGGER.trace("bestModel<- svm("+parameters.toString()+")");
                rEngine.parseAndEval("bestModel<- svm(" + parameters.toString() + ")");
                LOGGER.trace("modelPmml <- pmml(bestModel)");
                rEngine.parseAndEval("modelPmml <- pmml(bestModel)", rEnvironment, false);
				break;
		}

		LOGGER.trace("write(toString(modelPmml),file = '"+exportPath+"')");
		rEngine.parseAndEval("write(toString(modelPmml),file = '" + Paths.get(exportPath).toString() + "')", rEnvironment, false);
		LOGGER.debug("#Export Success - Path: " + exportPath);
	}
}
