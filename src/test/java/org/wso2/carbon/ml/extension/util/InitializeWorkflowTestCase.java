package org.wso2.carbon.ml.extension.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.wso2.carbon.ml.extension.model.MLFeature;
import org.wso2.carbon.ml.extension.model.MLWorkflow;

/**
 * Test cases related to {@link InitializeWorkflow}
 *
 */
public class InitializeWorkflowTestCase {

	private static final String RESOURCE_LOCATION = "src/test/resources/workflow-3.json";
	MLWorkflow workflow;
	
	@Before
	public void setup() {
		InitializeWorkflow init = new InitializeWorkflow();
		try {
			workflow = init.parseWorkflow(RESOURCE_LOCATION);
		} catch (FileNotFoundException e) {
			fail("Unexpected Exception - FileNotFoundException");
		} catch (IOException e) {
			fail("Unexpected Exception - IOException");
		} catch (ParseException e) {
			fail("Unexpected Exception - ParseException");
		}
	}
	
	@Test
	public void testParseWorkflow1() {
		assertNotNull(workflow);
	}

	@Test
	public void testParseWorkflow2() {

		assertEquals("b9e108cb-0335-43b9-83ca-3cf1b207543e", workflow.getWorkflowID());
		assertEquals("Classification", workflow.getAlgorithmClass());
		assertEquals("LOGISTIC_REGRESSION", workflow.getAlgorithmName());
		assertEquals("src/test/resources/dataset-3.csv", workflow.getDatasetURL());
		assertEquals("Class", workflow.getResponseVariable());
		assertEquals(Double.toString(0.69999999999999996),
		             Double.toString(workflow.getTrainDataFraction()));
		
		List<MLFeature> features = workflow.getFeatures();
		assertNotNull(features);
		assertEquals(9, features.size());
		
		Map<String, String> hyperParameters = workflow.getHyperParameters();
		assertNotNull(hyperParameters);
		assertEquals(1, hyperParameters.size());
		assertEquals("binomial", hyperParameters.get("family"));

	}
	
	

}
