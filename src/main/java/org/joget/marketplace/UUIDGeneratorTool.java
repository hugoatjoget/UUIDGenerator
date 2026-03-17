package org.joget.marketplace;

import java.util.Map;
import java.util.UUID;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;

public class UUIDGeneratorTool extends DefaultApplicationPlugin {

    private static final String MESSAGE_PATH = "message/app/UUIDGeneratorTool";

    @Override
    public String getName() {
        return "UUID Generator Tool";
    }

    @Override
    public String getVersion() {
        return "9.0.0";
    }

    @Override
    public String getDescription() {
        return "Generates a unique ID based on UUID with configurable length";
    }

    @Override
    public String getLabel() {
        return "UUID Generator Tool";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/app/uuidGeneratorTool.json", null, true, MESSAGE_PATH);
    }

    @Override
    public Object execute(Map properties) {
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            WorkflowAssignment wfAssignment = (WorkflowAssignment) properties.get("workflowAssignment");
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");

            String formDefId = (String) properties.get("formDefId");
            String fieldId = (String) properties.get("fieldId");
            String recordId = (String) properties.get("recordId");

            String tableName = null;
            if (formDefId != null && !formDefId.isEmpty()) {
                tableName = appService.getFormTableName(appDef, formDefId);
            }

            String value = getUniqueValue(formDefId, tableName, fieldId);

            if (!value.isEmpty()) {
                // Store to form
                if (fieldId != null && !fieldId.isEmpty() && formDefId != null && !formDefId.isEmpty()) {
                    if (recordId != null && !recordId.isEmpty()) {
                        recordId = AppUtil.processHashVariable(recordId, wfAssignment, null, null, appDef);
                    }

                    if ((recordId == null || recordId.isEmpty()) && wfAssignment != null) {
                        recordId = appService.getOriginProcessId(wfAssignment.getProcessId());
                    }

                    if (FormUtil.PROPERTY_ID.equals(fieldId)) {
                        recordId = value;
                    }

                    FormRowSet rowSet = new FormRowSet();
                    rowSet.setMultiRow(false);
                    FormRow row = new FormRow();
                    row.setId(recordId);
                    row.setProperty(fieldId, value);
                    rowSet.add(row);
                    appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), formDefId, rowSet, recordId);
                }

                // Store to workflow variable
                String workflowVariable = (String) properties.get("workflowVariable");
                if (workflowVariable != null && !workflowVariable.isEmpty() && wfAssignment != null) {
                    WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
                    workflowManager.activityVariable(wfAssignment.getActivityId(), workflowVariable, value);
                }
            }
        } catch (Exception e) {
            LogUtil.error(UUIDGeneratorTool.class.getName(), e, "");
        }

        return null;
    }

    protected String getUniqueValue(String formDefId, String tableName, String fieldId) {
        String value = generateUUID();
        if (formDefId == null || formDefId.isEmpty() || tableName == null || tableName.isEmpty()
                || fieldId == null || fieldId.isEmpty()) {
            return value;
        }
        try {
            FormDataDao dao = (FormDataDao) FormUtil.getApplicationContext().getBean("formDataDao");
            int maxAttempts = 20;
            for (int i = 0; i < maxAttempts; i++) {
                value = generateUUID();
                boolean isUnique;
                if (FormUtil.PROPERTY_ID.equals(fieldId)) {
                    isUnique = dao.load(formDefId, tableName, value) == null;
                } else {
                    String existingKey = dao.findPrimaryKey(formDefId, tableName, fieldId, value);
                    isUnique = existingKey == null || existingKey.trim().isEmpty();
                }
                if (isUnique) {
                    return value;
                }
            }
        } catch (Exception e) {
            LogUtil.error(UUIDGeneratorTool.class.getName(), e, "");
        }
        return value;
    }

    protected String generateUUID() {
        int length = 36;
        boolean includeDashes = true;
        boolean uppercase = false;

        try {
            String lengthStr = getPropertyString("UUIDLength");
            if (lengthStr != null && !lengthStr.trim().isEmpty()) {
                length = Math.max(1, Integer.parseInt(lengthStr.trim()));
            }
        } catch (NumberFormatException e) {
            LogUtil.warn(UUIDGeneratorTool.class.getName(), "Invalid length configuration, using default 36");
        }

        includeDashes = "true".equalsIgnoreCase(getPropertyString("includeDashes"));
        uppercase = "true".equalsIgnoreCase(getPropertyString("uppercase"));

        String result;
        if (includeDashes) {
            // Standard UUID format (max 36 chars with dashes)
            String uuid = UUID.randomUUID().toString();
            result = uuid.substring(0, Math.min(length, uuid.length()));
        } else {
            // Concatenate UUID hex chars (no dashes) to fill requested length
            StringBuilder rawChars = new StringBuilder();
            while (rawChars.length() < length) {
                rawChars.append(UUID.randomUUID().toString().replace("-", ""));
            }
            result = rawChars.substring(0, length);
        }

        return uppercase ? result.toUpperCase() : result;
    }
}
