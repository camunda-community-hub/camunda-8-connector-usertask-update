package org.camunda.connector.usertaskupdate.operation;

import io.camunda.client.api.command.UpdateUserTaskCommandStep1;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.camunda.connector.usertaskupdate.UserTaskUpdateInput;
import org.camunda.connector.usertaskupdate.UserTaskUpdateOutput;
import org.camunda.connector.usertaskupdate.toolbox.UserTaskUpdateError;

import java.util.List;
import java.util.Map;

/**
 * Priority operation group, driven by a single {@code priorityOperation} selector:
 * <ul>
 *     <li>INCREASE / DECREASE - move the current priority up/down by {@code priorityStep}</li>
 *     <li>ABSOLUTE - set the priority to {@code priorityAbsoluteValue} (0-100)</li>
 *     <li>SETLEVEL - set the priority to the value Tasklist maps to {@code priorityLevel}'s band
 *     (see https://docs.camunda.io/docs/components/tasklist/userguide/defining-task-priorities/ :
 *     0-25 Low, 26-50 Medium, 51-75 High, 76-100 Critical - the upper bound of each band is used)</li>
 *     <li>CLEAR - reset the priority to its BPMN default, 50 (Zeebe has no "clear priority" command;
 *     per the Camunda docs, a user task with no priority set defaults to 50)</li>
 * </ul>
 * Backed by {@link UpdateUserTaskCommandStep1#priority(Integer)}.
 */
public class PriorityOperation {

    public static final int DEFAULT_PRIORITY = 50;

    public static final String OPERATION_NOTHING = "NOTHING";
    public static final String OPERATION_INCREASE = "INCREASE";
    public static final String OPERATION_DECREASE = "DECREASE";
    public static final String OPERATION_ABSOLUTE = "ABSOLUTE";
    public static final String OPERATION_SETLEVEL = "SETLEVEL";
    public static final String OPERATION_CLEAR = "CLEAR";

    public static final String LEVEL_LOW = "LOW";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_CRITICAL = "CRITICAL";

    // Upper bound of each Tasklist priority band (0-25 Low, 26-50 Medium, 51-75 High, 76-100 Critical)
    public static final int LEVEL_LOW_VALUE = 25;
    public static final int LEVEL_MEDIUM_VALUE = 50;
    public static final int LEVEL_HIGH_VALUE = 75;
    public static final int LEVEL_CRITICAL_VALUE = 100;

    public static final String PRIORITY_OPERATION = "priorityOperation";
    public static final RunnerParameter parameterPriorityOperation = new RunnerParameter(
            PRIORITY_OPERATION, // name
            "Priority: operation", // label
            String.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "NOTHING (default) leaves the priority untouched. INCREASE/DECREASE the current priority by priorityStep, set it to an ABSOLUTE value, set it from priorityLevel (SETLEVEL), or CLEAR it back to the default (50).")
            .addChoice(OPERATION_NOTHING, "Nothing")
            .addChoice(OPERATION_INCREASE, "Increase")
            .addChoice(OPERATION_DECREASE, "Decrease")
            .addChoice(OPERATION_ABSOLUTE, "Absolute")
            .addChoice(OPERATION_SETLEVEL, "Set level")
            .addChoice(OPERATION_CLEAR, "Clear")
            .setDefaultValue(OPERATION_NOTHING)
            .setGroup("Priority")
            .setVisibleInTemplate();

