package org.wso2.carbon.ml.extension.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Constants {

    /**
     * Default export path of the PMML model
     */
    public static final Path DEFAULT_EXPORT_PATH = Paths.get("/home/"+System.getProperty("user.name")+"/model.pmml");
    /**
     * Maps the algorithm names with R function names
     */
    public static final Map<String, String> ALGORITHM_MAP = new HashMap<>();

    /**
     * ALGORITHM_MAP initializer
     */
    static{
        ALGORITHM_MAP.put("RANDOM_FOREST", "rf");
        ALGORITHM_MAP.put("LOGISTIC_REGRESSION", "multinom");
        ALGORITHM_MAP.put("LINEAR_REGRESSION", "lm");
        ALGORITHM_MAP.put("SVM", "svmPoly");
        ALGORITHM_MAP.put("NAIVE_BAYES", "nb");
        ALGORITHM_MAP.put("DECISION_TREES", "rpart");
        ALGORITHM_MAP.put("KMEANS", "kmeans");
    }
}
