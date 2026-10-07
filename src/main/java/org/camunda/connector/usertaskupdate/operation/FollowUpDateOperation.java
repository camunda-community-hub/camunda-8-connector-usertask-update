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
 * Follow-up date operation group, driven by a single {@code followUpDateOperation} selector:
 * <ul>
 *     <li>INCREASE / DECREASE - shift the current follow-up date by {@code followUpDateStep}
 *     (ISO-8601 duration)</li>
 *     <li>ABSOLUTE - set the follow-up date to {@code followUpDateAbsoluteValue}</li>
 *     <li>CLEAR - remove the follow-up date (no date)</li>
 * </ul>
 * Backed by {@link UpdateUserTaskCommandStep1#followUpDate(String)} and
 * {@link UpdateUserTaskCommandStep1#clearFollowUpDate()}.
 */
public class FollowUpDateOperation {

    public static final String OPERATION_NOTHING = "NOTHING";
    public static final String OPERATION_INCREASE = "INCREASE";
    public static final String OPERATION_DECREASE = "DECREASE";
    public static final String OPERATION_ABSOLUTE = "ABSOLUTE";
    public static final String OPERATION_CLEAR = "CLEAR";

    public static final String FOLLOW_UP_DATE_OPERATION = "followUpDateOperation";
    public static final RunnerParameter parameterFollowUpDateOperation = new RunnerParameter(
            FOLLOW_UP_DATE_OPERATION, // name
            "Follow-up date: operation", // label
            String.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "NOTHING (default) leaves the follow-up date untouched. INCREASE/DECREASE the current follow-up date by followUpDateStep, set it to an ABSOLUTE value, or CLEAR it (no date).")
            .addChoice(OPERATION_NOTHING, "Nothing")
            .addChoice(OPERATION_INCREASE, "Increase")
            .addChoice(OPERATION_DECREASE, "Decrease")
            .addChoice(OPERATION_ABSOLUTE, "Absolute")
            .addChoice(OPERATION_CLEAR, "Clear")
            .setDefaultValue(OPERATION_NOTHING)
            .setGroup("Follow-up date")
            .setVisibleInTemplate();

    public static final String FOLLOW_UP_DATE_STEP = "followUpDateStep";
    public static final RunnerParameter parameterFollowUpDateStep = new RunnerParameter(
            FOLLOW_UP_DATE_STEP, // name
            "Follow-up date: step", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Used with followUpDateOperation=INCREASE/DECREASE: an ISO-8601 duration (e.g. PT2H, P1D) added to or removed from the current follow-up date. If the task has no follow-up date yet, now() is used as the base.")
            .addCondition(FOLLOW_UP_DATE_OPERATION, List.of(OPERATION_INCREASE, OPERATION_DECREASE))
            .setGroup("Follow-up date")
            .setVisibleInTemplate();

    public static final String FOLLOW_UP_DATE_ABSOLUTE_VALUE = "followUpDateAbsoluteValue";
    public static final RunnerParameter parameterFollowUpDateAbsoluteValue = new RunnerParameter(
            FOLLOW_UP_DATE_ABSOLUTE_VALUE, // name
            "Follow-up date: absolute value", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Used with followUpDateOperation=ABSOLUTE: the new follow-up date (ISO-8601 date-time).")
            .addCondition(FOLLOW_UP_DATE_OPERATION, List.of(OPERATION_ABSOLUTE))
            .setGroup("Follow-up date")
            .setVisibleInTemplate();

    public static final List<RunnerParameter> allParameters = List.of(
            parameterFollowUpDateOperation,
            parameterFollowUpDateStep,
            parameterFollowUpDateAbsoluteValue);

    private FollowUpDateOperation() {
    }

    public static boolean apply(UserTaskUpdateInput input, UserTask currentUserTask, UpdateUserTaskCommandStep1 command,
                                Map<String, Object> taskOutput) {
        taskOutput.put(UserTaskUpdateOutput.FIELD_FOLLOW_UP_DATE,
                currentUserTask.getFollowUpDate() != null ? currentUserTask.getFollowUpDate().toString() : null);

        String operation = input.getFollowUpDateOperation();
        if (operation == null || operation.isBlank()) {
            return false;
        }

        switch (operation.toUpperCase()) {
            case OPERATION_NOTHING:
                return false;
            case OPERATION_CLEAR:
                command.clearFollowUpDate();
                taskOutput.put(UserTaskUpdateOutput.FIELD_FOLLOW_UP_DATE, null);
                return true;
            case OPERATION_ABSOLUTE:
                String absoluteValue = input.getFollowUpDateAbsoluteValue();
                if (absoluteValue == null || absoluteValue.isBlank()) {
                    throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                            "followUpDateOperation=ABSOLUTE requires followUpDateAbsoluteValue");
                }
                command.followUpDate(absoluteValue);
                taskOutput.put(UserTaskUpdateOutput.FIELD_FOLLOW_UP_DATE, absoluteValue);
                return true;
            case OPERATION_INCREASE:
            case OPERATION_DECREASE:
                String step = input.getFollowUpDateStep();
                if (step == null || step.isBlank()) {
                    throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                            "followUpDateOperation=" + operation + " requires followUpDateStep");
                }
                OffsetDateTime base = currentUserTask.getFollowUpDate() != null ? currentUserTask.getFollowUpDate() : OffsetDateTime.now();
                Duration duration = Duration.parse(step);
                OffsetDateTime newFollowUpDate = OPERATION_INCREASE.equalsIgnoreCase(operation) ? base.plus(duration) : base.minus(duration);
                String newFollowUpDateStr = newFollowUpDate.toString();
                command.followUpDate(newFollowUpDateStr);
                taskOutput.put(UserTaskUpdateOutput.FIELD_FOLLOW_UP_DATE, newFollowUpDateStr);
                return true;
            default:
                throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                        "Unknown followUpDateOperation [" + operation + "], expected INCREASE/DECREASE/ABSOLUTE/CLEAR");
        }
    }
}
