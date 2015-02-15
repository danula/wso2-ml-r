package org.wso2.carbon.ml.extension.algorithms;

import org.wso2.carbon.ml.extension.bean.MLRWorkflow;

import java.util.ArrayList;
import java.util.List;

public abstract class RAlgorithm {

    private String algrithmName;
    private List<String> rLibraries;

    public RAlgorithm(String algorithmName, List<String> rLibraries){
        this.algrithmName = algorithmName;
        this.rLibraries = rLibraries;
    }

    public ArrayList<String> generateScript(){
        return null;
    }

    /**
     * Generate script to export R model as a PMML file.
     * @param parameters optimized parameters
     * @param mlrWorkflow {@link org.wso2.carbon.ml.extension.bean.MLRWorkflow}
     * @return
     */
    public abstract ArrayList<String> generatePMML(StringBuilder parameters, MLRWorkflow mlrWorkflow);

}
