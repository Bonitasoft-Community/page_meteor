package org.bonitasoft.meteor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class MeteorToolbox {

	/**
	 * update data from the html page
	 *
	 * @param request
	 */

	/**
	 * @param mapRequestMultipart
	 * @param paramName
	 * @return
	 */
	public static boolean getParameterBoolean(final Map<String, Object> mapRequestMultipart, final String paramName) {
		final Object value = mapRequestMultipart.get(paramName);
		if (value == null) {
			return false;
		}
		if ("on".equalsIgnoreCase(value.toString())) {
			return true;
		}
		return false;
	}

	/**
	 * @param mapRequestMultipart
	 * @param paramName
	 * @param defaultValue
	 * @return
	 */
	public static long getParameterLong(final Map<String, Object> mapRequestMultipart, final String paramName, final long defaultValue) {
		final Object value = mapRequestMultipart.get(paramName);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Long) {
			return (Long) value;
		}
		try {
			final long valueLong = Long.valueOf(value.toString());
			return valueLong;
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	public static Map<String, Object> getParameterHashMap(final Map<String, Object> mapRequestMultipart, final String paramName, final Map<String, Object> defaultValue) {
		final Object value = mapRequestMultipart.get(paramName);
		if (value == null || (!(value instanceof String))) {
			return defaultValue;
		}
		final Map<String, Object> valueHashMap = new HashMap<String, Object>();
		try {
			final StringTokenizer st = new StringTokenizer((String) value, ";");
			while (st.hasMoreTokens()) {
				final String oneValue = st.nextToken();
				final int index = oneValue.indexOf("=");
				if (index != -1) {
					valueHashMap.put(oneValue.substring(0, index), oneValue.substring(index + 1));
				}
			}
			return valueHashMap;
		} catch (final Exception e) {
			return defaultValue;
		}
	}
	public static List<Object> getParameterList(final Map<String, Object> mapRequestMultipart, final String paramName, final List<Object> defaultValue) {
		final Object value = mapRequestMultipart.get(paramName);
		if (value == null || (!(value instanceof List))) {
			return defaultValue;
		}
		return (List) value;
	}

	public static String getParameterString(final Map<String, Object> mapRequestMultipart, final String paramName, final String defaultValue) {
		final Object value = mapRequestMultipart.get(paramName);
		if (value == null) {
			return defaultValue;
		}
		try {
			return value.toString();
		} catch (final Exception e) {
			return defaultValue;
		}
	}
	
	/**
	 * Bonita Input require LONG when the JSON return INTEGER
	 * @param inputMap
	 * @return
	 */
	
	public static void transformJsonContentForBonitaInput(Object inputObject)
	{
		if (inputObject ==null)
			return;
		if (inputObject instanceof Map)
		{
			Map<String,Object> inputObjectMap = (Map<String,Object>) inputObject;
			for (String key : inputObjectMap.keySet())
			{
				if (inputObjectMap.get(key) instanceof Map)
				{
					transformJsonContentForBonitaInput( inputObjectMap.get(key) );
				}
				if (inputObjectMap.get(key) instanceof List)
				{
					transformJsonContentForBonitaInput( inputObjectMap.get(key) );
				}
				if (inputObjectMap.get(key) instanceof Long)
				{
					inputObjectMap.put( key, Integer.valueOf( ((Long)inputObjectMap.get(key)).intValue() ));
				}
			}
		}
		
		if (inputObject instanceof List)
		{
			List<Object> inputObjectList = (List<Object>)  inputObject;

				for (Object item : inputObjectList)
				{
					transformJsonContentForBonitaInput( item );
				}
			
		}
	}
}