    public static final String PRIORITY_STEP = "priorityStep";
    public static final RunnerParameter parameterPriorityStep = new RunnerParameter(
            PRIORITY_STEP, // name
            "Priority: step", // label
            Integer.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Step used with priorityOperation=INCREASE/DECREASE to move the current priority. The result is clamped to [0, 100].")
            .setDefaultValue("10")
            .addCondition(PRIORITY_OPERATION, List.of(OPERATION_INCREASE, OPERATION_DECREASE))
            .setGroup("Priority")
            .setVisibleInTemplate();

    public static final String PRIORITY_ABSOLUTE_VALUE = "priorityAbsoluteValue";
    public static final RunnerParameter parameterPriorityAbsoluteValue = new RunnerParameter(
            PRIORITY_ABSOLUTE_VALUE, // name
            "Priority: absolute value", // label
            Integer.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Used with priorityOperation=ABSOLUTE: the new priority (0-100).")
            .addCondition(PRIORITY_OPERATION, List.of(OPERATION_ABSOLUTE))
            .setGroup("Priority")
            .setVisibleInTemplate();

    public static final String PRIORITY_LEVEL = "priorityLevel";
    public static final RunnerParameter parameterPriorityLevel = new RunnerParameter(
            PRIORITY_LEVEL, // name
            "Priority: level", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Used with priorityOperation=SETLEVEL: sets the priority to the value matching this Tasklist priority band (Low=25, Medium=50, High=75, Critical=100).")
            .addChoice(LEVEL_LOW, "Low")
            .addChoice(LEVEL_MEDIUM, "Medium")
            .addChoice(LEVEL_HIGH, "High")
            .addChoice(LEVEL_CRITICAL, "Critical")
            .setDefaultValue(LEVEL_HIGH)
            .addCondition(PRIORITY_OPERATION, List.of(OPERATION_SETLEVEL))
            .setGroup("Priority")
            .setVisibleInTemplate();

    public static final List<RunnerParameter> allParameters = List.of(
            parameterPriorityOperation,
            parameterPriorityStep,
            parameterPriorityAbsoluteValue,
            parameterPriorityLevel);

    private PriorityOperation() {
    }

    /**
     * @return true if the operation mutated the update command, so the caller knows it must be sent
     */
    public static boolean apply(UserTaskUpdateInput input, UserTask currentUserTask, UpdateUserTaskCommandStep1 command,
                                Map<String, Object> taskOutput) {
        taskOutput.put(UserTaskUpdateOutput.FIELD_PRIORITY, currentUserTask.getPriority());

        String operation = input.getPriorityOperation();
        if (operation == null || operation.isBlank()) {
            return false;
        }

        int newPriority;
        switch (operation.toUpperCase()) {
            case OPERATION_NOTHING:
                return false;
            case OPERATION_CLEAR:
                newPriority = DEFAULT_PRIORITY;
                break;
            case OPERATION_ABSOLUTE:
                Integer absoluteValue = input.getPriorityAbsoluteValue();
                if (absoluteValue == null) {
                    throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                            "priorityOperation=ABSOLUTE requires priorityAbsoluteValue");
                }
                newPriority = clamp(absoluteValue);
                break;
            case OPERATION_SETLEVEL:
                newPriority = levelToPriority(input.getPriorityLevel());
                break;
            case OPERATION_INCREASE:
            case OPERATION_DECREASE:
                int currentPriority = currentUserTask.getPriority() != null ? currentUserTask.getPriority() : DEFAULT_PRIORITY;
                int step = input.getPriorityStep() != null ? input.getPriorityStep() : 10;
                newPriority = clamp(OPERATION_INCREASE.equalsIgnoreCase(operation) ? currentPriority + step : currentPriority - step);
                break;
            default:
                throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                        "Unknown priorityOperation [" + operation + "], expected INCREASE/DECREASE/ABSOLUTE/SETLEVEL/CLEAR");
        }

        command.priority(newPriority);
        taskOutput.put(UserTaskUpdateOutput.FIELD_PRIORITY, newPriority);
        return true;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static int levelToPriority(String level) {
        if (level == null || level.isBlank()) {
            throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                    "priorityOperation=SETLEVEL requires priorityLevel");
        }
        switch (level.toUpperCase()) {
            case LEVEL_LOW:
                return LEVEL_LOW_VALUE;
            case LEVEL_MEDIUM:
                return LEVEL_MEDIUM_VALUE;
            case LEVEL_HIGH:
                return LEVEL_HIGH_VALUE;
            case LEVEL_CRITICAL:
                return LEVEL_CRITICAL_VALUE;
            default:
                throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                        "Unknown priorityLevel [" + level + "], expected LOW/MEDIUM/HIGH/CRITICAL");
        }
    }
}
