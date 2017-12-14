package org.bonitasoft.meteor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.bonitasoft.engine.bpm.contract.InputDefinition;
import org.bonitasoft.engine.bpm.contract.Type;

import javassist.bytecode.ByteArray;

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

	public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:MM:ss");

	public static String getHumanDate(Date date) {
		if (date != null)
			return sdf.format(date);
		return null;
	}

	public static String getHumanDelay(long delayMs) {
		String accumulate = "";
		long calculMs = delayMs;
		if (calculMs > 1000 * 60 * 60) {
			accumulate += (calculMs / (1000 * 60 * 60)) + " h ";
			calculMs = calculMs % (1000 * 60 * 60);
		}
		if (calculMs > 1000 * 60) {
			accumulate += (calculMs / (1000 * 60)) + " mn ";
			calculMs = calculMs % (1000 * 60);
		}
		// display the MS only if delay are small
		if (calculMs > 1000) {
			accumulate += (calculMs / 1000) + " s ";
			calculMs = calculMs % 1000;
		}
		if (calculMs > 0 && delayMs < 60 * 1000)
			accumulate += calculMs + " ms ";
		if (accumulate.length() == 0)
			accumulate = "";
		// else
		// accumulate+="("+delayMs+")";
		return accumulate;
	}
}
