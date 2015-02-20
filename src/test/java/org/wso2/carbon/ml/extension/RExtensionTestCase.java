package org.wso2.carbon.ml.extension;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wso2.carbon.ml.extension.bean.MLRWorkflow;
import org.wso2.carbon.ml.extension.exception.EvaluationException;
import org.wso2.carbon.ml.extension.exception.FormattingException;
import org.wso2.carbon.ml.extension.exception.InitializationException;
import org.wso2.carbon.ml.extension.utils.CommonConstants;
import org.wso2.carbon.ml.extension.utils.WorkflowParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class RExtensionTestCase {
	
	private static final Logger LOGGER = Logger.getLogger(RExtensionTestCase.class);
	
	private static final String RESOURCE_LOCATION = "src/test/resources/";
	
	private RExtension rex;
	
	private MLRWorkflow mlRWorkflow;
	
	@Before
	public void setup(){
		try {
            WorkflowParser parser = new WorkflowParser();
	        this.rex = RExtension.getInstance();
            this.mlRWorkflow = parser.parseWorkflow(RESOURCE_LOCATION + "workflow-1.json");
        } catch (InitializationException e) {
            fail("Unexpected Exception: Cannot create R engine");
        }  catch (FormattingException e) {
            fail("Unexpected Exception: FormattingException");
        }

    }

	@Test
	public void testEvaluate1(){
        try {
            rex.generateModel(RESOURCE_LOCATION + "workflow-1.json",
                              "src/test/resources/Temp/model1-1.pmml");
            FileInputStream fr = new FileInputStream("src/test/resources/Temp/model1-1.pmml");
            //pmml should be exported in the given location
            assertNotNull(fr);

            rex.generateModel(mlRWorkflow, "src/test/resources/Temp/model1-2.pmml");
            fr = new FileInputStream("src/test/resources/Temp/model1-2.pmml");
            //pmml should be exported in the given location
            assertNotNull(fr);
        } catch (FormattingException e) {
            fail("Unexpected Exception: FormattingException");
        } catch (InitializationException e) {
            fail("Unexpected Exception: InitializationException");
        } catch (EvaluationException e) {
            fail("Unexpected Exception: EvaluationException");
        } catch (FileNotFoundException e) {
            fail("Unexpected Exception: FileNotFoundException");
        }
    }
	
	@Test
	public void testEvaluate2(){
        try {
            rex.generateModel(RESOURCE_LOCATION + "workflow-3.json");
            FileInputStream fr = new FileInputStream(CommonConstants.DEFAULT_EXPORT_PATH.toString());
            //pmml should be exported in the default location
            assertNotNull(fr);

            rex.generateModel(mlRWorkflow);
            fr = new FileInputStream(CommonConstants.DEFAULT_EXPORT_PATH.toString());
            //pmml should be exported in the default location
            assertNotNull(fr);
        } catch (FormattingException e) {
            fail("Unexpected Exception: FormattingException");
        } catch (InitializationException e) {
            fail("Unexpected Exception: InitializationException");
        } catch (EvaluationException e) {
            fail("Unexpected Exception: EvaluationException");
        } catch (FileNotFoundException e) {
            fail("Unexpected Exception: FileNotFoundException");
        }

    }

	@After
	public void cleanup(){

        boolean b;
		File file = new File("src/test/resources/Temp/model1-1.pmml");
        b = file.delete();

		file = new File("src/test/resources/Temp/model1-2.pmml");
        b = file.delete();

		file = new File(CommonConstants.DEFAULT_EXPORT_PATH.toString());
        b = file.delete();

	}
	

}