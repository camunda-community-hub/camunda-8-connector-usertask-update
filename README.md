[![Community badge: Stable](https://img.shields.io/badge/Lifecycle-Stable-brightgreen)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#stable-)
[![Community extension badge](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)
![Compatible with: Camunda Platform 8](https://img.shields.io/badge/Compatible%20with-Camunda%20Platform%208-0072Ce)

# camunda-8-connector-usertask-update

![UserTaskUpdate.png](UserTaskUpdate.png)

This connector updates one or several user tasks of a process instance: **priority**, **due date**, **follow-up date**, **assignment** (assignee or unassign), **candidate groups**, and **candidate users**.

Every one of these six areas is driven by its own `*Operation` selector (`priorityOperation`, `dueDateOperation`, `followUpDateOperation`, `assigneeOperation`, `candidateGroupsOperation`, `candidateUsersOperation`), so a single connector call can touch any combination of them at once — leaving the rest of the task untouched.

## Table of contents

* [Concepts](#concepts)
* [Priority](#priority)
* [Due date and follow-up date](#due-date-and-follow-up-date)
* [Assignment](#assignment)
* [Candidate groups and candidate users](#candidate-groups-and-candidate-users)
* [Which user task(s) are updated](#which-user-tasks-are-updated)
* [Output](#output)
* [Use the connector](#use-the-connector)
* [How to install this connector?](#how-to-install-this-connector)

---

## Concepts

Each of the six areas below (priority, due date, follow-up date, assignment, candidate groups, candidate users) follows the same pattern:

* An **operation** parameter (e.g. `priorityOperation`) selects what to do. It always defaults to **`NOTHING`**, which leaves that area of the task completely untouched — so a single connector call only needs to set the operations it actually cares about.
* One or more **value** parameters become visible (and are mandatory) once an operation that needs them is selected — for example, `priorityAbsoluteValue` only appears when `priorityOperation=ABSOLUTE`.
* Operations that read the task's *current* value (`ASSIGN`, `ADD`, `REMOVE`, `INCREASE`, `DECREASE`) fetch it live from the user task before deciding what to do — they are never based on the connector's input alone.

If every operation is left as `NOTHING` (or omitted), the connector resolves the target user task(s) but doesn't change anything.

---

## Priority

`priorityOperation` accepts:

| Value       | Effect                                                                                     |
|-------------|----------------------------------------------------------------------------------------------|
| `NOTHING`   | (default) leave the priority untouched                                                       |
| `INCREASE`  | add `priorityStep` to the current priority                                                   |
| `DECREASE`  | subtract `priorityStep` from the current priority                                            |
| `ABSOLUTE`  | set the priority to `priorityAbsoluteValue`                                                   |
| `SETLEVEL`  | set the priority from `priorityLevel` (see table below)                                       |
| `CLEAR`     | reset the priority to its BPMN default, **50**                                               |

Priority is an integer in `[0, 100]`; `INCREASE`/`DECREASE`/`ABSOLUTE` results are clamped to that range. There is no "clear priority" command in the Camunda API, so `CLEAR` sets it back to 50 — the default a user task gets when no `zeebe:priorityDefinition` is configured at all.

`SETLEVEL` maps `priorityLevel` to the exact value Tasklist's own UI uses to label a priority (see [Defining task priorities](https://docs.camunda.io/docs/components/tasklist/userguide/defining-task-priorities/)):

| `priorityLevel`  | Priority value | Tasklist band      |
|-------------------|-----------------|----------------------|
| `LOW`             | 25              | 0–25 → Low           |
| `MEDIUM`          | 50              | 26–50 → Medium       |
| `HIGH` (default)  | 75              | 51–75 → High         |
| `CRITICAL`        | 100             | 76–100 → Critical    |

---

## Due date and follow-up date

`dueDateOperation` and `followUpDateOperation` work identically (one for the due date, one for the follow-up date):

| Value       | Effect                                                                                     |
|-------------|----------------------------------------------------------------------------------------------|
| `NOTHING`   | (default) leave the date untouched                                                           |
| `INCREASE`  | add `dueDateStep` / `followUpDateStep` (an ISO-8601 duration, e.g. `PT2H`, `P1D`) to the current date. If the task has no date yet, `now()` is used as the base |
| `DECREASE`  | subtract the same step from the current date                                                 |
| `ABSOLUTE`  | set the date to `dueDateAbsoluteValue` / `followUpDateAbsoluteValue` (ISO-8601 date-time, e.g. `2026-12-31T23:59:00Z`) |
| `CLEAR`     | remove the date (no due date / no follow-up date)                                            |

---

## Assignment

`assigneeOperation` accepts:

| Value         | Effect                                                                                   |
|---------------|---------------------------------------------------------------------------------------------|
| `NOTHING`     | (default) leave the assignment untouched                                                    |
| `FORCEASSIGN` | set `assignee` unconditionally, replacing any current assignee                               |
| `ASSIGN`      | same as `FORCEASSIGN`, but only if the task is **not already assigned** — does nothing otherwise |
| `CLEAR`       | unassign the task                                                                            |

`assignee` is required for `FORCEASSIGN`/`ASSIGN`, and ignored otherwise. Unlike the other areas, assignment isn't part of the single "update" API call — it uses Camunda's dedicated assign/unassign commands.

---

## Candidate groups and candidate users

`candidateGroupsOperation` and `candidateUsersOperation` work identically (one for candidate groups, one for candidate users):

| Value         | Effect                                                                                       |
|---------------|-----------------------------------------------------------------------------------------------|
| `NOTHING`     | (default) leave it untouched                                                                  |
| `FORCEASSIGN` | set it to `candidateGroupsValue` / `candidateUsersValue` unconditionally, replacing any current value |
| `ASSIGN`      | same as `FORCEASSIGN`, but only if the task currently has **none** — does nothing otherwise    |
| `CLEAR`       | remove every candidate group / candidate user                                                 |
| `ADD`         | add `candidateGroupsValue` / `candidateUsersValue` to the task's current list (deduplicated)   |
| `REMOVE`      | remove `candidateGroupsValue` / `candidateUsersValue` from the task's current list             |

`candidateGroupsValue` / `candidateUsersValue` is required for `FORCEASSIGN`/`ASSIGN`/`ADD`/`REMOVE`, and accepts either form:
* a String of ids separated by comma: `"sales, support"`
* a List of ids (FEEL): `["sales", "support"]`

---

## Which user task(s) are updated

* `userTaskKey` — a single key or a List of keys. When set, these exact user tasks are updated directly and no search is performed.
* Otherwise, the connector searches every **active** (state `CREATED`) user task of a process instance:
  * `processInstanceKey` — if not set, the current process instance (from the job context) is used.
  * `filterTaskElementId` — a task element id or a List of them, to restrict the search to specific tasks. Leave empty to update every active user task found.

---

## Output

A single output variable, `updatedUserTasks`, is produced: a List with one record per user task that was actually changed (a task where every operation resolved to `NOTHING` is not reported). Each record contains `userTaskKey`, `elementId`, `processInstanceKey`, and — for every area (priority, dueDate, followUpDate, assignee, candidateGroups, candidateUsers) — the **current value after the call**: either the task's unchanged current value (if that area's operation was `NOTHING`), or the freshly-applied new value.

```json
[
  {
    "userTaskKey": 123456,
    "elementId": "Review",
    "processInstanceKey": 789012,
    "priority": 75,
    "dueDate": "2026-12-31T23:59:00Z",
    "followUpDate": null,
    "assignee": "alice",
    "candidateGroups": ["sales"],
    "candidateUsers": []
  }
]
```

---

## Use the connector

### Inputs
| Name                      | Description                    | Class             | Level    |
|---------------------------|---------------------------------|-------------------|----------|
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

Note: a parameter marked `REQUIRED` above is only mandatory once its matching `*Operation` selects a value that needs it (see the conditions described in each section above) — it stays hidden and unused otherwise.

### Outputs
| Name             | Description        | Class          | Level    |
|------------------|---------------------|----------------|----------|
| updatedUserTasks | Updated user tasks | java.util.List | OPTIONAL |

### Errors
| Name                     | Explanation                                                                                                             |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------|
| CANT_UPDATE_USERTASK     | Error during the update of a user task (priority, due date, follow-up date, assignment, candidate groups/users, action) |
| ERROR_NO_CAMUNDA_CLIENT  | No CamundaClient is available to search user tasks, groups or users                                                     |
| ERROR_DURING_OPERATION   | Error during the search of user tasks, groups or users                                                                  |
| ERROR_NO_TARGET_USERTASK | No user task can be resolved from userTaskKey/processInstanceKey/filterTaskElementId                                    |
| ERROR_PARSE_DURATION     | Duration parse failed (dueDateStep / followUpDateStep must be an ISO-8601 duration, e.g. `PT2H`, `P1D`)                   |
| BAD_INPUTPARAMETER       | During the bind, some input does not have the expected type                                                             |
| ERROR_NO_PROCESSINSTANCE | The process instance key can't be found in the job context                                                              |

---

## How to install this connector?

### Element template

Go to the `element-templates` folder, or download directly:
[usertask-update-function.json](element-templates/usertask-update-function.json)

### JAR file

Build the project with `mvn package`. Two JARs are produced:
* `camunda-8-connector-usertask-update-<version>.jar` — the plain JAR, without embedded dependencies
  (use this if your application already provides Spring Boot and the Camunda connector runtime)
* `camunda-8-connector-usertask-update-<version>-with-dependencies.jar` — a self-contained JAR bundling
  all dependencies (use this for a standalone deployment)

This connector needs a `CamundaClient` to search and update user tasks, so it must run as a Spring bean of the connector runtime (`spring-boot-starter-camunda-connectors`), not as a bare SPI connector — the runtime autowires the `CamundaClient` bean into it.

### Integrated in your application

If you already have an application with its own worker/connector runtime — i.e. it already depends on `spring-boot-starter-camunda-connectors` and has a `CamundaClient` bean configured — you can integrate this connector directly as a Maven dependency instead of deploying a separate JAR. Just add it to your `pom.xml`:

```xml
<dependency>
    <groupId>org.camunda.connector</groupId>
    <artifactId>connector-usertask-update</artifactId>
    <version>1.0.0</version>
</dependency>
```

Spring Boot's component scanning won't automatically pick up `UserTaskUpdateFunction` just because the JAR is on the classpath — it's wired in via a Spring Boot auto-configuration (`UserTaskUpdateFunctionAutoConfiguration`, declared in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`), which Spring Boot's `@EnableAutoConfiguration` discovers automatically on any `@SpringBootApplication`. So once the dependency is on your classpath, the connector is registered and starts polling for `c-usertaskupdate-function` jobs alongside your own workers — no further configuration needed.
