package org.camunda.connector.usertaskupdate.operation;

import io.camunda.client.api.command.UpdateUserTaskCommandStep1;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.camunda.connector.usertaskupdate.UserTaskUpdateInput;
import org.camunda.connector.usertaskupdate.UserTaskUpdateOutput;
import org.camunda.connector.usertaskupdate.toolbox.ParameterToolbox;
import org.camunda.connector.usertaskupdate.toolbox.UserTaskUpdateError;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Candidate users operation group, driven by a single {@code candidateUsersOperation} selector:
 * <ul>
 *     <li>FORCEASSIGN - set the candidate users to {@code candidateUsersValue}, replacing any
 *     current value</li>
 *     <li>ASSIGN - same as FORCEASSIGN, but only when the task currently has no candidate user
 *     (does nothing otherwise)</li>
 *     <li>CLEAR - remove every candidate user</li>
 *     <li>ADD - add {@code candidateUsersValue} to the task's current candidate users</li>
 *     <li>REMOVE - remove {@code candidateUsersValue} from the task's current candidate users</li>
 * </ul>
 * {@code candidateUsersValue} is a String of usernames separated by comma, or a List of usernames.
 * Backed by {@link UpdateUserTaskCommandStep1#candidateUsers(List)} and
 * {@link UpdateUserTaskCommandStep1#clearCandidateUsers()}.
 */
public class CandidateUsersOperation {

    public static final String OPERATION_NOTHING = "NOTHING";
    public static final String OPERATION_FORCEASSIGN = "FORCEASSIGN";
    public static final String OPERATION_ASSIGN = "ASSIGN";
    public static final String OPERATION_CLEAR = "CLEAR";
    public static final String OPERATION_ADD = "ADD";
    public static final String OPERATION_REMOVE = "REMOVE";

    public static final String CANDIDATE_USERS_OPERATION = "candidateUsersOperation";
    public static final RunnerParameter parameterCandidateUsersOperation = new RunnerParameter(
            CANDIDATE_USERS_OPERATION, // name
            "Candidate users: operation", // label
            String.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "NOTHING (default) leaves candidate users untouched. FORCEASSIGN sets candidateUsersValue unconditionally; ASSIGN sets it only if the task has no candidate user yet; CLEAR removes every candidate user; ADD/REMOVE add or remove candidateUsersValue from the task's current candidate users.")
            .addChoice(OPERATION_NOTHING, "Nothing")
            .addChoice(OPERATION_FORCEASSIGN, "Force assign")
            .addChoice(OPERATION_ASSIGN, "Assign if not assigned")
            .addChoice(OPERATION_CLEAR, "Clear")
            .addChoice(OPERATION_ADD, "Add")
            .addChoice(OPERATION_REMOVE, "Remove")
            .setDefaultValue(OPERATION_NOTHING)
            .setGroup("Candidate users")
            .setVisibleInTemplate();

    public static final String CANDIDATE_USERS_VALUE = "candidateUsersValue";
    public static final RunnerParameter parameterCandidateUsersValue = new RunnerParameter(
            CANDIDATE_USERS_VALUE, // name
            "Candidate users: value", // label
            Object.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Used with candidateUsersOperation=FORCEASSIGN/ASSIGN/ADD/REMOVE. Input is a String of UserName, separated by comma, or a List of UserName (String).")
            .addCondition(CANDIDATE_USERS_OPERATION, List.of(OPERATION_FORCEASSIGN, OPERATION_ASSIGN, OPERATION_ADD, OPERATION_REMOVE))
            .setGroup("Candidate users")
            .setVisibleInTemplate();

    public static final List<RunnerParameter> allParameters = List.of(
            parameterCandidateUsersOperation,
            parameterCandidateUsersValue);

    private CandidateUsersOperation() {
    }

    public static boolean apply(UserTaskUpdateInput input, UserTask currentUserTask, UpdateUserTaskCommandStep1 command,
                                Map<String, Object> taskOutput) {
        List<String> currentUsers = currentUserTask.getCandidateUsers() != null ? currentUserTask.getCandidateUsers() : List.of();
        taskOutput.put(UserTaskUpdateOutput.FIELD_CANDIDATE_USERS, currentUsers);

        String operation = input.getCandidateUsersOperation();
        if (operation == null || operation.isBlank()) {
            return false;
        }

        List<String> newUsers;
        switch (operation.toUpperCase()) {
            case OPERATION_NOTHING:
                return false;
            case OPERATION_CLEAR:
                command.clearCandidateUsers();
                taskOutput.put(UserTaskUpdateOutput.FIELD_CANDIDATE_USERS, List.of());
                return true;
            case OPERATION_ASSIGN:
                if (!currentUsers.isEmpty()) {
                    return false;
                }
                // fallthrough intentional: no candidate user yet, behave like FORCEASSIGN
            case OPERATION_FORCEASSIGN:
                newUsers = getRequiredValue(input, operation);
                break;
            case OPERATION_ADD:
                Set<String> merged = new LinkedHashSet<>(currentUsers);
                merged.addAll(getRequiredValue(input, operation));
                newUsers = List.copyOf(merged);
                break;
            case OPERATION_REMOVE:
                List<String> toRemove = getRequiredValue(input, operation);
                newUsers = currentUsers.stream().filter(userName -> !toRemove.contains(userName)).toList();
                break;
            default:
                throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                        "Unknown candidateUsersOperation [" + operation + "], expected FORCEASSIGN/ASSIGN/CLEAR/ADD/REMOVE");
        }

        command.candidateUsers(newUsers);
        taskOutput.put(UserTaskUpdateOutput.FIELD_CANDIDATE_USERS, newUsers);
        return true;
    }

    private static List<String> getRequiredValue(UserTaskUpdateInput input, String operation) {
        List<String> value = ParameterToolbox.getListOfString(input.getCandidateUsersValue());
        if (value.isEmpty()) {
            throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                    "candidateUsersOperation=" + operation + " requires candidateUsersValue");
        }
        return value;
    }
}
