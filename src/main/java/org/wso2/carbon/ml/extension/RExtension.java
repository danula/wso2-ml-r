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
import org.wso2.carbon.ml.extension.util.InitializeWorkflow;

import java.util.List;
import java.util.Map;

public class RExtension {

	private final static Logger LOGGER = Logger.getLogger(RExtension.class);
	public static REngine re = null;
	private StringBuffer script;

	/**
	 * Default constructor for {@link RExtension}. Creates a REngine instance.
	 * 
	 * @throws REngineException
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
		RExtension.re.close();
	}

	/**
	 * @return the script
	 */
	public StringBuffer getScript() {
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
		initializeScript(mlWorkflow, "");
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
		evaluate(workflowURL, "");
	}

	/**
	 * Evaluates {@link MLWorkflow}. Parses JSON mapped workflow form the given
	 * URL.
	 * 
	 * @param workflowURL
	 *            absolute location of the JSON mapped workflow
	 * @param exportLocation
	 *            absolute path to the exported PMML file
	 * @throws org.wso2.carbon.ml.extension.exception.FormattingException
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(String workflowURL, String exportLocation) throws FormattingException, InitializationException, EvaluationException {
		InitializeWorkflow init = new InitializeWorkflow();
		MLWorkflow mlWorkflow = init.parseWorkflow(workflowURL);
		initializeScript(mlWorkflow, exportLocation);
	}

	/**
	 * Evaluates {@link MLWorkflow}
	 *
	 * @param mlWorkflow
	 *            MLWorkflow bean
	 * @param exportLocation
	 *            absolute path to the exported PMML file
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(MLWorkflow mlWorkflow, String exportLocation) throws EvaluationException {
		initializeScript(mlWorkflow, exportLocation);
	}

	private void initializeScript(MLWorkflow mlWorkflow, String exportLocation) throws EvaluationException {
		StringBuffer tempBuffer = new StringBuffer();

		try {
			REXP env = re.newEnvironment(null, true);

			LOGGER.debug("#Reading CSV : " + mlWorkflow.getDatasetURL());
			re.parseAndEval("input <- read.csv('" + mlWorkflow.getDatasetURL() + "')", env, false);
			LOGGER.trace("input <- read.csv('" + mlWorkflow.getDatasetURL() + "')");

			List<MLFeature> features = mlWorkflow.getFeatures();

			if (mlWorkflow.getAlgorithmClass().equals("Classification")) {
				// for classification
				tempBuffer.append(mlWorkflow.getResponseVariable());
				tempBuffer.append(" ~ ");

				boolean flag = false;

				for (MLFeature feature : features) {
					if (feature.isInclude()) {
						if (!mlWorkflow.getResponseVariable().equals(feature.getName())) {
							if (flag)
								tempBuffer.append("+");
							tempBuffer.append(feature.getName());
							flag = true;
						}

						if (feature.getType().equals("CATEGORICAL"))
							defineCategoricalData(feature, env);

						impute(feature, env);
					}
				}

				tempBuffer.append(", method =");
				tempBuffer.append("'" + Constants.ALGORITHM_MAP.get(mlWorkflow.getAlgorithmName()) + "'");

				tempBuffer.append(",data=input");

			} else if (mlWorkflow.getAlgorithmClass().equals("Clustering")) {
				// for clustering
				tempBuffer.append("x = input$");
				tempBuffer.append(mlWorkflow.getResponseVariable());

				for (MLFeature feature : features) {
					if (feature.isInclude()) {

						if (feature.getType().equals("CATEGORICAL"))
							defineCategoricalData(feature, env);

						impute(feature, env);
					}
				}

			}

			runScript(mlWorkflow, env, tempBuffer);

		} catch (REngineException e) {
			LOGGER.error(e.getMessage());
			throw new EvaluationException("Operation requested cannot be executed in R", e);
		} catch (REXPMismatchException e) {
			LOGGER.error(e.getMessage());
			throw new EvaluationException("Operation requested is not supported by the given R object type", e);
		}
	}

	private void runScript(MLWorkflow mlWorkflow, REXP env, StringBuffer tempBuffer) throws REngineException,
	                                                                  REXPMismatchException {


		re.parseAndEval("library('caret')");
		LOGGER.trace("library('caret')");
		re.parseAndEval("data(iris)");
		re.parseAndEval("train_control <- trainControl(method='repeatedcv', number=10, repeats=3)");
		/*Should install klaR, MASS and is libraries: ADD to documentation*/
		re.parseAndEval("model <- train(Species~., data=iris, trControl=train_control, method='nb')");


