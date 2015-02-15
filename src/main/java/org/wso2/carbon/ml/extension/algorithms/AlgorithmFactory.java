package org.wso2.carbon.ml.extension.algorithms;

public class AlgorithmFactory {

    public RAlgorithm getAlgorithm(String algorithmName){

        switch(algorithmName) {
            case "NAIVE_BAYES": return new NaiveBayes();
        }
        return null;
    }
}