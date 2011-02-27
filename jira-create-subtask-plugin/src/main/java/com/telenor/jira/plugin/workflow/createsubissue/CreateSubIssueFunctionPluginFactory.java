///*
// * Created on Jan 11, 2005
// *
// * To change the template for this generated file go to
// * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
// */
//package com.telenor.jira.plugin.workflow.createsubissue;
//
//import com.atlassian.jira.config.SubTaskManager;
//import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
//import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
//import com.opensymphony.workflow.loader.AbstractDescriptor;
//import com.opensymphony.workflow.loader.FunctionDescriptor;
//import org.ofbiz.core.entity.GenericValue;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @author t535293
// */
//public class CreateSubIssueFunctionPluginFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginFunctionFactory {
//	private SubTaskManager subTaskManager;
//
//	public CreateSubIssueFunctionPluginFactory(SubTaskManager subTaskManager) {
//		this.subTaskManager = subTaskManager;
//	}
//
//	public Map getDescriptorParams(Map conditionParams) {
//		Map params = new HashMap();
//		params.put("field.subIssueTypeId", extractSingleParam(conditionParams, "subIssueTypeId"));
//		params.put("field.subIssueOverview", extractSingleParam(conditionParams, "subIssueOverview"));
//		params.put("field.subIssueDescription", extractSingleParam(conditionParams, "subIssueDescription"));
//		params.put("field.subIssueAssignTo", extractSingleParam(conditionParams, "subIssueAssignTo"));
//		return params;
//	}
//
//	protected void getVelocityParamsForView(Map velocityParams, AbstractDescriptor descriptor) {
//		if (!(descriptor instanceof FunctionDescriptor)) {
//			throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
//		}
//		FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
//		String subIssueTypeId = (String) functionDescriptor.getArgs().get("field.subIssueTypeId");
//		if(subIssueTypeId!=null) {
//			GenericValue subTaskType = subTaskManager.getSubTaskIssueTypeById(subIssueTypeId);
//			velocityParams.put("subIssueTypeName", subTaskType.getString("name"));
//			velocityParams.put("subIssueOverview", functionDescriptor.getArgs().get("field.subIssueOverview"));
////			velocityParams.put("subIssueDescription", functionDescriptor.getArgs().get("field.subIssueDescription"));
////			velocityParams.put("subIssueAssignTo", functionDescriptor.getArgs().get("field.subIssueAssignTo"));
//			
//			int assignTo = CreateSubIssueFunction.ASSIGN_TO_REPORTER;
//			try {
//				assignTo = Integer.parseInt((String)functionDescriptor.getArgs().get("field.subIssueAssignTo"));
//			} catch(Exception e) { /* Ignored */ }
//			
//			velocityParams.put("subIssueAssignToName", CreateSubIssueFunction.ASSIGNEE[assignTo]);
//		}
//	}
//
//	protected void getVelocityParamsForInput(Map velocityParams) {
//		velocityParams.put("subIssueTypes", subTaskManager.getSubTasksIssueTypes());
//	}
//
//	protected void getVelocityParamsForEdit(Map velocityParams, AbstractDescriptor descriptor) {
//		if (!(descriptor instanceof FunctionDescriptor)) {
//			throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
//		}
//		FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
//		velocityParams.put("currentSubIssueTypeId", functionDescriptor.getArgs().get("field.subIssueTypeId"));
//		velocityParams.put("currentSubIssueOverview", functionDescriptor.getArgs().get("field.subIssueOverview"));
//		velocityParams.put("currentSubIssueDescription", functionDescriptor.getArgs().get("field.subIssueDescription"));
//		velocityParams.put("currentSubIssueAssignTo", new Integer((String)functionDescriptor.getArgs().get("field.subIssueAssignTo")));
//		velocityParams.put("subIssueTypes", subTaskManager.getSubTasksIssueTypes());		
//	}
//}
// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   CreateSubIssueFunctionPluginFactory.java

package com.telenor.jira.plugin.workflow.createsubissue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.util.Log;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.customfields.impl.TextCFType;
import com.atlassian.jira.issue.customfields.impl.UserCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;
import com.telenor.jira.plugin.workflow.createsubissue.CreateSubIssueFunction.AssignMode;

// Referenced classes of package com.telenor.jira.plugin.workflow.createsubissue:
//            CreateSubIssueFunction

public class CreateSubIssueFunctionPluginFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginFunctionFactory{

	private final CustomFieldManager customFieldManager = ComponentManager.getInstance().getCustomFieldManager();

	public CreateSubIssueFunctionPluginFactory(SubTaskManager subTaskManager, ConstantsManager constantsManager) {
		this.subTaskManager = subTaskManager;
		this.constantsManager = constantsManager;
	}

