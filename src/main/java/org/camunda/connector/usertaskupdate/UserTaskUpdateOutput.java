package org.camunda.connector.usertaskupdate;

import io.camunda.connector.cherrytemplate.CherryOutput;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.camunda.connector.usertaskupdate.toolbox.ParameterToolbox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Output of the UserTaskUpdate connector.
 * A single output variable "updatedUserTasks" is produced: a list with one record per user task that
 * was actually updated. Each record carries the task identification (userTaskKey, elementId,
 * processInstanceKey) plus, for every operation that was applied on that task, the resulting value
 * under its own field (priority, dueDate, followUpDate, assignee, candidateGroups, candidateUsers,
 * action).
 * <pre>
 * [
 *   {
 *     "userTaskKey": ...,
 *     "elementId": ...,
 *     "processInstanceKey": ...,
 *     "priority": ...,
 *     "dueDate": ...,
 *     "assignee": ...
 *   }
 * ]
 * </pre>
 */
public class UserTaskUpdateOutput implements CherryOutput {

    public static final String OUTPUT_UPDATED_USER_TASKS = "updatedUserTasks";
    public static final RunnerParameter parameterUpdatedUserTasks = new RunnerParameter(
            OUTPUT_UPDATED_USER_TASKS, // name
            "Updated user tasks", // label
            List.class, // class
            RunnerParameter.Level.OPTIONAL,
            "List, one record per updated user task, with the userTaskKey/elementId/processInstanceKey and the resulting value of each operation that was applied on it (priority, dueDate, followUpDate, assignee, candidateGroups, candidateUsers, action).");
    public static final List<RunnerParameter> allParameters = List.of(parameterUpdatedUserTasks);

    // Fields of one updated user task record
    public static final String FIELD_USER_TASK_KEY = "userTaskKey";
    public static final String FIELD_ELEMENT_ID = "elementId";
    public static final String FIELD_PROCESS_INSTANCE_KEY = "processInstanceKey";
    public static final String FIELD_PRIORITY = "priority";
    public static final String FIELD_DUE_DATE = "dueDate";
    public static final String FIELD_FOLLOW_UP_DATE = "followUpDate";
    public static final String FIELD_ASSIGNEE = "assignee";
    public static final String FIELD_CANDIDATE_GROUPS = "candidateGroups";
    public static final String FIELD_CANDIDATE_USERS = "candidateUsers";

    private final List<Map<String, Object>> updatedUserTasks = new ArrayList<>();

    public List<Map<String, Object>> getUpdatedUserTasks() {
        return updatedUserTasks;
    }

    /**
     * Record one user task that was updated, together with the resulting value of every operation
     * that was applied on it (as collected by the *.operation classes in a per-task result map).
     */
    public void addUpdatedUserTask(long userTaskKey, String elementId, long processInstanceKey, Map<String, Object> operationResults) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put(FIELD_USER_TASK_KEY, userTaskKey);
        record.put(FIELD_ELEMENT_ID, elementId);
        record.put(FIELD_PROCESS_INSTANCE_KEY, processInstanceKey);
        record.putAll(operationResults);
        updatedUserTasks.add(record);
    }

    @Override
    public List<Map<String, Object>> getOutputParameters() {
        return ParameterToolbox.getOutputParameters(allParameters);
    }
}
