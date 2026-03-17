<#if includeMetaData>
    <div class="form-cell" ${elementMetaData!}>
        <#if element.properties.hidden! != 'true'>
            <label class="label">${element.properties.label}</label>
            <span>@@form.uuidgeneratorfield.auto@@</span>
        </#if>
    </div>
<#else>
    <#if element.properties.hidden! == 'true'>
        <#if value??><input id="${elementParamName!}" name="${elementParamName!}" type="hidden" value="${value!?html}" /></#if>
    <#else>
        <div class="form-cell" ${elementMetaData!}>
            <label field-tooltip="${elementParamName!}" class="label">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
            <span>${value!?html}<#if !value?? || value == ''>@@form.uuidgeneratorfield.auto@@</#if></span>
            <#if value??><input id="${elementParamName!}" name="${elementParamName!}" type="hidden" value="${value!?html}" /></#if>
        </div>
    </#if>
</#if>
