package org.wso2.carbon.ml.extension;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

public class Main {

	public static void main(String[] args) {
		try {
	        RExtension rex = new RExtension();
	        rex.evaluate("example_workflow.json", true);
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
