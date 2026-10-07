# User task update function

Update one or several user tasks: priority, due date, follow-up date, assignment (assignee/unassign), candidate groups, candidate users, and a free-text action label.


### Inputs
| Name                      | Description                    | Class             | Level    |
|---------------------------|--------------------------------|-------------------|----------|
| userTaskKey               | User task key(s)               | java.lang.Object  | OPTIONAL |
| processInstanceKey        | Process instance key           | java.lang.Long    | OPTIONAL |
| filterTaskElementId       | Filter task element id(s)      | java.lang.Object  | OPTIONAL |
| priorityOperation         | Priority: operation            | java.lang.String  | OPTIONAL |
| priorityStep              | Priority: step                 | java.lang.Integer | REQUIRED |
| priorityAbsoluteValue     | Priority: absolute value       | java.lang.Integer | REQUIRED |
| priorityLevel             | Priority: level                | java.lang.String  | REQUIRED |
| dueDateOperation          | Due date: operation            | java.lang.String  | OPTIONAL |
| dueDateStep               | Due date: step                 | java.lang.String  | REQUIRED |
| dueDateAbsoluteValue      | Due date: absolute value       | java.lang.String  | REQUIRED |
| followUpDateOperation     | Follow-up date: operation      | java.lang.String  | OPTIONAL |
| followUpDateStep          | Follow-up date: step           | java.lang.String  | REQUIRED |
| followUpDateAbsoluteValue | Follow-up date: absolute value | java.lang.String  | REQUIRED |
| assigneeOperation         | Assignment: operation          | java.lang.String  | OPTIONAL |
| assignee                  | Assignee                       | java.lang.String  | REQUIRED |
| candidateGroupsOperation  | Candidate groups: operation    | java.lang.String  | OPTIONAL |
| candidateGroupsValue      | Candidate groups: value        | java.lang.Object  | REQUIRED |
| candidateUsersOperation   | Candidate users: operation     | java.lang.String  | OPTIONAL |
| candidateUsersValue       | Candidate users: value         | java.lang.Object  | REQUIRED |



### Outputs
| Name             | Description        | Class          | Level    |
|------------------|--------------------|----------------|----------|
| updatedUserTasks | Updated user tasks | java.util.List | OPTIONAL |



### Errors
| Name                     | Explanation                                                                                                             |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------|
| CANT_UPDATE_USERTASK     | Error during the update of a user task (priority, due date, follow-up date, assignment, candidate groups/users, action) |
| ERROR_NO_CAMUNDA_CLIENT  | No CamundaClient is available to search user tasks, groups or users                                                     |
| ERROR_DURING_OPERATION   | Error during the search of user tasks, groups or users                                                                  |
| ERROR_NO_TARGET_USERTASK | No user task can be resolved from userTaskKey/processInstanceKey/filterTaskElementId                                    |
| ERROR_PARSE_DURATION     | Duration parse failed                                                                                                   |
| BAD_INPUTPARAMETER       | During the bind, some input does not have the expected type                                                             |
| ERROR_NO_PROCESSINSTANCE | The process instance key can't be found in the job context                                                              |

