package com.bonitasoft.custompage.meteor;

import java.util.HashMap;
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
    public static boolean getParameterBoolean(final Map<String, String> mapRequestMultipart, final String paramName) {
        final String value = mapRequestMultipart.get(paramName);
        if (value == null) {
            return false;
        }
        if ("on".equalsIgnoreCase(value)) {
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
    public static long getParameterLong(final Map<String, String> mapRequestMultipart, final String paramName, final long defaultValue) {
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

    public static Map<String, Object> getParameterHashMap(final Map<String, String> mapRequestMultipart, final String paramName,
            final Map<String, Object> defaultValue) {
        final String value = mapRequestMultipart.get(paramName);
        if (value == null) {
            return defaultValue;
        }
        final Map<String, Object> valueHashMap = new HashMap<String, Object>();
        try {
            final StringTokenizer st = new StringTokenizer(value, ";");
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

    public static String getParameterString(final Map<String, String> mapRequestMultipart, final String paramName, final String defaultValue) {
        final String value = mapRequestMultipart.get(paramName);
        if (value == null) {
            return defaultValue;
        }
        try {
            return value;
        } catch (final Exception e) {
            return defaultValue;
        }
    }

}
