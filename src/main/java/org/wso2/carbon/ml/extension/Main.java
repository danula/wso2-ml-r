package org.wso2.carbon.ml.extension;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

public class Main {
	
	private static final Logger LOGGER = Logger.getLogger(Main.class);

	public static void main(String[] args) {
		try {
			long st = System.currentTimeMillis();
	        RExtension rex = new RExtension();
	        rex.evaluate("src/test/resources/workflow-3.json", "/home/madawa/test.pmml");
	        long end = System.currentTimeMillis();
	        LOGGER.info(end-st);
	        
        } catch (REngineException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (FileNotFoundException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (ParseException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (REXPMismatchException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	}

}
