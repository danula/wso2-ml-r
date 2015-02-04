package org.wso2.carbon.ml.extension.util;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;
import org.wso2.carbon.ml.extension.bean.MLFeature;
import org.wso2.carbon.ml.extension.bean.MLWorkflow;

import static org.junit.Assert.*;

/**
 * Test cases related to {@link WorkflowParser}
 *
 */
public class WorkflowParserTestCase {

	private static final String RESOURCE_LOCATION = "src/test/resources/workflow-1.json";
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
		assertEquals("DECISION_TREES", workflow.getAlgorithmName());
		assertEquals("src/test/resources/dataset-1.csv", workflow.getDatasetURL());
		assertEquals("Channel", workflow.getResponseVariable());
		assertEquals(Double.toString(0.69999999999999996),
		             Double.toString(workflow.getTrainDataFraction()));
		
		List<MLFeature> features = workflow.getFeatures();
		assertNotNull(features);
		assertEquals(8, features.size());
		
		Map<String, String> hyperParameters = workflow.getHyperParameters();
		assertNotNull(hyperParameters);
        assertEquals(0, hyperParameters. size());

        Map<String, String> trainControls = workflow.getTrainControls();
        assertNotNull(trainControls);
        assertEquals(3, trainControls.size());
        assertEquals("repeatedcv",trainControls.get("method"));

	}
}
