package org.wso2.carbon.ml.extension.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CommonConstants {

	/**
	 * Default export path of the PMML model
	 */
	public static final Path DEFAULT_EXPORT_PATH =
			Paths.get("/home/" + System.getProperty("user.name") + "/model.pmml");

	/**
	 * Maps the algorithms names with R function names
	 */
	public static final Map<String, String> ALGORITHM_MAP = new HashMap<>();

	/**
	 * Default train controls
	 */
	public static final Map<String, String> DEFAULT_TRAIN_CONTROLS = new HashMap<>();

	/**
	 * MLRWorkflow related constants
	 */
	public static final String CLASSIFICATION = "Classification";
	public static final String CLUSTERING = "Clustering";
	public static final String CATEGORICAL = "CATEGORICAL";
	public static final String MEAN_REPLACE = "REPLACE_WTH_MEAN";
	public static final String DISCARD = "DISCARD";

	/**
	 * R variable names
	 */
	public static final String DATASET = "input";
	public static final String MODEL = "model";
	public static final String TUNED_PARAMETERS = "model$bestTune";
	public static final String TRAIN_CONTROL_PARAMETERS = "train_control";
	public static final String TUNE_GRID = "tuneGrid";
	public static final String FINAL_MODEL = "model$finalModel";
	public static final String TUNED_MODEL = "bestModel";
	public static final String PMML_MODEL = "pmmlModel";
	public static final String PREDICTION = "prediction";

	/**
	 * Common R libraries
	 */
	public static final String LIBRARY_CARET = "library('caret')";
	public static final String LIBRARY_PMML = "library('pmml')";

	/**
	 * Constant Map Initializer
	 */
	static {
		ALGORITHM_MAP.put("RANDOM_FOREST", "rf");
		ALGORITHM_MAP.put("LOGISTIC_REGRESSION", "multinom");
		ALGORITHM_MAP.put("LINEAR_REGRESSION", "lm");
		ALGORITHM_MAP.put("SVM", "svmPoly");
		ALGORITHM_MAP.put("NAIVE_BAYES", "nb");
		ALGORITHM_MAP.put("DECISION_TREES", "rpart");
		ALGORITHM_MAP.put("KMEANS", "kmeans");

		DEFAULT_TRAIN_CONTROLS.put("method", "repeatedcv");
		DEFAULT_TRAIN_CONTROLS.put("number", "10");
		DEFAULT_TRAIN_CONTROLS.put("repeats", "4");
	}

}
