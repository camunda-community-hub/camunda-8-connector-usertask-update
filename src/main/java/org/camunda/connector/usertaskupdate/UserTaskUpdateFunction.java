package org.camunda.connector.usertaskupdate;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.UpdateUserTaskCommandStep1;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.cherrytemplate.CherryConnector;
import org.camunda.connector.usertaskupdate.operation.*;
import org.camunda.connector.usertaskupdate.toolbox.ParameterToolbox;
import org.camunda.connector.usertaskupdate.toolbox.UserTaskUpdateError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Update one or several user tasks: priority, due date, follow-up date, assignment (assignee /
 * unassign), candidate groups, candidate users, and the free-text "action" audit label.
 * <p>
 * The target user task(s) are resolved either directly from {@code userTaskKey} (one key or a list),
 * or by searching the active (state=CREATED) user tasks of a process instance (the current one by
 * default, or {@code processInstanceKey}), optionally restricted to {@code filterTaskElementId}.
 */
@OutboundConnector(name = "UserTaskUpdateFunction", inputVariables = {
        UserTaskUpdateInput.USER_TASK_KEY,
        UserTaskUpdateInput.PROCESS_INSTANCE_KEY,
        UserTaskUpdateInput.FILTER_TASK_ELEMENT_ID,
        PriorityOperation.PRIORITY_OPERATION,
        PriorityOperation.PRIORITY_STEP,
        PriorityOperation.PRIORITY_ABSOLUTE_VALUE,
        PriorityOperation.PRIORITY_LEVEL,
        DueDateOperation.DUE_DATE_OPERATION,
        DueDateOperation.DUE_DATE_STEP,
        DueDateOperation.DUE_DATE_ABSOLUTE_VALUE,
        FollowUpDateOperation.FOLLOW_UP_DATE_OPERATION,
        FollowUpDateOperation.FOLLOW_UP_DATE_STEP,
        FollowUpDateOperation.FOLLOW_UP_DATE_ABSOLUTE_VALUE,
        AssignmentOperation.ASSIGNEE_OPERATION,
        AssignmentOperation.ASSIGNEE,
        CandidateGroupsOperation.CANDIDATE_GROUPS_OPERATION,
        CandidateGroupsOperation.CANDIDATE_GROUPS_VALUE,
        CandidateUsersOperation.CANDIDATE_USERS_OPERATION,
        CandidateUsersOperation.CANDIDATE_USERS_VALUE,
}, type = "c-usertaskupdate-function")
public class UserTaskUpdateFunction implements OutboundConnectorFunction, CherryConnector {

    private static final String WORKER_LOGO = "data:image/png;base64,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";

    private final Logger logger = LoggerFactory.getLogger(UserTaskUpdateFunction.class.getName());

    /**
     * See InvolvedUserFunction for why the CamundaClient can't be obtained from the
     * OutboundConnectorContext and is instead injected as a Spring bean (see
     * UserTaskUpdateFunctionAutoConfiguration).
     */
    @Nullable
    private final CamundaClient camundaClient;

    public UserTaskUpdateFunction() {
        this(null);
    }

