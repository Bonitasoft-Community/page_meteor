package org.bonitasoft.meteor;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.bpm.contract.ContractDefinition;
import org.bonitasoft.engine.bpm.contract.InputDefinition;
import org.bonitasoft.engine.bpm.contract.Type;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.json.simple.JSONValue;

public class MeteorTransformInputs {

	public static BEvent EventUnknowTypeContract = new BEvent(MeteorTransformInputs.class.getName(), 1, Level.ERROR, "Unkown Type", "The type declare in the contract is unknown", "Input transformation is not possible, JSON object is not transformed", "Report the error to the community");
	public static BEvent EventExceptionTransformation = new BEvent(MeteorTransformInputs.class.getName(), 2, Level.APPLICATIONERROR, "Exception during transformation", "A transformation can not be realized", "Input transformation is not possible, JSON object is not transformed",
			"Check the JSON as input");

	public static Logger logger = Logger.getLogger(MeteorRobot.class.getName());

	List<BEvent> listEventsTransformation = new ArrayList<BEvent>();

	public List<BEvent> getEventsTransformation() {
		return listEventsTransformation;
	}

	/* ******************************************************************** */
	/*                                                                      */
	/* Transform the Json to match contract									*/
	/*                                                                      */
	/*                                                                      */
	/* ******************************************************************** */

	/**
	 * transform the input according the contract Definition
	 * 
	 * @param inputsObject
	 * @param contractDefinition
	 */
	public void transformJsonContentForBonitaInput(Map<String, Serializable> inputsObject, ContractDefinition contractDefinition) {
		if (inputsObject == null || !(inputsObject instanceof Map))
			return;
		if (contractDefinition==null)
			return;
		
		// the contract pilot the input
		List<InputDefinition> listInput = contractDefinition.getInputs();
		for (InputDefinition input : listInput) {
			Object oneObject = inputsObject.get(input.getName());
			inputsObject.put(input.getName(), (Serializable) transformByInput(oneObject, input, false));
		}
	}

	// DateOnlyInput (DATE ONLY) : 01/02/2018  			format "2018-02-01T00:00:00.000Z" ==> sdfLocalDate
	// Date-Time (NO TIME ZONE) 25/12/2017 09:25:55  	format "2017-12-25T09:25:55",	==> sdfLocalDateTime
	// Date-Time (TIME ZONE)  10/12/2017 09:30:15 PDT 	format "2017-12-10T18:30:15Z" ==> sdfOffsetDateTime
	// Date (deprecated) 01/04/2017 					format "2017-04-01T00:00:00.000Z",==> sdfDate
	public static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	// format "2018-02-01T00:00:00.000Z"
	// public static final SimpleDateFormat sdfLocalDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	public static DateTimeFormatter dtfLocalDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	// format : 2017-12-05T19:25:00Z
	//public static final SimpleDateFormat sdfLocalDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public static DateTimeFormatter dtfLocalDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	// DateTimeNoTomeZone format 2017-12-05T11:25:00",
	//public static final SimpleDateFormat sdfOffsetDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	public static DateTimeFormatter dtfOffsetDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

	 
	    
	    
	public Object transformByInput(Object sourceObject, InputDefinition input, boolean runTheList) {
		// this is correct : the object is here, but null.
		if (sourceObject == null)
			return sourceObject;
		// input maybe a LIST OF : in that circonstance, look on every item
		if ((!runTheList) && input.isMultiple()) {
			// so the object is suppose to be a list
			if (!(sourceObject instanceof List))
				return sourceObject; // we can't do anything, the contract is
										// wrong
			List<Object> transformList = new ArrayList<Object>();
			for (Object inputItem : (List) sourceObject) {
				transformList.add(transformByInput(inputItem, input, true));
			}
			return transformList;
		}
		// now, check the input
		if (input.hasChildren()) {
			// Map
			if (!(sourceObject instanceof Map))
				return sourceObject; // we can't do anything, the contract is
										// wrong
			// run all item of the map
			Map<String, Object> transformMap = new HashMap<String, Object>();

			List<InputDefinition> listSubInput = input.getInputs();
			for (InputDefinition subInput : listSubInput) {
				Object subSourceObject = ((Map) sourceObject).get(subInput.getName());
				transformMap.put(subInput.getName(), transformByInput(subSourceObject, subInput, false));
			}
			return transformMap;
		}

		Type typeInput = input.getType();

		Object transformObject = null;
		try {
			if (Type.BOOLEAN.equals(typeInput)) {
				transformObject = Boolean.valueOf(sourceObject.toString());
			} else if (Type.BYTE_ARRAY.equals(typeInput)) {
				transformObject = sourceObject;
			} else if (Type.DATE.equals(typeInput)) {
				transformObject = sdfDate.parseObject( sourceObject.toString() );
			} else if (Type.DECIMAL.equals(typeInput)) {
				transformObject = Double.valueOf(sourceObject.toString());
			} else if (Type.FILE.equals(typeInput)) {
				transformObject = sourceObject;
			} else if (Type.INTEGER.equals(typeInput)) {
				transformObject = Integer.valueOf(sourceObject.toString());
			} else if ("LONG".equals(typeInput.toString())) {
				//
				transformObject = Long.valueOf(sourceObject.toString());
				// start in 7.5
			} else if ("LOCALDATE".equals(typeInput.toString())) {
				// Date format following the ISO-8601 norm.
				transformObject = LocalDate.parse( sourceObject.toString(), dtfLocalDate);

				// start in 7.5
			} else if ("LOCALDATETIME".equals(typeInput.toString())) {
				// Date format following the ISO-8601 norm.
				transformObject = LocalDateTime.parse( sourceObject.toString(), dtfLocalDateTime );
				// start in 7.5
			} else if ("OFFSETDATETIME".equals(typeInput.toString())) {
				// Date format following the ISO-8601 norm.
				LocalDateTime l = LocalDateTime.parse( sourceObject.toString(), dtfOffsetDateTime );
				transformObject = OffsetDateTime.of( l, ZoneOffset.UTC );
			} else if (Type.TEXT.equals(typeInput)) {
				transformObject = sourceObject;
			} else {
				// unknow
				transformObject = sourceObject;
				listEventsTransformation.add(new BEvent(EventUnknowTypeContract, "InputName[" + input.getName() + "] type[" + typeInput.toString() + "]"));
			}
		} catch (Exception e) {
			listEventsTransformation.add(new BEvent(EventExceptionTransformation, "InputName[" + input.getName() + "] type[" + typeInput.toString() + "] JsonObject[" + sourceObject.toString() + "]"));
			logger.severe("MeteorTransformationInput error InputName[" + input.getName() + "] type[" + typeInput.toString() + "] JsonObject[" + sourceObject.toString() + "]");
			// can't transform
			transformObject = sourceObject;
		}
		return transformObject;
	}
	
