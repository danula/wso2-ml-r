package org.wso2.carbon.ml.extension.exception;

public class FormattingException extends Exception {

    public FormattingException(String message) {
        super(message);
    }

    public FormattingException(String message, Exception e) {
        super(message, e);
    }
}
