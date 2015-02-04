package org.wso2.carbon.ml.extension;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wso2.carbon.ml.extension.bean.MLWorkflow;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;
import org.wso2.carbon.ml.extension.util.WorkflowParser;

import static org.junit.Assert.fail;

public class RExtensionTestCase {
	
	private static final Logger LOGGER = Logger.getLogger(RExtensionTestCase.class);
	
	private static final String RESOURCE_LOCATION = "src/test/resources/";
	
	private RExtension rex;
	
	private MLWorkflow mlWorkflow;
	
	@Before
	public void setup(){
		try {
	        this.rex = new RExtension();
        } catch (InitializationException e) {
            fail("Unexpected Exception: Cannot create R engine");
        }

        WorkflowParser parser = new WorkflowParser();
        try {
            this.mlWorkflow = parser.parseWorkflow(RESOURCE_LOCATION + "workflow-1.json");
        } catch (InitializationException e) {
            fail("Unexpected Exception: InitializationException");
        } catch (FormattingException e) {
            fail("Unexpected Exception: FormattingException");
        }

    }

	@Test
	public void testEvaluate1(){

    }
	
	@Test
	public void testEvaluate2(){
//
//		try {
//	        rex.evaluate(mlWorkflow);
//
//	        File file = new File("model.pmml");
//
//	        assertNotNull(file);
//
//        } catch (REngineException e) {
//        	fail("Unexpected Exception - REngineException");
//        } catch (REXPMismatchException e) {
//        	fail("Unexpected Exception - REXPMismatchException");
//        }
	}
	
	
	@Test
	public void testSVM(){
//
//		String svmScript = "model <- svm(UNS ~ STG+SCG+STR+LPR+PEG,data=input,probability=TRUE,type='C',kernel='linear')";
//
//		try {
//	        rex.evaluate("src/test/resources/workflow-2.json");
//	        assertEquals(svmScript,rex.getScript().toString());
//        } catch (IOException | ParseException | REngineException | REXPMismatchException e) {
//	        fail("Unexpected Exception");
//        }
	}
	
	@Test
	public void testLogisticRegression(){
//
//		String logRegScript = "model <- glm(Class ~ Age+BMI+DBP+DPF+NumPregnancies+PG2+SI2+TSFT,data=input,family=binomial)";
//
//		try {
//	        rex.evaluate("src/test/resources/workflow-3.json");
//	        assertEquals(logRegScript,rex.getScript().toString());
//        } catch (IOException | ParseException | REngineException | REXPMismatchException e) {
//	        fail("Unexpected Exception");
//        }
	}
	
	
	@After
	public void cleanup(){
//		File file = new File("model.pmml");
//
//		if(file != null)
//			file.delete();
//
//		file = new File("src/test/resources/test3.pmml");
//
//		if(file != null)
//			file.delete();
//
//		file = new File("src/test/resources/test4.pmml");
//
//		if(file != null)
//			file.delete();
	}
	

}