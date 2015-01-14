package org.wso2.carbon.ml.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.wso2.carbon.ml.extension.model.MLWorkflow;
import org.wso2.carbon.ml.extension.util.InitializeWorkflow;

public class RExtensionTestCase {
	
	private static final String RESOURCE_LOCATION = "src/test/resources/";
	
	private RExtension rex;
	
	private MLWorkflow mlWorkflow;
	
	@Before
	public void setup(){
		try {
	        this.rex = new RExtension();
        } catch (REngineException e) {
	        fail("Unexpected Exception - REngineException");
        }
		
		StringBuffer workflowLocation = new StringBuffer(RESOURCE_LOCATION);
		
		InitializeWorkflow init = new InitializeWorkflow();
		try {
	        this.mlWorkflow = init.parseWorkflow(workflowLocation.append("workflow-1.json").toString());
        } catch (FileNotFoundException e) {
        	fail("Unexpected Exception - FileNotFoundException");
        } catch (IOException e) {
        	fail("Unexpected Exception - IOException");
        } catch (ParseException e) {
        	fail("Unexpected Exception - ParseException");
        }
	}

	@Test
	public void testEvaluate1(){
		
		StringBuffer workflowLocation = new StringBuffer(RESOURCE_LOCATION);
		
		try {
			
	        rex.evaluate(workflowLocation.append("workflow-1.json").toString());
	        
	        File file = new File("model.pmml");
	        
	        assertNotNull(file);
	        
        } catch (FileNotFoundException e) {
        	fail("Unexpected Exception - FileNotFoundException");
        } catch (IOException e) {
        	fail("Unexpected Exception - IOException");
        } catch (ParseException e) {
        	fail("Unexpected Exception - ParseException");
        } catch (REngineException e) {
        	fail("Unexpected Exception - REngineException");
        } catch (REXPMismatchException e) {
        	fail("Unexpected Exception - REXPMismatchException");
        } 
	}
	
	@Test
	public void testEvaluate2(){
		
		try {			
	        rex.evaluate(mlWorkflow);
	        
	        File file = new File("model.pmml");
	        
	        assertNotNull(file);
	        
        } catch (REngineException e) {
        	fail("Unexpected Exception - REngineException");
        } catch (REXPMismatchException e) {
        	fail("Unexpected Exception - REXPMismatchException");
        }
	}
	
	@Test
	public void testEvaluate3(){
		
		StringBuffer workflowLocation = new StringBuffer(RESOURCE_LOCATION);
		File file;
		
		try {
			
	        rex.evaluate(workflowLocation.append("workflow-1.json").toString(), "src/test/resources/test3.pmml");
	        file = new File("src/test/resources/test3.pmml");
	        
	        assertNotNull(file);
	        
        } catch (FileNotFoundException e) {
        	fail("Unexpected Exception - FileNotFoundException");
        } catch (IOException e) {
        	fail("Unexpected Exception - IOException");
        } catch (ParseException e) {
        	fail("Unexpected Exception - ParseException");
        } catch (REngineException e) {
        	fail("Unexpected Exception - REngineException");
        } catch (REXPMismatchException e) {
        	fail("Unexpected Exception - REXPMismatchException");
        } 
	}
	
	@Test
	public void testEvaluate4(){
		
		try {			
	        rex.evaluate(mlWorkflow, "src/test/resources/test4.pmml");
	        
	        File file = new File("src/test/resources/test4.pmml");
	        
	        assertNotNull(file);
	        
        } catch (REngineException e) {
        	fail("Unexpected Exception - REngineException");
        } catch (REXPMismatchException e) {
        	fail("Unexpected Exception - REXPMismatchException");
        }
	}
	
	@Test
	public void testSVM(){
		
		String svmScript = "model <- svm(Channel ~ Region+Fresh+Milk+Grocery+Frozen+Detergents_Paper+Delicassen,data=input,probability=TRUE,type='C',kernel='linear')";

		try {
	        rex.evaluate("src/test/resources/workflow-1.json");
	        assertEquals(svmScript,rex.getScript().toString());
        } catch (IOException | ParseException | REngineException | REXPMismatchException e) {
	        fail("Unexpected Exception");
        }
	}
	
	@Test
	public void testLogisticRegression(){
		
		String logRegScript = "model <- glm(Class ~ Age+BMI+DBP+DPF+NumPregnancies+PG2+SI2+TSFT,data=input,family=binomial)";

		try {
	        rex.evaluate("src/test/resources/workflow-3.json");
	        assertEquals(logRegScript,rex.getScript().toString());
        } catch (IOException | ParseException | REngineException | REXPMismatchException e) {
	        fail("Unexpected Exception");
        }
	}
	
	
	@After
	public void cleanup(){
		File file = new File("model.pmml");
		
		if(file != null)
			file.delete();
		
		file = new File("src/test/resources/test3.pmml");
		
		if(file != null)
			file.delete();
		
		file = new File("src/test/resources/test4.pmml");
		
		if(file != null)
			file.delete();
	}
	

}