		script = new StringBuffer();

		re.parseAndEval("train_control <- trainControl(method='cv', number=10)", env, false);
		LOGGER.trace("train_control <- trainControl(method='cv', number=10)");

		script.append("model <- train(");

		script.append(tempBuffer);

		// appending parameters to the script
		Map<String, String> hyperParameters = mlWorkflow.getHyperParameters();
		if(appendParameters(hyperParameters,env)){
			script.append(",tuneGrid=tuneGrid");
		}
		script.append(",trControl=train_control)");
		LOGGER.trace(script.toString());


		// evaluating the R script
		re.parseAndEval(script.toString(), env, false);
		re.parseAndEval("prediction<-predict(model,input[-match('"+mlWorkflow.getResponseVariable()+"',names(input))])", env, false);
		LOGGER.trace("prediction<-predict(model,input[-match('"+mlWorkflow.getResponseVariable()+"',names(input))])");

		REXP out = re.parseAndEval("confusionMatrix(prediction,input$"+mlWorkflow.getResponseVariable() +")", env, true);
		LOGGER.trace("confusionMatrix(prediction,input$"+mlWorkflow.getResponseVariable() +")");
		//out = re.parseAndEval("prediction", env, true);
		System.out.println(out.toDebugString());
		//print(out);

		//REXP inp = re.parseAndEval("input", env, true);
		//System.out.println(inp.toDebugString());

