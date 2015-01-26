package org.wso2.carbon.ml.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.rosuda.REngine.*;

/**
 * Created by danula on 1/26/15.
 */
public class JSONConverter {
    public String convertToJSON(REXP e) {
        //System.out.println(e.toDebugString());


        if(e instanceof REXPNull){

        }
        else if(e.isList()) {
            RList rl ;
            if(e instanceof REXPGenericVector) rl = ((REXPGenericVector)e).asList();
            else rl = ((REXPList)e).asList();
            for(int i=0;i<rl.size();i++){
                //jo.put(rl.keyAt(i), convertToJSON(rl.at(i)));
                rl.elementAt(i).getClass();

            }

        }
        else if(e instanceof REXPString){
            //System.out.println("REXPString");
            String[] array = ((REXPString)e).asStrings();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            return gson.toJson(array);

        }
        else if(e instanceof REXPSymbol){
            //System.out.println("REXPString");
            String[] arr = ((REXPSymbol)e).asStrings();
            for(String s:arr){
                System.out.print(s + " ");
            }

        }
        else if(e instanceof REXPList){
            System.out.println("REXPList");
        }
        else if(e instanceof REXPInteger){
            //System.out.println("REXPInt");
            RList rl1=null;
            REXP type =null;

            if(e._attr()!=null) {
                rl1 = e._attr().asList();
                type = rl1.at("names");
            }
            String[] names = null;
            if(type!=null) {
                names = ((REXPString) rl1.at("names")).asStrings();
                int[] arr = ((REXPInteger)e).asIntegers();
                for(int i=0;i<arr.length;i++){
                    if(rl1.at("class")==null)System.out.print(names[i]);
                    System.out.println(arr[i]);
                }

            }else if(rl1.at("dim")!=null) {
                int rows = ((REXPInteger) rl1.at("dim")).asIntegers()[0];
                int cols = ((REXPInteger) rl1.at("dim")).asIntegers()[1];
                String[] axislabels = null;
                String[] rowNames = null;
                String[] colNames = null;

                if(rl1.at("dimnames")!=null) {
                    if(rl1.at("dimnames")._attr()!=null) {
                        axislabels = ((REXPString) rl1.at("dimnames")._attr().asList().at("names")).asStrings();
                    }


                    if (!((((REXPGenericVector) rl1.at("dimnames")).asList().at(0)) instanceof REXPNull)) {
                        rowNames = ((REXPString) ((REXPGenericVector) rl1.at("dimnames")).asList().at(0)).asStrings();
                        colNames = ((REXPString) ((REXPGenericVector) rl1.at("dimnames")).asList().at(1)).asStrings();
                    }
                }
                int[] arr = ((REXPInteger)e).asIntegers();
                if(axislabels!=null)System.out.println("\t\t"+axislabels[0]);
                for(int i=-1;i<rows;i++){
                    for(int j=-1;j<cols;j++){
                        if(i==-1&&j==-1){
                            if(axislabels!=null)System.out.print(axislabels[1]+"\t");
                        }
                        else if(i==-1) {
                            if(colNames!=null) System.out.print(colNames[j]+"\t");
                        }
                        else if(j==-1) {
                            if(rowNames!=null) System.out.print("\t"+rowNames[i]+"\t");
                        }
                        else System.out.print(arr[i*cols+j]+"\t");
                    }
                    System.out.println();
                }
            }

            System.out.println();

        }
        else if(e instanceof REXPDouble){
            RList rl1=null;
            REXP type =null;

            if(e._attr()!=null) {
                rl1 = e._attr().asList();
                type = rl1.at("names");
            }
            String[] names = null;
            if(type!=null) {
                names = ((REXPString) rl1.at("names")).asStrings();
                double[] arr = ((REXPDouble)e).asDoubles();
                for(int i=0;i<arr.length;i++){
                    System.out.print(names[i]);
                    System.out.println(arr[i]);
                }

            }else if(e._attr()!=null){
                int rows = ((REXPInteger) rl1.at("dim")).asIntegers()[0];
                int cols = ((REXPInteger) rl1.at("dim")).asIntegers()[1];

                String[] axislabels = null;
                String[] rowNames = null;
                String[] colNames = null;

                if(rl1.at("dimnames")!=null) {
                    if (rl1.at("dimnames")._attr() != null) {
                        axislabels = ((REXPString) rl1.at("dimnames")._attr().asList().at("names")).asStrings();
                    }

                    if (!((((REXPGenericVector) rl1.at("dimnames")).asList().at(0)) instanceof REXPNull)) {
                        rowNames = ((REXPString) ((REXPGenericVector) rl1.at("dimnames")).asList().at(0)).asStrings();
                        colNames = ((REXPString) ((REXPGenericVector) rl1.at("dimnames")).asList().at(1)).asStrings();
                    }
                }
                double[] arr = ((REXPDouble)e).asDoubles();
                if(axislabels!=null)System.out.println("\t\t"+axislabels[0]);
                for(int i=-1;i<rows;i++){
                    for(int j=-1;j<cols;j++){
                        if(i==-1&&j==-1){
                            if(axislabels!=null)System.out.print(axislabels[1]+"\t");
                        }
                        else if(i==-1) {
                            if(colNames!=null) System.out.print(colNames[j]+"\t");
                        }
                        else if(j==-1) {
                            if(rowNames!=null) System.out.print("\t"+rowNames[i]+"\t");
                        }
                        else System.out.print(arr[i*cols+j]+"\t");
                    }
                    System.out.println();
                }
            }
        }
        else if(e instanceof REXPSymbol){

        }
        else System.out.println(e.toDebugString());
        return new String();
    }
    private String[] getNames(REXP rexp){
        RList rl = rexp._attr().asList();
        return ((REXPString) rl.at("names")).asStrings();
    }
}
