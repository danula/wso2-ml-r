package org.wso2.carbon.ml.extension.algorithms;

import org.apache.log4j.Logger;
import org.wso2.carbon.ml.extension.exception.InitializationException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AlgorithmFactory {

	private static final Logger log = Logger.getLogger(AlgorithmFactory.class);

	private static AlgorithmFactory algorithmFactory;
	private Map<String, String> algorithmClasses = new HashMap<>();



	private AlgorithmFactory() throws InitializationException {
		boolean b = readProperties();

		if(!b) {
			throw new InitializationException("Unexpected error reading algorithm properties file. ");
		}
	}

	public static AlgorithmFactory getAlgorithmFactory() {
		if(algorithmFactory == null) {
			log.info("Create AlgorithmFactory.");
			try {
				algorithmFactory = new AlgorithmFactory();
			} catch (InitializationException e) {
				log.error("Unexpected error reading algorithm properties file. ");
				algorithmFactory = null;
			}
		}

		return algorithmFactory;
	}

    public RAlgorithm getAlgorithmObject(String algorithmName){
		log.debug("Creating algorithm object for " + algorithmName + "from class: " + algorithmClasses.get(algorithmName));
	    try {
		    Class clazz = Class.forName(algorithmClasses.get(algorithmName));

		    Object object = clazz.newInstance();

		    if(object instanceof RAlgorithm) {
			    return (RAlgorithm) object;
		    }else {
			    log.error("Unexpected error creating instance of the algorithm class");
		    }

	    } catch (ClassNotFoundException e) {
		    log.error("Algorithm class not defined.");
	    } catch (InstantiationException e) {
		    log.error("Cannot instantiate algorithm class.");
	    } catch (IllegalAccessException e) {
		    log.error("Illegal access.");
	    }
	    return null;
    }

	private boolean readProperties() {
		Properties algorithmProperties = new Properties();
		InputStream inStream;

		try{
			inStream = new FileInputStream("algorithms.properties");

			algorithmProperties.load(inStream);

			Enumeration<?> e = algorithmProperties.propertyNames();

			while(e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = algorithmProperties.getProperty(key);
				algorithmClasses.put(key, value);
			}
		} catch (IOException e) {
			return false;
		}

		return true;
	}
}
