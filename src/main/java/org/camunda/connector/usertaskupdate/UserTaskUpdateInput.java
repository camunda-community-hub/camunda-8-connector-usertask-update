package org.camunda.connector.usertaskupdate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.camunda.connector.cherrytemplate.CherryInput;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.camunda.connector.usertaskupdate.operation.*;
import org.camunda.connector.usertaskupdate.toolbox.ParameterToolbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * the JsonIgnoreProperties is mandatory: the template may contain additional widget to help the designer, especially on the OPTIONAL parameters
 * This avoids the MAPPING Exception
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserTaskUpdateInput implements CherryInput {

    /**
     * Attention, each Input here must be added in the UserTaskUpdateFunction, list of InputVariables
     */

    // ----------------------------------------------------------------------------------
    // Targeting: which user task(s) does this operation apply to
    // ----------------------------------------------------------------------------------

    public static final String USER_TASK_KEY = "userTaskKey";
    public static final RunnerParameter parameterUserTaskKey = new RunnerParameter(
            USER_TASK_KEY, // name
            "User task key(s)", // label
            Object.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "userTaskKey or List<userTaskKey>. Update exactly these user tasks. Takes precedence over processInstanceKey/filterTaskElementId: when set, no search is performed.")
            .setVisibleInTemplate();

    public static final String PROCESS_INSTANCE_KEY = "processInstanceKey";
    public static final RunnerParameter parameterProcessInstanceKey = new RunnerParameter(
            PROCESS_INSTANCE_KEY, // name
            "Process instance key", // label
            Long.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "Search the active (CREATED) user tasks of this process instance and update them. When not set (and userTaskKey is not set), the current process instance is used. Ignored when userTaskKey is set.")
            .setVisibleInTemplate();

    public static final String FILTER_TASK_ELEMENT_ID = "filterTaskElementId";
    public static final RunnerParameter parameterFilterTaskElementId = new RunnerParameter(
            FILTER_TASK_ELEMENT_ID, // name
            "Filter task element id(s)", // label
            Object.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "TaskElementId or List<TaskElementId>. When searching by processInstanceKey, only update these task ids (user task element id). Leave empty to update every active user task found. Ignored when userTaskKey is set.")
            .setVisibleInTemplate();


    public static final List<RunnerParameter> allParameters = new ArrayList<>();

    static {
        allParameters.add(parameterUserTaskKey);
        allParameters.add(parameterProcessInstanceKey);
        allParameters.add(parameterFilterTaskElementId);
        allParameters.addAll(PriorityOperation.allParameters);
        allParameters.addAll(DueDateOperation.allParameters);
        allParameters.addAll(FollowUpDateOperation.allParameters);
        allParameters.addAll(AssignmentOperation.allParameters);
        allParameters.addAll(CandidateGroupsOperation.allParameters);
        allParameters.addAll(CandidateUsersOperation.allParameters);
    }

    // ----------------------------------------------------------------------------------
    // Fields bound by Jackson from the job's input variables. Each field's name matches the
    // RunnerParameter name declared above (here) or in the matching operation.* class.
    // ----------------------------------------------------------------------------------

    public Object userTaskKey;
    public Long processInstanceKey;
    public Object filterTaskElementId;

    // Priority
    public String priorityOperation;
    public Integer priorityStep = 10;
    public Integer priorityAbsoluteValue;
    public String priorityLevel;

    // Due date
    public String dueDateOperation;
    public String dueDateStep;
    public String dueDateAbsoluteValue;

    // Follow-up date
    public String followUpDateOperation;
    public String followUpDateStep;
    public String followUpDateAbsoluteValue;

    // Assignment
    public String assigneeOperation;
    public String assignee;

    // Candidate groups
    public String candidateGroupsOperation;
    public Object candidateGroupsValue;

    // Candidate users
    public String candidateUsersOperation;
    public Object candidateUsersValue;

    public Object getUserTaskKey() {
        return userTaskKey;
    }

    public Long getProcessInstanceKey() {
        return processInstanceKey;
    }

    public Object getFilterTaskElementId() {
        return filterTaskElementId;
    }


    public String getPriorityOperation() {
        return priorityOperation;
    }

    public Integer getPriorityStep() {
        return priorityStep;
    }

    public Integer getPriorityAbsoluteValue() {
        return priorityAbsoluteValue;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    public String getDueDateOperation() {
        return dueDateOperation;
    }

    public String getDueDateStep() {
        return dueDateStep;
    }

    public String getDueDateAbsoluteValue() {
        return dueDateAbsoluteValue;
    }

    public String getFollowUpDateOperation() {
        return followUpDateOperation;
    }

    public String getFollowUpDateStep() {
        return followUpDateStep;
    }

    public String getFollowUpDateAbsoluteValue() {
        return followUpDateAbsoluteValue;
    }

    public String getAssigneeOperation() {
        return assigneeOperation;
    }

    public String getAssignee() {
        return assignee;
    }


    public String getCandidateGroupsOperation() {
        return candidateGroupsOperation;
    }

    public Object getCandidateGroupsValue() {
        return candidateGroupsValue;
    }

    public String getCandidateUsersOperation() {
        return candidateUsersOperation;
    }

    public Object getCandidateUsersValue() {
        return candidateUsersValue;
    }


    @Override
    public List<Map<String, Object>> getInputParameters() {
        return ParameterToolbox.getInputParameters(allParameters);
    }
}
