package org.camunda.connector.usertaskupdate.toolbox;

public class UserTaskUpdateError {

    public static final String ERROR_BAD_INPUTPARAMETER = "BAD_INPUTPARAMETER";
    public static final String ERROR_BAD_INPUTPARAMETER_EXPLANATION = "During the bind, some input does not have the expected type";

    public static final String ERROR_NO_PROCESSINSTANCE = "ERROR_NO_PROCESSINSTANCE";
    public static final String ERROR_NO_PROCESSINSTANCE_EXPLANATION = "The process instance key can't be found in the job context";

    public static final String ERROR_DURING_OPERATION = "ERROR_DURING_OPERATION";
    public static final String ERROR_DURING_OPERATION_EXPLANATION = "Error during the search of user tasks, groups or users";

    public static final String ERROR_NO_CAMUNDA_CLIENT = "ERROR_NO_CAMUNDA_CLIENT";
    public static final String ERROR_NO_CAMUNDA_CLIENT_EXPLANATION = "No CamundaClient is available to search user tasks, groups or users";

    public static final String ERROR_PARSE_DURATION = "ERROR_PARSE_DURATION";
    public static final String ERROR_PARSE_DURATION_EXPLANATION = "Duration parse failed";

    public static final String ERROR_NO_TARGET_USERTASK = "ERROR_NO_TARGET_USERTASK";
    public static final String ERROR_NO_TARGET_USERTASK_EXPLANATION = "No user task can be resolved from userTaskKey/processInstanceKey/filterTaskElementId";

    public static final String CANT_UPDATE_USERTASK = "CANT_UPDATE_USERTASK";
    public static final String CANT_UPDATE_USERTASK_EXPLANATION = "Error during the update of a user task (priority, due date, follow-up date, assignment, candidate groups/users, action)";

    private UserTaskUpdateError() {
    }
}
