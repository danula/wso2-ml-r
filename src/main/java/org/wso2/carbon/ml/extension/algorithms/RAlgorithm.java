package org.wso2.carbon.ml.extension.algorithms;

import org.wso2.carbon.ml.extension.bean.MLRWorkflow;
import org.wso2.carbon.ml.extension.utils.CommonConstants;

import java.util.ArrayList;
import java.util.Map;

public abstract class RAlgorithm {

	/**
	 * Generates script to train the model. Default implementation generates the script to train
	 * the model using <a href="http://caret.r-forge.r-project.org/">caret library</a>.
	 *
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
	 * @param formula     model formula generated
	 * @return list of R script lines.
	 */
	public ArrayList<String> generateScript(MLRWorkflow mlRWorkflow, StringBuilder formula) {
		ArrayList<String> script = new ArrayList<>();
		StringBuilder trainScript = new StringBuilder();

		script.add(CommonConstants.LIBRARY_CARET);
		script.add(appendControlParameters(mlRWorkflow).toString());

		trainScript.append(CommonConstants.MODEL).append(" <- train(").append(formula)
		           .append(", method =");
		trainScript.append("'")
		           .append(CommonConstants.ALGORITHM_MAP.get(mlRWorkflow.getAlgorithmName()))
		           .append("',data=" + CommonConstants.DATASET);

		StringBuilder tuneGrid = appendHyperParameters(mlRWorkflow);

		if (tuneGrid.length() != 0) {
			trainScript.append(",tuneGrid=").append(CommonConstants.TUNE_GRID);
		}

		trainScript.append(",trControl=").append(CommonConstants.TRAIN_CONTROL_PARAMETERS)
		           .append(")");
		script.add(trainScript.toString());
		script.add(CommonConstants.TUNED_PARAMETERS);

		return script;
	}

	/**
	 * Appends the train controls to the script.
	 *
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
	 * @return train controls
	 */
	protected StringBuilder appendControlParameters(MLRWorkflow mlRWorkflow) {
		Map<String, String> trainControls = mlRWorkflow.getTrainControls();
		StringBuilder trainControl =
				new StringBuilder(CommonConstants.TRAIN_CONTROL_PARAMETERS + " <- trainControl(");
		boolean first = true;
		for (Map.Entry<String, String> entry : trainControls.entrySet()) {
			if (first)
				first = false;
			else
				trainControl.append(", ");

			if (entry.getKey().equals("method")) {
				trainControl.append(entry.getKey()).append("='").append(entry.getValue())
				            .append("'");
				continue;
			}

			trainControl.append(entry.getKey()).append("=").append(entry.getValue());
		}
		trainControl.append(")");

		return trainControl;
	}

	/**
	 * Appends the hyper parameters to the script.
	 *
	 * @param mlRWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
	 * @return hyper parameters
	 */
	protected StringBuilder appendHyperParameters(MLRWorkflow mlRWorkflow) {

		Map<String, String> hyperParameters = mlRWorkflow.getHyperParameters();
		StringBuilder tuneGrid = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> entry : hyperParameters.entrySet()) {
			if (first) {
				tuneGrid.append(CommonConstants.TUNE_GRID + " <-  expand.grid(");
				first = false;
			} else {
				tuneGrid.append(",");
			}
			tuneGrid.append(entry.getKey()).append("=").append(entry.getValue());
		}
		if (!first)
			tuneGrid.append(")");
		return tuneGrid;
	}

	/**
	 * Generate script to export R model as a PMML file.
	 *
	 * @param parameters  optimized parameters
	 * @param mlrWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
	 * @return the script to export the PMML model
	 */
	public abstract ArrayList<String> generatePMML(StringBuilder parameters,
	                                               MLRWorkflow mlrWorkflow);

}