	/* ******************************************************************** */
	/*                                                                      */
	/* propose a JSON by the contract										*/
	/*                                                                      */
	/*                                                                      */
	/* ******************************************************************** */

	/*
	 * return a JSON proposition from the contract
	 */
	public String getProposeJson(ContractDefinition contractDefinition)
	{
		
		Map<String, Object> proposedJson = new HashMap<String,Object>();
		if (contractDefinition!=null)
		{
			List<InputDefinition> listInput = contractDefinition.getInputs();
			for (InputDefinition input : listInput) {
				Object oneObject = getProposeJsonOneInput(input, false );
				proposedJson.put(input.getName(), oneObject);
			}
		}
		return JSONValue.toJSONString(proposedJson);
	}
	
	private Object getProposeJsonOneInput( InputDefinition input, boolean runTheList)
	{
		if ((!runTheList) && input.isMultiple()) {
			
			List<Object> list = new ArrayList<Object>();
			list.add( getProposeJsonOneInput(input,true));
			return list;
		}
		// now, check the input
		Type typeInput = input.getType();
		if (Type.FILE.equals(typeInput)) {
			return null;
		}

		
		if (input.hasChildren()) {
			// Map
			Map<String,Object> map =new HashMap<String,Object>();
			List<InputDefinition> listSubInput = input.getInputs();
			for (InputDefinition subInput : listSubInput) {
				map.put(subInput.getName(), getProposeJsonOneInput(subInput, false));
			}
			return map;
		}


		
		try {
			if (Type.BOOLEAN.equals(typeInput)) {
				return Boolean.TRUE;
			} else if (Type.BYTE_ARRAY.equals(typeInput)) {
				return null;
			} else if (Type.DATE.equals(typeInput)) {
				return sdfDate.format(new Date());
			} else if (Type.DECIMAL.equals(typeInput)) {
				return Double.valueOf(3.141516);
			} else if (Type.INTEGER.equals(typeInput)) {
				return Integer.valueOf(724);
			} else if ("LONG".equals(typeInput.toString())) {
				return Long.valueOf(10190708);
			} else if ("LOCALDATE".equals(typeInput.toString())) {
				// Date format following the ISO-8601 norm.
				return LocalDateTime.now().format( dtfLocalDate );
				// start in 7.5
			} else if ("LOCALDATETIME".equals(typeInput.toString())) {
				// Date format following the ISO-8601 norm.
				return LocalDateTime.now().format( dtfLocalDateTime);
				// start in 7.5
			} else if ("OFFSETDATETIME".equals(typeInput.toString())) {
				// Date format following the ISO-8601 norm.
				return LocalDateTime.now().format( dtfOffsetDateTime );
			} else if (Type.TEXT.equals(typeInput)) {
				return "this is a text";
			} else {
				// unknow
				listEventsTransformation.add(new BEvent(EventUnknowTypeContract, "InputName[" + input.getName() + "] type[" + typeInput.toString() + "]"));
				return null;
			}
		} catch (Exception e) {
			listEventsTransformation.add(new BEvent(EventExceptionTransformation, "InputName[" + input.getName() + "] type[" + typeInput.toString() + "]"));
			logger.severe("MeteorTransformationInput error InputName[" + input.getName() + "] type[" + typeInput.toString() + "]");
			// can't transform
			return null;
		}
		
	}
}
