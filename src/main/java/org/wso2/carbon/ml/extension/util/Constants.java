package org.wso2.carbon.ml.extension.util;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    /**
     * Maps the algorithm names with R function names
     */
    public static final Map<String, String> ALGORITHM_MAP = new HashMap<>();

    /**
     * ALGORITHM_MAP initializer
     */
    static{
        ALGORITHM_MAP.put("RANDOM_FOREST", "rf");
        ALGORITHM_MAP.put("LOGISTIC_REGRESSION", "bayesglm");
        ALGORITHM_MAP.put("LINEAR_REGRESSION", "glm");
        ALGORITHM_MAP.put("SVM", "svmPoly");
        ALGORITHM_MAP.put("NAIVE_BAYES", "nb");
        ALGORITHM_MAP.put("DECISION_TREES", "rpart");
        ALGORITHM_MAP.put("KMEANS", "kmeans");
    }
}
