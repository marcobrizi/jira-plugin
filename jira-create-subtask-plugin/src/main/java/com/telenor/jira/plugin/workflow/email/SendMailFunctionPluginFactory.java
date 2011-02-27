// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   SendMailFunctionPluginFactory.java

package com.telenor.jira.plugin.workflow.email;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;

public class SendMailFunctionPluginFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginFunctionFactory{

	protected void getVelocityParamsForView(Map<String,Object> velocityParams, AbstractDescriptor descriptor) {
		if (!(descriptor instanceof FunctionDescriptor)) {
			throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
		} else {
			FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
			velocityParams.put("individualEmails", functionDescriptor.getArgs().get("field.individualEmails"));
			velocityParams.put("groupEmails", functionDescriptor.getArgs().get("field.groupEmails"));
			return;
		}
	}

	public Map<String,?> getDescriptorParams(Map<String,Object> conditionParams) {
		Map<String, String> params = new HashMap<String, String>();
		String groupEmails = extractSingleParam(conditionParams, "groupEmails");
		params.put("field.groupEmails", groupEmails);
		String individualEmails = extractSingleParam(conditionParams, "individualEmails");
		params.put("field.individualEmails", individualEmails);
		return params;
	}

	protected void getVelocityParamsForEdit(Map<String,Object> velocityParams, AbstractDescriptor descriptor)  {
	}

	protected void getVelocityParamsForInput(Map<String,Object> velocityParams)  {
	}
}