	public Map<String,?> getDescriptorParams(Map<String,Object> formParams) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("field.subIssueTypeId", extractSingleParam(formParams, "subIssueTypeId"));
		params.put("field.subIssueOverview", extractSingleParam(formParams, "subIssueOverview"));
		params.put("field.subIssueDescription", extractSingleParam(formParams, "subIssueDescription"));
		params.put("field.subIssueAssignTo", extractSingleParam(formParams, "subIssueAssignTo"));
		params.put("field.subIssuePriorityId", extractSingleParam(formParams, "subIssuePriorityId"));

		params.put("field.subIssueAssigneeField", extractSingleParam(formParams, "subIssueAssigneeField"));
		params.put("field.subIssueEstimateField", extractSingleParam(formParams, "subIssueEstimateField"));

		Boolean subIssueExistsCondition = Boolean.FALSE;
		if (formParams.containsKey("subIssueExistsCondition"))
			subIssueExistsCondition = Boolean.TRUE;
		params.put("field.subIssueExistsCondition", subIssueExistsCondition.toString());
		
		Boolean subIssueInheritVersion = Boolean.FALSE;
		if (formParams.containsKey("subIssueInheritVersion"))
			subIssueInheritVersion = Boolean.TRUE;
		params.put("field.subIssueInheritVersion", subIssueInheritVersion.toString());

		return params;
	}

	protected void getVelocityParamsForView(Map<String,Object> velocityParams, AbstractDescriptor descriptor)  {
		if (!(descriptor instanceof FunctionDescriptor))
			throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
		FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
		String subIssueTypeId = (String) functionDescriptor.getArgs().get("field.subIssueTypeId");
		if (subIssueTypeId != null) {
			GenericValue subTaskType = subTaskManager.getSubTaskIssueTypeById(subIssueTypeId);
			velocityParams.put("subIssueTypeName", subTaskType.getString("name"));
			velocityParams.put("subIssueOverview", functionDescriptor.getArgs().get("field.subIssueOverview"));
			
			AssignMode assignTo = AssignMode.ASSIGN_TO_UNASSIGNED;
			try {
				assignTo = AssignMode.valueOf((String)functionDescriptor.getArgs().get("field.subIssueAssignTo"));
			} catch (Exception e) {
				Log.error("Unable to get the issue assignee mode: " + e.getMessage());
			}
			velocityParams.put("subIssueAssignToName", assignTo.getText());
		}
	}

	protected void getVelocityParamsForInput(Map<String,Object> velocityParams)  {
		velocityParams.put("subIssueTypes", subTaskManager.getSubTaskIssueTypeObjects());
		velocityParams.put("subIssuePriorities", constantsManager.getPriorityObjects());
		velocityParams.put("subIssueAssignModes", AssignMode.values());

		// user custom fields
		List<CustomField> allCustomFields = customFieldManager.getCustomFieldObjects();
		List<CustomField> customFields = new ArrayList<CustomField>();

		for (CustomField customField : allCustomFields) {
			if (customField.getCustomFieldType() instanceof UserCFType)
				customFields.add(customField);
		}
		velocityParams.put("subIssueUserField", customFields);

		// text custom fields
		customFields = new ArrayList<CustomField>();

		for (CustomField customField : allCustomFields) {
			if (customField.getCustomFieldType() instanceof TextCFType)
				customFields.add(customField);
		}
		velocityParams.put("subIssueTextField", customFields);

	}

	protected void getVelocityParamsForEdit(Map<String,Object> velocityParams, com.opensymphony.workflow.loader.AbstractDescriptor descriptor)  {
		if (!(descriptor instanceof FunctionDescriptor)) {
			throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
		} else {
			FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
			velocityParams.put("currentSubIssueTypeId", functionDescriptor.getArgs().get("field.subIssueTypeId"));
			velocityParams.put("currentSubIssueOverview", functionDescriptor.getArgs().get("field.subIssueOverview"));
			velocityParams.put("currentSubIssueDescription", functionDescriptor.getArgs().get(
					"field.subIssueDescription"));
			velocityParams.put("currentSubIssueAssignTo", functionDescriptor.getArgs().get("field.subIssueAssignTo"));
			velocityParams.put("currentSubIssuePriorityId", functionDescriptor.getArgs()
					.get("field.subIssuePriorityId"));

			velocityParams.put("currentSubIssueAssigneeField", functionDescriptor.getArgs().get(
					"field.subIssueAssigneeField"));
			velocityParams.put("currentSubIssueEstimateField", functionDescriptor.getArgs().get(
					"field.subIssueEstimateField"));
			velocityParams.put("currentSubIssueExistsCondition", Boolean.parseBoolean((String) functionDescriptor
					.getArgs().get("field.subIssueExistsCondition")));
			velocityParams.put("currentSubIssueInheritVersion", Boolean.parseBoolean((String) functionDescriptor
					.getArgs().get("field.subIssueInheritVersion")));

			getVelocityParamsForInput(velocityParams);

			return;
		}
	}

	private SubTaskManager subTaskManager;
	private ConstantsManager constantsManager;
}