    public UserTaskUpdateFunction(@Nullable CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @Override
    public Object execute(OutboundConnectorContext outboundConnectorContext) throws ConnectorException {
        UserTaskUpdateInput input;
        try {
            input = outboundConnectorContext.bindVariables(UserTaskUpdateInput.class);
        } catch (Exception e) {
            logger.error("Bad Input Parameters to bindVariables ", e);
            throw new ConnectorException(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER, "UserTaskUpdate can't bind variable [" + e.getMessage() + "]");
        }

        if (camundaClient == null) {
            logger.error("No CamundaClient available: UserTaskUpdateFunction must be run as a Spring bean so the CamundaClient can be injected");
            throw new ConnectorException(UserTaskUpdateError.ERROR_NO_CAMUNDA_CLIENT, UserTaskUpdateError.ERROR_NO_CAMUNDA_CLIENT_EXPLANATION);
        }

        long beginTime = System.currentTimeMillis();
        UserTaskUpdateOutput output = new UserTaskUpdateOutput();

        List<UserTask> targetUserTasks = resolveTargetUserTasks(outboundConnectorContext, input);
        logger.info("UserTaskUpdateFunction: {} target user task(s) resolved", targetUserTasks.size());

        for (UserTask userTask : targetUserTasks) {
            long userTaskKey = userTask.getUserTaskKey();
            try {
                Map<String, Object> taskOutput = new LinkedHashMap<>();
                UpdateUserTaskCommandStep1 command = camundaClient.newUpdateUserTaskCommand(userTaskKey);

                boolean anyUpdateApplied = PriorityOperation.apply(input, userTask, command, taskOutput);
                anyUpdateApplied |= DueDateOperation.apply(input, userTask, command, taskOutput);
                anyUpdateApplied |= FollowUpDateOperation.apply(input, userTask, command, taskOutput);
                anyUpdateApplied |= CandidateGroupsOperation.apply(input, userTask, command, taskOutput);
                anyUpdateApplied |= CandidateUsersOperation.apply(input, userTask, command, taskOutput);

                if (anyUpdateApplied) {
                    command.execute();
                }

                boolean assignmentApplied = AssignmentOperation.apply(input, userTask, camundaClient, userTaskKey, taskOutput);

                if (anyUpdateApplied || assignmentApplied) {
                    output.addUpdatedUserTask(userTaskKey, userTask.getElementId(), userTask.getProcessInstanceKey(), taskOutput);
                }
            } catch (ConnectorException ce) {
                throw ce;
            } catch (Exception e) {
                logger.error("UserTaskUpdateFunction: Can't update user task [{}] : {}", userTaskKey, e.getMessage());
                throw new ConnectorException(UserTaskUpdateError.CANT_UPDATE_USERTASK, "UserTaskKey[" + userTaskKey + "] errors :" + e.getMessage());
            }
        }

        logger.info("UserTaskUpdateFunction End in {} ms, {} task(s) updated", System.currentTimeMillis() - beginTime, output.getUpdatedUserTasks().size());
        return output;
    }

    /**
     * Resolve the user task(s) this operation must be applied on: directly from userTaskKey when
     * given, otherwise by searching the active (CREATED) user tasks of a process instance (the
     * current one by default), optionally restricted to filterTaskElementId.
     */
    private List<UserTask> resolveTargetUserTasks(OutboundConnectorContext outboundConnectorContext, UserTaskUpdateInput input) {
        List<Long> userTaskKeys = ParameterToolbox.getListOfLong(input.getUserTaskKey());
        if (!userTaskKeys.isEmpty()) {
            List<UserTask> result = new ArrayList<>();
            for (Long userTaskKey : userTaskKeys) {
                result.add(camundaClient.newUserTaskGetRequest(userTaskKey).execute());
            }
            return result;
        }

        long processInstanceKey = input.getProcessInstanceKey() != null
                ? input.getProcessInstanceKey()
                : outboundConnectorContext.getJobContext().getProcessInstanceKey();
        if (processInstanceKey == 0) {
            throw new ConnectorException(UserTaskUpdateError.ERROR_NO_PROCESSINSTANCE, UserTaskUpdateError.ERROR_NO_PROCESSINSTANCE_EXPLANATION);
        }

        List<UserTask> userTasks = camundaClient.newUserTaskSearchRequest()
                .filter(f -> f.processInstanceKey(processInstanceKey).state(UserTaskState.CREATED))
                .execute()
                .items();

        List<String> filterTaskElementId = ParameterToolbox.getListOfString(input.getFilterTaskElementId());
        if (filterTaskElementId.isEmpty()) {
            return userTasks;
        }
        return userTasks.stream().filter(userTask -> filterTaskElementId.contains(userTask.getElementId())).toList();
    }

    @Override
    public String getDescription() {
        return "Update one or several user tasks: priority, due date, follow-up date, assignment (assignee/unassign), candidate groups, candidate users, and a free-text action label.";
    }

    @Override
    public String getLogo() {
        return WORKER_LOGO;
    }

    @Override
    public String getCollectionName() {
        return "Users";
    }

    @Override
    public Map<String, String> getListBpmnErrors() {
        Map<String, String> allErrors = new java.util.HashMap<>();
        allErrors.put(UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER, UserTaskUpdateError.ERROR_BAD_INPUTPARAMETER_EXPLANATION);
        allErrors.put(UserTaskUpdateError.ERROR_NO_PROCESSINSTANCE, UserTaskUpdateError.ERROR_NO_PROCESSINSTANCE_EXPLANATION);
        allErrors.put(UserTaskUpdateError.ERROR_DURING_OPERATION, UserTaskUpdateError.ERROR_DURING_OPERATION_EXPLANATION);
        allErrors.put(UserTaskUpdateError.ERROR_NO_CAMUNDA_CLIENT, UserTaskUpdateError.ERROR_NO_CAMUNDA_CLIENT_EXPLANATION);
        allErrors.put(UserTaskUpdateError.ERROR_NO_TARGET_USERTASK, UserTaskUpdateError.ERROR_NO_TARGET_USERTASK_EXPLANATION);
        allErrors.put(UserTaskUpdateError.CANT_UPDATE_USERTASK, UserTaskUpdateError.CANT_UPDATE_USERTASK_EXPLANATION);
        allErrors.put(UserTaskUpdateError.ERROR_PARSE_DURATION, UserTaskUpdateError.ERROR_PARSE_DURATION_EXPLANATION);
        return allErrors;
    }

    @Override
    public Class<?> getInputParameterClass() {
        return UserTaskUpdateInput.class;
    }

    @Override
    public Class<?> getOutputParameterClass() {
        return UserTaskUpdateOutput.class;
    }

    @Override
    public List<String> getAppliesTo() {
        return null;
    }

    @Override
    public String getElementType() {
        return null;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getRelease() {
        return "1.0.0";
    }
}
