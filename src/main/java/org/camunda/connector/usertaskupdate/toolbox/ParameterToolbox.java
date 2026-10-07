package org.camunda.connector.usertaskupdate.toolbox;

import io.camunda.connector.cherrytemplate.RunnerParameter;

import java.util.*;

/**
 * UserTaskUpdate is a single-function connector (no sub-function selection, unlike CalendarAdvance),
 * so this toolbox is a straightforward pass-through: it turns the declared RunnerParameter constants
 * into the List&lt;Map&gt; shape expected by CherryInput/CherryOutput.
 */
public class ParameterToolbox {

    /**
     * This is a toolbox, only static method
     */
    private ParameterToolbox() {
    }

    public static List<Map<String, Object>> getInputParameters(List<RunnerParameter> parameters) {
        // the parameterNameForCondition argument is unused by RunnerParameter.toMap() itself (it is
        // only meaningful in CalendarAdvance's multi-sub-function toolbox), so null is passed here.
        return parameters.stream().map(t -> t.toMap(null)).toList();
    }

    public static List<Map<String, Object>> getOutputParameters(List<RunnerParameter> parameters) {
        return parameters.stream().map(t -> t.toMap(null)).toList();
    }

    /**
     * The input maybe a List (from a FEEL expression) or a String with , to separate values
     *
     * @param value the value to decode
     * @return a list of string
     */
    public static List<String> getListOfString(Object value) {
        if (value instanceof List<?> valueList) {
            return valueList.stream().map(Object::toString).toList();
        }
        if (value instanceof String valueString) {
            List<String> listOfStrings = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(valueString, ",");
            while (st.hasMoreTokens()) {
                listOfStrings.add(st.nextToken().trim());
            }
            return listOfStrings;
        }
        return Collections.emptyList();
    }

    /**
     * The input maybe a Long, a Number, a String, or a List of any of these (from a FEEL expression)
     *
     * @param value the value to decode
     * @return a list of Long
     */
    public static List<Long> getListOfLong(Object value) {
        if (value instanceof List<?> valueList) {
            return valueList.stream().map(ParameterToolbox::toLong).filter(java.util.Objects::nonNull).toList();
        }
        Long singleValue = toLong(value);
        return singleValue == null ? Collections.emptyList() : List.of(singleValue);
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
