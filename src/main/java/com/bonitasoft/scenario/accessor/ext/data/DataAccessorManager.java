package com.bonitasoft.scenario.accessor.ext.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Level;

import com.bonitasoft.scenario.accessor.Accessor;

public class DataAccessorManager extends HashMap<String, Serializable> {
	private Accessor accessor = null;
	private HashMap<String, DataAccessor> dataAccessors = new HashMap<String, DataAccessor>();

	public DataAccessorManager(Accessor accessor) {
		this.accessor = accessor;
	}

	public void register(String dataName, DataAccessor dataAccessor) {
		dataAccessors.put(dataName, dataAccessor);
	}

	@Override
	public Serializable get(Object key) {
		DataAccessor dataAccessor = dataAccessors.get(key);
		if (dataAccessor == null) {
			accessor.log(Level.INFO, "The data " + key + " does not exist");
		}

		try {
			return dataAccessor.getValue(accessor);
		} catch (Exception e) {
			accessor.log(Level.SEVERE, "The value of the data " + key + " could not be got", e);
		}

		return null;
	}
}
