package org.wso2.carbon.ml.extension.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;
import org.wso2.carbon.ml.extension.model.MLFeature;
import org.wso2.carbon.ml.extension.model.MLWorkflow;

/**
 * Test cases related to {@link WorkflowParser}
 *
 */
public class WorkflowParserTestCase {

	private static final String RESOURCE_LOCATION = "src/test/resources/workflow-3.json";
	MLWorkflow workflow;
	WorkflowParser init;
	
	@Before
	public void setup() {
		 init = new WorkflowParser();
		try {
			workflow = init.parseWorkflow(RESOURCE_LOCATION);
		}  catch (FormattingException e) {
			fail("Unexpected Exception - Formatting Exception");
		} catch (InitializationException e) {
			fail("Unexpected Exception - Initializaion Exception");
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

	/*Testing Exceptions*/
	@Test(expected = InitializationException.class)
	public void testParseWorkflowexception1() throws FormattingException, InitializationException {
		init.parseWorkflow("test.json");
	}

	@Test(expected = FormattingException.class)
	public void testParseWorkflowexception2() throws FormattingException, InitializationException {
		init.parseWorkflow("src/test/resources/empty.json");
	}

}
