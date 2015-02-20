package org.wso2.carbon.ml.extension;

import org.apache.log4j.Logger;
import org.wso2.carbon.ml.extension.exception.EvaluationException;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;

public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class);

	public static void main(String[] args) {
		long st = System.currentTimeMillis();
		RExtension rex = null;
		try {
			rex = RExtension.getInstance();
			rex.generateModel("src/test/resources/workflow-1.json");
			rex.destroyREngine();
		} catch (FormattingException e) {
			e.printStackTrace();
		} catch (EvaluationException e) {
			e.printStackTrace();
		} catch (InitializationException e) {
			e.printStackTrace();
		}

		LOGGER.info(System.currentTimeMillis() - st);

	}

}