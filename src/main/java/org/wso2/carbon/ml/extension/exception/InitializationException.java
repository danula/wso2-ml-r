package org.wso2.carbon.ml.extension.exception;

public class InitializationException extends Exception {

	public InitializationException(String message) {
		super(message);
	}

	public InitializationException(String message, Exception e) {
		super(message);
	}
}
