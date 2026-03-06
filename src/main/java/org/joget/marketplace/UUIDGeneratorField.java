package org.joget.marketplace;

import java.util.Map;
import java.util.UUID;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBuilderPalette;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.ResourceBundleUtil;

public class UUIDGeneratorField extends Element implements FormBuilderPaletteElement {

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "uuidReferenceGeneratorField.ftl";
        String value = FormUtil.getElementPropertyValue(this, formData);

        dataModel.put("value", value);

        Object validatorProp = getProperty("validator");
        if (validatorProp instanceof Map<?, ?> validatorMap) {
            Object propertiesObj = validatorMap.get("properties");
            if (propertiesObj instanceof Map<?, ?> properties) {
                dataModel.put("validator", properties.get("type"));
            }
        }

        return FormUtil.generateElementHtml(this, formData, template, dataModel);
    }

    @Override
    public FormRowSet formatData(FormData formData) {
        FormRowSet rowSet = null;
        String id = getPropertyString(FormUtil.PROPERTY_ID);
        if (id != null) {
            String value = FormUtil.getElementPropertyValue(this, formData);
            if ((value == null || value.trim().isEmpty()) && !FormUtil.isReadonly(this, formData)) {

                int maxAttempts = 20;
                for (int i = 0; i < maxAttempts; i++) {
                    value = getGeneratedValue(formData);
                    formData.addRequestParameterValues(id, new String[]{value});
                    if (FormUtil.executeValidators(this, formData)) {
                        break;
                    }
                }

                String paramName = FormUtil.getElementParameterName(this);
                formData.addRequestParameterValues(paramName, new String[]{value});
            }
            if (value != null) {
                FormRow result = new FormRow();
                result.setProperty(id, value);
                rowSet = new FormRowSet();
                rowSet.add(result);
            }
        }
        return rowSet;
    }

    protected String getGeneratedValue(FormData formData) {
        String value = "";
        if (formData != null) {
            try {
                value = FormUtil.getElementPropertyValue(this, formData);
                if (value == null || value.trim().isEmpty()) {
                    value = generateUUID();
                }
            } catch (Exception e) {
                LogUtil.error(UUIDGeneratorField.class.getName(), e, "");
            }
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
            LogUtil.warn(UUIDGeneratorField.class.getName(), "Invalid length configuration, using default 36");
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

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getName() {
        return "UUID Generator Field";
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
    public String getFormBuilderCategory() {
        return FormBuilderPalette.CATEGORY_CUSTOM;
    }

    @Override
    public int getFormBuilderPosition() {
        return 200;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i><span>" + ResourceBundleUtil.getMessage("org.joget.marketplace.UUIDGeneratorField.pluginLabel") + "</span></i>";
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>" + ResourceBundleUtil.getMessage("org.joget.marketplace.UUIDGeneratorField.pluginLabel") + "</label><span></span>";
    }

    @Override
    public String getLabel() {
        return "UUID Generator Field";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/form/uuidGeneratorField.json", null, true, "message/form/UUIDGeneratorField");
    }
}
