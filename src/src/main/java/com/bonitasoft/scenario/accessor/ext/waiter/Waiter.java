package com.bonitasoft.scenario.accessor.ext.waiter;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.Constants;

public abstract class Waiter {
	private Long limit = 2500L;
	private Long period = 500L;
	protected Accessor accessor = null;
	protected Map<String, Serializable> parameters = new HashMap<String, Serializable>();
	
	public Waiter(Accessor accessor, Map<String, Serializable> parameters) {
		if(parameters != null) {
			this.parameters = parameters;
		}
		if(this.parameters.containsKey(Constants.LIMIT)) {
			this.limit = Long.parseLong(this.parameters.get(Constants.LIMIT).toString());
		}
		if(this.parameters.containsKey(Constants.PERIOD)) {
			this.period = Long.parseLong(this.parameters.get(Constants.PERIOD).toString());
		}
		
		this.accessor = accessor;
	}
	
	public Serializable execute() throws Exception {
		Serializable result = null;
		Long endDate = new Date(new Date().getTime() + limit).getTime();
		while(new Date().getTime() < endDate) {
			result = check();
			if(result != null) {
				if(result instanceof Boolean) {
					if((Boolean)result) {
						accessor.log(Level.FINE, "The waiter ends positively: " + result);
						return result;
					}
				} else if(result instanceof Number) {
					if(((Number)result).doubleValue() > 0) {
						accessor.log(Level.FINE, "The waiter ends positively: " + result);
						return result;
					}
				} else if(result instanceof String) {
					if(!((String)result).isEmpty()) {
						accessor.log(Level.FINE, "The waiter ends positively: " + result);
						return result;
					}
				} else {
					accessor.log(Level.FINE, "The waiter ends positively: " + result);
					return result;
				}
			}
			
			accessor.log(Level.FINE, "The waiter waits " + period + " ms");
			Thread.sleep(period);
		}

		accessor.log(Level.FINE, "The waiter ends negatively");
		return null;
	}
	
	abstract protected Serializable check() throws Exception;
}
