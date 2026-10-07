package org.camunda.connector.usertaskupdate;

import io.camunda.cherry.definition.RunnerDecorationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementTemplateGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ElementTemplateGenerator.class.getName());

    public static void generate() {
        try {
            logger.info("Generating ElementTemplate");
            RunnerDecorationTemplate runnerDecorationTemplate = new RunnerDecorationTemplate(new UserTaskUpdateFunction());
            runnerDecorationTemplate.generateElementTemplate("./element-templates/", "usertask-update-function.json");
        } catch (Exception e) {
            logger.error("Error during generation", e);
        }
    }

    public static void main(String[] args) {
        generate();
    }
}
