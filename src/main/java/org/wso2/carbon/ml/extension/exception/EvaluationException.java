package org.wso2.carbon.ml.extension.exception;

public class EvaluationException extends Exception {

    public EvaluationException(String message) {
        super(message);
    }

    public EvaluationException(String message, Exception e) {
        super(message, e);
    }
}
