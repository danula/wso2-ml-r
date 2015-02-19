package org.wso2.carbon.ml.extension.algorithms;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class AlgorithmFactory {

	private static final Logger log = Logger.getLogger(AlgorithmFactory.class);

	private static AlgorithmFactory algorithmFactory;
	private Map<String, String> algorithmClasses = new HashMap<>();



	private AlgorithmFactory() {
		algorithmClasses.put("RANDOM_FOREST", "org.wso2.carbon.ml.extension.algorithms.RandomForest");
		algorithmClasses.put("LOGISTIC_REGRESSION", "org.wso2.carbon.ml.extension.algorithms.LogisticRegression");
		algorithmClasses.put("SVM", "org.wso2.carbon.ml.extension.algorithms.SupportVectorMachine");
		algorithmClasses.put("NAIVE_BAYES", "org.wso2.carbon.ml.extension.algorithms.NaiveBayes");
		algorithmClasses.put("DECISION_TREES", "org.wso2.carbon.ml.extension.algorithms.DecisionTree");
		algorithmClasses.put("KMEANS", "org.wso2.carbon.ml.extension.algorithms.KMeans");
	}

	public static AlgorithmFactory getAlgorithmFactory() {
		if(algorithmFactory == null) {
			log.info("Create AlgorithmFactory.");
			return new AlgorithmFactory();
		}

		return algorithmFactory;
	}

    public RAlgorithm getAlgorithmObject(String algorithmName){
		log.debug("Creating algorithm object for " + algorithmName + "from class: " + algorithmClasses.get(algorithmName));
	    try {
		    Class clazz = Class.forName(algorithmClasses.get(algorithmName));

		    Object object = clazz.newInstance();

		    if(object instanceof RAlgorithm) {
			    return (RAlgorithm) object;
		    }else {
			    log.error("Unexpected error creating instance of the algorithm class");
		    }

	    } catch (ClassNotFoundException e) {
		    log.error("Algorithm class not defined.");
	    } catch (InstantiationException e) {
		    log.error("Cannot instantiate algorithm class.");
	    } catch (IllegalAccessException e) {
		    log.error("Illegal access.");
	    }
	    return null;
    }
}