		//System.out.println(x.toDebugString());
		// exporting the model in PMML format
		//exportToPMML(env, exportLocation);

	}

	private String[] getNames(REXP rexp){
		RList rl = rexp._attr().asList();
		return ((REXPString) rl.at("names")).asStrings();
	}


	private void print(REXP e) {
		//System.out.println(e.toDebugString());
		if(e instanceof REXPNull){

		}
		else if(e.isList()) {
			//if(e instanceof REXPGenericVector) {
			RList rl ;
			if(e instanceof REXPGenericVector) rl = ((REXPGenericVector)e).asList();
			else rl = ((REXPList)e).asList();
			for(int i=0;i<rl.size();i++){
				System.out.println(rl.keyAt(i));
				//System.out.println(rl.elementAt(i).getClass());
				print(rl.at(i));
			}

		}
		else if(e instanceof REXPString){
			//System.out.println("REXPString");
			String[] arr = ((REXPString)e).asStrings();
			for(String s:arr){
				System.out.print(s + " ");
			}

		}
		else if(e instanceof REXPSymbol){
			//System.out.println("REXPString");
			String[] arr = ((REXPSymbol)e).asStrings();
			for(String s:arr){
				System.out.print(s + " ");
			}

		}
		else if(e instanceof REXPList){
			System.out.println("REXPList");
		}
		else if(e instanceof REXPInteger){
			//System.out.println("REXPInt");
			RList rl1=null;
			REXP type =null;

			if(e._attr()!=null) {
				rl1 = e._attr().asList();
				type = rl1.at("names");
			}
			String[] names = null;
			if(type!=null) {
				names = ((REXPString) rl1.at("names")).asStrings();
				int[] arr = ((REXPInteger)e).asIntegers();
				for(int i=0;i<arr.length;i++){
					if(rl1.at("class")==null)System.out.print(names[i]);
					System.out.println(arr[i]);
				}

			}else if(rl1.at("dim")!=null) {
				int rows = ((REXPInteger) rl1.at("dim")).asIntegers()[0];
				int cols = ((REXPInteger) rl1.at("dim")).asIntegers()[1];
				String[] axislabels = null;
				String[] rowNames = null;
				String[] colNames = null;

				if(rl1.at("dimnames")!=null) {
					if(rl1.at("dimnames")._attr()!=null) {
						axislabels = ((REXPString) rl1.at("dimnames")._attr().asList().at("names")).asStrings();
					}


					if (!((((REXPGenericVector) rl1.at("dimnames")).asList().at(0)) instanceof REXPNull)) {
						rowNames = ((REXPString) ((REXPGenericVector) rl1.at("dimnames")).asList().at(0)).asStrings();
						colNames = ((REXPString) ((REXPGenericVector) rl1.at("dimnames")).asList().at(1)).asStrings();
					}
				}
				int[] arr = ((REXPInteger)e).asIntegers();
				if(axislabels!=null)System.out.println("\t\t"+axislabels[0]);
				for(int i=-1;i<rows;i++){
					for(int j=-1;j<cols;j++){
						if(i==-1&&j==-1){
							if(axislabels!=null)System.out.print(axislabels[1]+"\t");
						}
						else if(i==-1) {
							if(colNames!=null) System.out.print(colNames[j]+"\t");
						}
						else if(j==-1) {
							if(rowNames!=null) System.out.print("\t"+rowNames[i]+"\t");
						}
						else System.out.print(arr[i*cols+j]+"\t");
					}
					System.out.println();
				}
			}

			System.out.println();

		}
		else if(e instanceof REXPDouble){
			RList rl1=null;
			REXP type =null;

			if(e._attr()!=null) {
				rl1 = e._attr().asList();
				type = rl1.at("names");
			}
			String[] names = null;
			if(type!=null) {
				names = ((REXPString) rl1.at("names")).asStrings();
				double[] arr = ((REXPDouble)e).asDoubles();
				for(int i=0;i<arr.length;i++){
					System.out.print(names[i]);
					System.out.println(arr[i]);
				}

			}else if(e._attr()!=null){
				int rows = ((REXPInteger) rl1.at("dim")).asIntegers()[0];
				int cols = ((REXPInteger) rl1.at("dim")).asIntegers()[1];

				String[] axislabels = null;
				String[] rowNames = null;
				String[] colNames = null;

				if(rl1.at("dimnames")!=null) {
					if (rl1.at("dimnames")._attr() != null) {
						axislabels = ((REXPString) rl1.at("dimnames")._attr().asList().at("names")).asStrings();
					}

					if (!((((REXPGenericVector) rl1.at("dimnames")).asList().at(0)) instanceof REXPNull)) {
						rowNames = ((REXPString) ((REXPGenericVector) rl1.at("dimnames")).asList().at(0)).asStrings();
						colNames = ((REXPString) ((REXPGenericVector) rl1.at("dimnames")).asList().at(1)).asStrings();
					}
				}
				double[] arr = ((REXPDouble)e).asDoubles();
				if(axislabels!=null)System.out.println("\t\t"+axislabels[0]);
				for(int i=-1;i<rows;i++){
					for(int j=-1;j<cols;j++){
						if(i==-1&&j==-1){
							if(axislabels!=null)System.out.print(axislabels[1]+"\t");
						}
						else if(i==-1) {
							if(colNames!=null) System.out.print(colNames[j]+"\t");
						}
						else if(j==-1) {
							if(rowNames!=null) System.out.print("\t"+rowNames[i]+"\t");
						}
						else System.out.print(arr[i*cols+j]+"\t");
					}
					System.out.println();
				}
			}
		}
		else if(e instanceof REXPSymbol){

		}
		else System.out.println(e.toDebugString());

	}

	private boolean appendParameters(Map<String, String> hyperParameters,REXP env) throws REXPMismatchException, REngineException {
		StringBuffer script = new StringBuffer();
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
		re.parseAndEval(script.toString(),env,false);
		System.out.println(script.toString());
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

	private void exportToPMML(REXP env, String exportLocation) throws REngineException,
	                                                          REXPMismatchException {

		LOGGER.debug("#Exporting to PMML");
		LOGGER.debug("#Using library pmml");
		LOGGER.trace("library(pmml)");
		re.parseAndEval("library(pmml)", env, false);
		LOGGER.trace("modelpmml <- pmml(model$finalModel)");
		re.parseAndEval("modelpmml <- pmml(model$finalModel)", env, false);

		StringBuffer locationBuffer = new StringBuffer(exportLocation);

		StringBuffer buffer = new StringBuffer("write(toString(modelpmml),file = '");
		buffer.append(locationBuffer);

		if (exportLocation.trim().equals(""))
			buffer.append("model.pmml')");
		else
			buffer.append("')");

		re.parseAndEval(buffer.toString(), env, false);
		LOGGER.trace(buffer.toString());
		LOGGER.debug("#Export Success - Location: " + exportLocation);

	}

}
