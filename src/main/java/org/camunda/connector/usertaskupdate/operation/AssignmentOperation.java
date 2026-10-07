package org.camunda.connector.usertaskupdate.operation;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.camunda.connector.usertaskupdate.UserTaskUpdateInput;
import org.camunda.connector.usertaskupdate.UserTaskUpdateOutput;
import org.camunda.connector.usertaskupdate.toolbox.UserTaskUpdateError;

import java.util.List;
import java.util.Map;

/**
 * Assignment operation group, driven by a single {@code assigneeOperation} selector:
 * <ul>
 *     <li>FORCEASSIGN - set {@code assignee} unconditionally, replacing any current assignee</li>
 *     <li>ASSIGN - same as FORCEASSIGN, but only when the task is not already assigned (does
 *     nothing otherwise)</li>
 *     <li>CLEAR - unassign the task</li>
 * </ul>
 * Unlike priority/dueDate/followUpDate/candidateGroups/candidateUsers/action, this is NOT part of
 * {@code UpdateUserTaskCommandStep1} - the CamundaClient API exposes assignment through its own
 * dedicated commands, {@code newAssignUserTaskCommand} and {@code newUnassignUserTaskCommand}.
 */
public class AssignmentOperation {

    public static final String OPERATION_NOTHING = "NOTHING";
    public static final String OPERATION_FORCEASSIGN = "FORCEASSIGN";
    public static final String OPERATION_ASSIGN = "ASSIGN";
    public static final String OPERATION_UNASSIGN = "CLEAR";

    public static final String ASSIGNEE_OPERATION = "assigneeOperation";
    public static final RunnerParameter parameterAssigneeOperation = new RunnerParameter(
            ASSIGNEE_OPERATION, // name
            "Assignment: operation", // label
            String.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "NOTHING (default) leaves the assignment untouched. FORCEASSIGN sets assignee unconditionally; ASSIGN sets it only if the task is not already assigned; CLEAR unassigns the task.")
            .addChoice(OPERATION_NOTHING, "Nothing")
            .addChoice(OPERATION_FORCEASSIGN, "Force assign")
            .addChoice(OPERATION_ASSIGN, "Assign if not assigned")
            .addChoice(OPERATION_UNASSIGN, "Unassign")
            .setDefaultValue(OPERATION_NOTHING)
            .setGroup("Assignment")
            .setVisibleInTemplate();

    public static final String ASSIGNEE = "assignee";
    public static final RunnerParameter parameterAssignee = new RunnerParameter(
            ASSIGNEE, // name
            "Assignee", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Used with assigneeOperation=FORCEASSIGN/ASSIGN: set this user as the assignee of the task.")
            .addCondition(ASSIGNEE_OPERATION, List.of(OPERATION_FORCEASSIGN, OPERATION_ASSIGN))
            .setGroup("Assignment")
            .setVisibleInTemplate();

    public static final List<RunnerParameter> allParameters = List.of(
            parameterAssigneeOperation,
            parameterAssignee);

    private AssignmentOperation() {
    }

    /**
     * @return true if an assignment command (assign or unassign) was sent to the given user task
     */
    public static boolean apply(UserTaskUpdateInput input, UserTask currentUserTask, CamundaClient camundaClient,
                                long userTaskKey, Map<String, Object> taskOutput) {
        taskOutput.put(UserTaskUpdateOutput.FIELD_ASSIGNEE, currentUserTask.getAssignee());

        String operation = input.getAssigneeOperation();
        if (operation == null || operation.isBlank()) {
            return false;
        }

        switch (operation.toUpperCase()) {
            case OPERATION_NOTHING:
                return false;
            case OPERATION_UNASSIGN:
                camundaClient.newUnassignUserTaskCommand(userTaskKey).execute();
                taskOutput.put(UserTaskUpdateOutput.FIELD_ASSIGNEE, null);
                return true;
            case OPERATION_ASSIGN:
                String currentAssignee = currentUserTask.getAssignee();
                if (currentAssignee != null && !currentAssignee.isBlank()) {
                    return false;
                }
                // fallthrough intentional: not yet assigned, behave like FORCEASSIGN
            case OPERATION_FORCEASSIGN:
                String assignee = input.getAssignee();
                if (assignee == null || assignee.isBlank()) {
                    throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                            "assigneeOperation=" + operation + " requires assignee");
                }
                camundaClient.newAssignUserTaskCommand(userTaskKey)
                        .assignee(assignee)
                        .allowOverride(true)
                        .execute();
                taskOutput.put(UserTaskUpdateOutput.FIELD_ASSIGNEE, assignee);
                return true;
            default:
                throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                        "Unknown assigneeOperation [" + operation + "], expected FORCEASSIGN/ASSIGN/CLEAR");
        }
    }
}
