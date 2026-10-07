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
 * Candidate groups operation group, driven by a single {@code candidateGroupsOperation} selector:
 * <ul>
 *     <li>FORCEASSIGN - set the candidate groups to {@code candidateGroupsValue}, replacing any
 *     current value</li>
 *     <li>ASSIGN - same as FORCEASSIGN, but only when the task currently has no candidate group
 *     (does nothing otherwise)</li>
 *     <li>CLEAR - remove every candidate group</li>
 *     <li>ADD - add {@code candidateGroupsValue} to the task's current candidate groups</li>
 *     <li>REMOVE - remove {@code candidateGroupsValue} from the task's current candidate groups</li>
 * </ul>
 * {@code candidateGroupsValue} is a String of group ids separated by comma, or a List of group ids.
 * Backed by {@link UpdateUserTaskCommandStep1#candidateGroups(List)} and
 * {@link UpdateUserTaskCommandStep1#clearCandidateGroups()}.
 */
public class CandidateGroupsOperation {

    public static final String OPERATION_NOTHING = "NOTHING";
    public static final String OPERATION_FORCEASSIGN = "FORCEASSIGN";
    public static final String OPERATION_ASSIGN = "ASSIGN";
    public static final String OPERATION_CLEAR = "CLEAR";
    public static final String OPERATION_ADD = "ADD";
    public static final String OPERATION_REMOVE = "REMOVE";

    public static final String CANDIDATE_GROUPS_OPERATION = "candidateGroupsOperation";
    public static final RunnerParameter parameterCandidateGroupsOperation = new RunnerParameter(
            CANDIDATE_GROUPS_OPERATION, // name
            "Candidate groups: operation", // label
            String.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "NOTHING (default) leaves candidate groups untouched. FORCEASSIGN sets candidateGroupsValue unconditionally; ASSIGN sets it only if the task has no candidate group yet; CLEAR removes every candidate group; ADD/REMOVE add or remove candidateGroupsValue from the task's current candidate groups.")
            .addChoice(OPERATION_NOTHING, "Nothing")
            .addChoice(OPERATION_FORCEASSIGN, "Force assign")
            .addChoice(OPERATION_ASSIGN, "Assign if not assigned")
            .addChoice(OPERATION_CLEAR, "Clear")
            .addChoice(OPERATION_ADD, "Add")
            .addChoice(OPERATION_REMOVE, "Remove")
            .setDefaultValue(OPERATION_NOTHING)
            .setGroup("Candidate groups")
            .setVisibleInTemplate();

    public static final String CANDIDATE_GROUPS_VALUE = "candidateGroupsValue";
    public static final RunnerParameter parameterCandidateGroupsValue = new RunnerParameter(
            CANDIDATE_GROUPS_VALUE, // name
            "Candidate groups: value", // label
            Object.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Used with candidateGroupsOperation=FORCEASSIGN/ASSIGN/ADD/REMOVE. Input is a String of GroupId, separated by comma, or a List of GroupId (String).")
            .addCondition(CANDIDATE_GROUPS_OPERATION, List.of(OPERATION_FORCEASSIGN, OPERATION_ASSIGN, OPERATION_ADD, OPERATION_REMOVE))
            .setGroup("Candidate groups")
            .setVisibleInTemplate();

    public static final List<RunnerParameter> allParameters = List.of(
            parameterCandidateGroupsOperation,
            parameterCandidateGroupsValue);

    private CandidateGroupsOperation() {
    }

    public static boolean apply(UserTaskUpdateInput input, UserTask currentUserTask, UpdateUserTaskCommandStep1 command,
                                Map<String, Object> taskOutput) {
        List<String> currentGroups = currentUserTask.getCandidateGroups() != null ? currentUserTask.getCandidateGroups() : List.of();
        taskOutput.put(UserTaskUpdateOutput.FIELD_CANDIDATE_GROUPS, currentGroups);

        String operation = input.getCandidateGroupsOperation();
        if (operation == null || operation.isBlank()) {
            return false;
        }

        List<String> newGroups;
        switch (operation.toUpperCase()) {
            case OPERATION_NOTHING:
                return false;
            case OPERATION_CLEAR:
                command.clearCandidateGroups();
                taskOutput.put(UserTaskUpdateOutput.FIELD_CANDIDATE_GROUPS, List.of());
                return true;
            case OPERATION_ASSIGN:
                if (!currentGroups.isEmpty()) {
                    return false;
                }
                // fallthrough intentional: no candidate group yet, behave like FORCEASSIGN
            case OPERATION_FORCEASSIGN:
                newGroups = getRequiredValue(input, operation);
                break;
            case OPERATION_ADD:
                Set<String> merged = new LinkedHashSet<>(currentGroups);
                merged.addAll(getRequiredValue(input, operation));
                newGroups = List.copyOf(merged);
                break;
            case OPERATION_REMOVE:
                List<String> toRemove = getRequiredValue(input, operation);
                newGroups = currentGroups.stream().filter(group -> !toRemove.contains(group)).toList();
                break;
            default:
                throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                        "Unknown candidateGroupsOperation [" + operation + "], expected FORCEASSIGN/ASSIGN/CLEAR/ADD/REMOVE");
        }

        command.candidateGroups(newGroups);
        taskOutput.put(UserTaskUpdateOutput.FIELD_CANDIDATE_GROUPS, newGroups);
        return true;
    }

    private static List<String> getRequiredValue(UserTaskUpdateInput input, String operation) {
        List<String> value = ParameterToolbox.getListOfString(input.getCandidateGroupsValue());
        if (value.isEmpty()) {
            throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER,
                    "candidateGroupsOperation=" + operation + " requires candidateGroupsValue");
        }
        return value;
    }
}
