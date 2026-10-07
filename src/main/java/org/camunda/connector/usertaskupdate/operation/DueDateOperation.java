package org.camunda.connector.usertaskupdate.operation;

import io.camunda.client.api.command.UpdateUserTaskCommandStep1;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.camunda.connector.usertaskupdate.UserTaskUpdateInput;
import org.camunda.connector.usertaskupdate.UserTaskUpdateOutput;
import org.camunda.connector.usertaskupdate.toolbox.UserTaskUpdateError;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Due date operation group, driven by a single {@code dueDateOperation} selector:
 * <ul>
 *     <li>INCREASE / DECREASE - shift the current due date by {@code dueDateStep} (ISO-8601 duration)</li>
 *     <li>ABSOLUTE - set the due date to {@code dueDateAbsoluteValue}</li>
 *     <li>CLEAR - remove the due date (no date)</li>
 * </ul>
 * Backed by {@link UpdateUserTaskCommandStep1#dueDate(String)} and
 * {@link UpdateUserTaskCommandStep1#clearDueDate()}.
 */
public class DueDateOperation {

    public static final String OPERATION_NOTHING = "NOTHING";
    public static final String OPERATION_INCREASE = "INCREASE";
    public static final String OPERATION_DECREASE = "DECREASE";
    public static final String OPERATION_ABSOLUTE = "ABSOLUTE";
    public static final String OPERATION_CLEAR = "CLEAR";

    public static final String DUE_DATE_OPERATION = "dueDateOperation";
    public static final RunnerParameter parameterDueDateOperation = new RunnerParameter(
            DUE_DATE_OPERATION, // name
            "Due date: operation", // label
            String.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "NOTHING (default) leaves the due date untouched. INCREASE/DECREASE the current due date by dueDateStep, set it to an ABSOLUTE value, or CLEAR it (no date).")
            .addChoice(OPERATION_NOTHING, "Nothing")
            .addChoice(OPERATION_INCREASE, "Increase")
            .addChoice(OPERATION_DECREASE, "Decrease")
            .addChoice(OPERATION_ABSOLUTE, "Absolute")
            .addChoice(OPERATION_CLEAR, "Clear")
            .setDefaultValue(OPERATION_NOTHING)
            .setGroup("Due date")
            .setVisibleInTemplate();

    public static final String DUE_DATE_STEP = "dueDateStep";
    public static final RunnerParameter parameterDueDateStep = new RunnerParameter(
            DUE_DATE_STEP, // name
            "Due date: step", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Used with dueDateOperation=INCREASE/DECREASE: an ISO-8601 duration (e.g. PT2H, P1D) added to or removed from the current due date. If the task has no due date yet, now() is used as the base.")
            .addCondition(DUE_DATE_OPERATION, List.of(OPERATION_INCREASE, OPERATION_DECREASE))
            .setGroup("Due date")
            .setVisibleInTemplate();

    public static final String DUE_DATE_ABSOLUTE_VALUE = "dueDateAbsoluteValue";
    public static final RunnerParameter parameterDueDateAbsoluteValue = new RunnerParameter(
            DUE_DATE_ABSOLUTE_VALUE, // name
            "Due date: absolute value", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Used with dueDateOperation=ABSOLUTE: the new due date (ISO-8601 date-time, e.g. 2026-12-31T23:59:00Z).")
            .addCondition(DUE_DATE_OPERATION, List.of(OPERATION_ABSOLUTE))
            .setGroup("Due date")
            .setVisibleInTemplate();

    public static final List<RunnerParameter> allParameters = List.of(
            parameterDueDateOperation,
            parameterDueDateStep,
            parameterDueDateAbsoluteValue);

    private DueDateOperation() {
    }

    public static boolean apply(UserTaskUpdateInput input, UserTask currentUserTask, UpdateUserTaskCommandStep1 command,
                                Map<String, Object> taskOutput) {
        taskOutput.put(UserTaskUpdateOutput.FIELD_DUE_DATE,
                currentUserTask.getDueDate() != null ? currentUserTask.getDueDate().toString() : null);

        String operation = input.getDueDateOperation();
        if (operation == null || operation.isBlank()) {
            return false;
        }

        switch (operation.toUpperCase()) {
            case OPERATION_NOTHING:
                return false;
            case OPERATION_CLEAR:
                command.clearDueDate();
                taskOutput.put(UserTaskUpdateOutput.FIELD_DUE_DATE, null);
                return true;
            case OPERATION_ABSOLUTE:
                String absoluteValue = input.getDueDateAbsoluteValue();
                if (absoluteValue == null || absoluteValue.isBlank()) {
                    throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                            "dueDateOperation=ABSOLUTE requires dueDateAbsoluteValue");
                }
                command.dueDate(absoluteValue);
                taskOutput.put(UserTaskUpdateOutput.FIELD_DUE_DATE, absoluteValue);
                return true;
            case OPERATION_INCREASE:
            case OPERATION_DECREASE:
                String step = input.getDueDateStep();
                if (step == null || step.isBlank()) {
                    throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                            "dueDateOperation=" + operation + " requires dueDateStep");
                }
                OffsetDateTime base = currentUserTask.getDueDate() != null ? currentUserTask.getDueDate() : OffsetDateTime.now();
                Duration duration;
                try {
                    duration = Duration.parse(step);
                } catch (Exception e) {
                    throw new ConnectorException(UserTaskUpdateError.ERROR_PARSE_DURATION,
                            "dueDateOperation=" + operation + " ParseDuration[" + step + "] failed : " + e.getMessage());
                }
                OffsetDateTime newDueDate = OPERATION_INCREASE.equalsIgnoreCase(operation) ? base.plus(duration) : base.minus(duration);
                String newDueDateStr = newDueDate.toString();
                command.dueDate(newDueDateStr);
                taskOutput.put(UserTaskUpdateOutput.FIELD_DUE_DATE, newDueDateStr);
                return true;
            default:
                throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                        "Unknown dueDateOperation [" + operation + "], expected INCREASE/DECREASE/ABSOLUTE/CLEAR");
        }
    }
}
