package org.wso2.carbon.ml.extension;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.rosuda.REngine.JRI.JRIEngine;
import org.rosuda.REngine.*;
import org.wso2.carbon.ml.extension.algorithms.AlgorithmFactory;
import org.wso2.carbon.ml.extension.algorithms.RAlgorithm;
import org.wso2.carbon.ml.extension.bean.MLFeature;
import org.wso2.carbon.ml.extension.bean.MLRWorkflow;
import org.wso2.carbon.ml.extension.exception.EvaluationException;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;
import org.wso2.carbon.ml.extension.utils.CommonConstants;
import org.wso2.carbon.ml.extension.utils.WorkflowParser;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RExtension {

	private final static Logger log = Logger.getLogger(RExtension.class);
	private static REngine rEngine = null;
	private REXP rEnvironment = null;

	/**
	 * Default constructor for {@link RExtension}. Creates a REngine instance
	 * and a R Environment.
	 *
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 */
	public RExtension() throws InitializationException {
		PropertyConfigurator.configure("log4j.properties");
		try {
			log.info("Initializing R Engine");
			rEngine = JRIEngine.createEngine();
			rEnvironment = rEngine.newEnvironment(null, true);
		} catch (REngineException e) {
			log.error(e.getMessage());
			throw new InitializationException("Cannot create R Engine", e);
		} catch (REXPMismatchException e) {
			log.error(e.getMessage());
			throw new InitializationException("Cannot create R Environment", e);
		}
	}

	/**
	 * Destroy the REngine
	 */
	public void destroyREngine() {
		log.info("Destroying R Engine");
		if (RExtension.rEngine != null)
			RExtension.rEngine.close();
	}

	/**
	 * Generates the model for {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}.
	 * Exports PMML file to the default location.
	 *
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow} bean
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void generateModel(MLRWorkflow mlRWorkflow) throws EvaluationException {
		generateModel(mlRWorkflow, CommonConstants.DEFAULT_EXPORT_PATH.toString());
	}

	/**
	 * Generates the model for {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}.
	 * Parses JSON mapped workflow form the given URL.
	 * Exports PMML file to the default location.
	 *
	 * @param workflowURL absolute location of the JSON mapped workflow
	 * @throws org.wso2.carbon.ml.extension.exception.FormattingException
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void generateModel(String workflowURL)
			throws FormattingException, InitializationException, EvaluationException {
		generateModel(workflowURL, CommonConstants.DEFAULT_EXPORT_PATH.toString());
	}

	/**
	 * Generates the model for {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}.
	 * Parses JSON mapped workflow form the given URL.
	 *
	 * @param workflowURL absolute location of the JSON mapped workflow
	 * @param exportPath  absolute path to the exported PMML file
	 * @throws org.wso2.carbon.ml.extension.exception.FormattingException
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void generateModel(String workflowURL, String exportPath)
			throws FormattingException, InitializationException, EvaluationException {

		if (workflowURL == null) {
			log.error("Cannot parse workflow. Workflow URL is null");
			throw new InitializationException("Workflow URL is null");
		}
		WorkflowParser parser = new WorkflowParser();
		MLRWorkflow MLRWorkflow = parser.parseWorkflow(workflowURL);
		generateModel(MLRWorkflow, exportPath);
	}

	/**
	 * Generates the model for {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
	 *
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow} bean
	 * @param exportPath  absolute path to export PMML
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void generateModel(MLRWorkflow mlRWorkflow, String exportPath)
			throws EvaluationException {
		runScript(mlRWorkflow, exportPath);
	}

	/**
	 * Runs the script in R and export the model as a PMML file.
	 *
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
	 * @param exportPath  absolute path to export PMML
	 * @throws EvaluationException
	 */
	private void runScript(MLRWorkflow mlRWorkflow, String exportPath) throws EvaluationException {

		if (mlRWorkflow == null) {
			log.error("Workflow is null. Script cannot be generated. ");
			throw new EvaluationException("Workflow is null");
		}

		StringBuilder formula = generateFormula(mlRWorkflow);
		AlgorithmFactory algorithmFactory = AlgorithmFactory.getAlgorithmFactory();

		if(algorithmFactory == null){
			throw new EvaluationException("Unexpected error creating AlgorithmFactory.");
		}

		RAlgorithm algorithm = algorithmFactory.getAlgorithmObject(mlRWorkflow.getAlgorithmName());

		if(algorithm == null) {
			throw new EvaluationException("Unexpected error instantiating the algorithm class.");
		}

		ArrayList<String> trainScript = algorithm.generateScript(mlRWorkflow, formula);
		ArrayList<String> exportScript;

		if (trainScript == null || trainScript.size() == 0) {
			log.error("Training script is empty. ");
			throw new EvaluationException("Training script cannot be empty.");
		}

		REXP out;
		try {
			log.info("Executing training script in R.");
			out = executeScriptInR(trainScript, true);

			StringBuilder parameters = new StringBuilder();
			parameters.append(formula).append(",data=" + CommonConstants.DATASET);

			if (out == null) {
				log.error("Model parameters cannot be tuned");
				throw new EvaluationException(
						"Unexpected error occurred while tuning parameters. ");
			}

			log.info("Extracting tuned parameters.");
			String[] names = out._attr().asList().at("names").asStrings();
			RList values = out.asList();

			for (int i = 0; i < names.length; ++i) {
				parameters.append(",").append(names[i]).append("=")
				          .append(getDataElement(values.at(i)));
			}

			if (exportPath == null) {
				log.warn("Export path is null. Skipping exporting process");
			}

			log.info("Generating exporting script");
			exportScript = algorithm.generatePMML(parameters, mlRWorkflow);

			if (exportScript == null || exportScript.size() == 0) {
				log.warn("Export method not specified. Skipping exporting process.");
				return;
			}
			exportScript.add("write(toString(" + CommonConstants.PMML_MODEL + "),file = '" +
			                 Paths.get(exportPath).toString() + "')");
			log.info("Executing exporting script in R");
			executeScriptInR(exportScript, false);
			log.info("Model successfully saved at " + Paths.get(exportPath).toString());

		} catch (REngineException e) {
			throw new EvaluationException("Requested operation cannot be executed in R. ");
		} catch (REXPMismatchException e) {
			throw new EvaluationException(
					"Operation requested is not supported by the given R object type");
		} finally {
			destroyREngine();
		}
	}

	/**
	 * Execute a given list of scripts in R.
	 *
	 * @param script list of R script
	 * @param requireOutput whether an output is required
	 * @return output {@link org.rosuda.REngine.REXP}
	 * @throws REXPMismatchException
	 * @throws REngineException
	 */
	public REXP executeScriptInR(List<String> script, boolean requireOutput)
			throws REXPMismatchException, REngineException {
		REXP out = null;
		for (String line : script) {
			log.trace(line);
			out = rEngine.parseAndEval(line, rEnvironment, requireOutput);
		}

		return out;
	}

	/**
	 * Execute a given script line in R.
	 *
	 * @param script single line of script
	 * @param requireOutput whether an output is required
	 * @return output {@link org.rosuda.REngine.REXP}
	 * @throws REXPMismatchException
	 * @throws REngineException
	 */
	public REXP executeLineInR(String script, boolean requireOutput)
			throws REXPMismatchException, REngineException {
		log.trace(script);
		return rEngine.parseAndEval(script, rEnvironment, requireOutput);
	}

	/**
	 * Generates model formula.
	 *
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
	 * @return formula of the model
	 * @throws EvaluationException
	 */
	private StringBuilder generateFormula(MLRWorkflow mlRWorkflow) throws EvaluationException {
		log.info("Generating model formula");
		StringBuilder formula = new StringBuilder();

		try {
			log.info("Reading dataset. ");
			rEngine.parseAndEval(
					CommonConstants.DATASET + " <- read.csv('" + mlRWorkflow.getDatasetURL() + "')",
					rEnvironment, false);
			log.trace("input <- read.csv('" + mlRWorkflow.getDatasetURL() + "')");

			List<MLFeature> features = mlRWorkflow.getFeatures();

			if (mlRWorkflow.getAlgorithmClass().equals(CommonConstants.CLASSIFICATION)) {
				log.debug("Algorithm type: " + CommonConstants.CLASSIFICATION);
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

						if (feature.getType().equals(CommonConstants.CATEGORICAL))
							defineCategoricalData(feature);

						impute(feature);
					}
				}
			} else if (mlRWorkflow.getAlgorithmClass().equals(CommonConstants.CLUSTERING)) {
				log.debug("Algorithm type: " + CommonConstants.CLUSTERING);
				formula.append("x = input$");
				formula.append(mlRWorkflow.getResponseVariable());

				for (MLFeature feature : features) {
					if (feature.isInclude()) {

						if (feature.getType().equals(CommonConstants.CATEGORICAL))
							defineCategoricalData(feature);

						impute(feature);
					}
				}
			}
		} catch (REngineException e) {
			log.error(e.getMessage());
			throw new EvaluationException("Operation requested cannot be executed in R");
		} catch (REXPMismatchException e) {
			log.error(e.getMessage());
			throw new EvaluationException(
					"Operation requested is not supported by the given R object type");
		}

		return formula;
	}

	/**
	 * Perform imputation for missing data.
	 *
	 * @param feature {@link org.wso2.carbon.ml.extension.bean.MLFeature}
	 * @throws REngineException
	 * @throws REXPMismatchException
	 */
	private void impute(MLFeature feature) throws REngineException, REXPMismatchException {
		String name = feature.getName();
		if (feature.getImputeOption().equals(CommonConstants.MEAN_REPLACE)) {

			log.debug("#Impute - Replacing with mean " + name);
			rEngine.parseAndEval("temp <- mean(input$" + name + ",na.rm=TRUE)", rEnvironment,
			                     false);
			log.trace("temp <- mean(input$" + name + ",na.rm=TRUE)");
			rEngine.parseAndEval("input$" + name + "[is.na(input$" + name + ")] <- temp",
			                     rEnvironment, false);
			log.trace("input$" + name + "[is.na(input$" + name + ")] <- temp");
		} else if (feature.getImputeOption().equals(CommonConstants.DISCARD)) {
			log.debug("#Impute - discard " + name);
			rEngine.parseAndEval("input <- input[complete.cases(input$" + name + "),]",
			                     rEnvironment, false);
			log.trace("input <- input[complete.cases(input$" + name + "),]");
		}
	}

	/**
	 * Defines categorical data as a factor in R
	 *
	 * @param feature {@link org.wso2.carbon.ml.extension.bean.MLFeature}
	 * @throws REngineException
	 * @throws REXPMismatchException
	 */
	private void defineCategoricalData(MLFeature feature)
			throws REngineException, REXPMismatchException {
		String name = feature.getName();
		log.debug("#Define as categorical : " + name);
		rEngine.parseAndEval(CommonConstants.DATASET + "$" + name + "<- factor(input$" + name + ")",
		                     rEnvironment, false);
		log.trace(CommonConstants.DATASET + "$" + name + "<- factor(input$" + name + ")");

	}

	/**
	 * Returns an element of a R generated object. {@link org.rosuda.REngine.REXP}
	 *
	 * @param rexp {@link org.rosuda.REngine.REXP}
	 * @return data element
	 */
	private String getDataElement(REXP rexp) {
		if (rexp instanceof REXPInteger) {
			return Integer.toString(((REXPInteger) rexp).asIntegers()[0]);
		} else if (rexp instanceof REXPDouble) {
			return Double.toString(((REXPDouble) rexp).asDoubles()[0]);
		} else if (rexp instanceof REXPString) {
			return ((REXPString) rexp).asStrings()[0];
		} else if (rexp instanceof REXPLogical) {
			return ((REXPLogical) rexp).asStrings()[0];
		} else if (rexp instanceof REXPSymbol) {
			return ((REXPSymbol) rexp).asStrings()[0];
		}
		return null;
	}
}
