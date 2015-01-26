package org.wso2.carbon.ml.extension;

import org.apache.log4j.Logger;
import org.rosuda.REngine.REngineException;
import org.wso2.carbon.ml.extension.exception.EvaluationException;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        long st = System.currentTimeMillis();
        RExtension rex = null;
        try {
            rex = new RExtension();
            rex.evaluate("src/test/resources/workflow-3.json");
        } catch (REngineException e) {
            e.printStackTrace();
        } catch (FormattingException e) {
            e.printStackTrace();
        } catch (EvaluationException e) {
            e.printStackTrace();
        } catch (InitializationException e) {
            e.printStackTrace();
        } finally{
            rex.destroy();
        }
        
        long en = System.currentTimeMillis();
        System.out.println(en-st);


    }

}