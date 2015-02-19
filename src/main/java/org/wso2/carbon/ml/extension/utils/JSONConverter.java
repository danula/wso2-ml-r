package org.wso2.carbon.ml.extension.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.rosuda.REngine.*;

import java.util.HashMap;
import java.util.Map;

public class JSONConverter {
	public static String convertToJSONString(REXP e) {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		return gson.toJson(convertToJSON(e));
	}

	public static Object convertToJSON(REXP e) {
		Map<String, Object> jo = new HashMap<>();

		if (e instanceof REXPNull) {

		} else if (e.isList()) {
			RList rl;
			if (e instanceof REXPGenericVector)
				rl = ((REXPGenericVector) e).asList();
			else
				rl = ((REXPList) e).asList();
			for (int i = 0; i < rl.size(); i++) {
				jo.put(rl.keyAt(i), convertToJSON(rl.at(i)));

			}

		} else if (e instanceof REXPString) {
			String[] array = ((REXPString) e).asStrings();

			return array;

		} else if (e instanceof REXPSymbol) {
			String[] array = ((REXPSymbol) e).asStrings();
			return array;

		} else if (e instanceof REXPList) {
			return "REXPList";
		} else if (e instanceof REXPInteger) {
			if (e._attr() != null) {
				RList rl1 = e._attr().asList();
				for (String key : rl1.keys()) {
					jo.put(key, convertToJSON(rl1.at(key)));
				}
			}
			int[] array = ((REXPInteger) e).asIntegers();
			jo.put("data", array);

		} else if (e instanceof REXPDouble) {
			if (e._attr() != null) {
				RList rl1 = e._attr().asList();
				for (String key : rl1.keys()) {
					jo.put(key, convertToJSON(rl1.at(key)));
				}
			}
			double[] array = ((REXPDouble) e).asDoubles();
			jo.put("data", array);

		} else
			return "Other";
		return jo;
	}
}
