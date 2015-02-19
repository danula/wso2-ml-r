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
	 * @param workflowURL absolute location of the JSON mapped workflow
	 * @throws org.wso2.carbon.ml.extension.exception.FormattingException
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(String workflowURL)
			throws FormattingException, InitializationException, EvaluationException {
		evaluate(workflowURL, CommonConstants.DEFAULT_EXPORT_PATH.toString());
	}

	/**
	 * Evaluates {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}. Parses JSON mapped workflow form the given
	 * URL.
	 *
	 * @param workflowURL absolute location of the JSON mapped workflow
	 * @param exportPath  absolute path to the exported PMML file
	 * @throws org.wso2.carbon.ml.extension.exception.FormattingException
	 * @throws org.wso2.carbon.ml.extension.exception.InitializationException
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(String workflowURL, String exportPath)
			throws FormattingException, InitializationException, EvaluationException {
		WorkflowParser parser = new WorkflowParser();
		MLRWorkflow MLRWorkflow = parser.parseWorkflow(workflowURL);
		runScript(MLRWorkflow, exportPath);
	}

	/**
	 * Evaluates {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
	 *
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow} bean
	 * @param exportPath  absolute path to export PMML
	 * @throws org.wso2.carbon.ml.extension.exception.EvaluationException
	 */
	public void evaluate(MLRWorkflow mlRWorkflow, String exportPath) throws EvaluationException {
		runScript(mlRWorkflow, exportPath);
	}

	/**
	 * ]
	 * Manages script generation process.
	 *
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
	 * @param exportPath  absolute path to export PMML
	 * @throws EvaluationException
	 */
	private void runScript(MLRWorkflow mlRWorkflow, String exportPath) throws EvaluationException {
		StringBuilder formula = generateFormula(mlRWorkflow);
		RAlgorithm algorithm = AlgorithmFactory.getAlgorithm(mlRWorkflow.getAlgorithmName());
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

			log.info("Generating exporting script");
			exportScript = algorithm.generatePMML(parameters, mlRWorkflow);

			if (exportScript == null || exportScript.size() == 0) {
				log.info("Export method not specified. Skipping export.");
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

	private REXP executeScriptInR(ArrayList<String> script, boolean requireOutput)
			throws REXPMismatchException, REngineException {
		REXP out = null;
		for (String line : script) {
			log.trace(line);
			out = rEngine.parseAndEval(line, rEnvironment, requireOutput);
		}

		return out;
	}

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
	public String getDataElement(REXP rexp) {
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
