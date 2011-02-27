package com.telenor.jira.plugin.workflow.createsubissue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.swing.text.DefaultEditorKit.CutAction;

import org.apache.log4j.Logger;

import com.atlassian.core.util.InvalidDurationException;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.cache.CacheManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.util.JiraDurationUtils;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.user.User;
import com.opensymphony.workflow.WorkflowException;

public class CreateSubIssueFunction extends AbstractJiraFunctionProvider {
	
	private static final Logger log = Logger.getLogger(CreateSubIssueFunction.class);
	
	private final CustomFieldManager customFieldManager;
	private final SubTaskManager subTaskManager;
	private final IssueManager issueManager;
	private final IssueFactory issueFactory;
	private final JiraAuthenticationContext authenticationContext;
	private final ApplicationProperties applicationProperties;
	private final I18nHelper.BeanFactory i18nBeanFactory;
	private final EventPublisher eventPublisher;
	
	
	public CreateSubIssueFunction(CustomFieldManager customFieldManager, SubTaskManager subTaskManager, 
			IssueManager issueManager, IssueFactory issueFactory, JiraAuthenticationContext authenticationContext,
			ApplicationProperties applicationProperties, I18nHelper.BeanFactory i18nBeanFactory, EventPublisher eventPublisher) {
		this.customFieldManager = customFieldManager; 
		this.subTaskManager = subTaskManager;
		this.issueManager = issueManager;
		this.issueFactory = issueFactory;
		this.authenticationContext = authenticationContext;
		this.applicationProperties = applicationProperties;
		this.i18nBeanFactory = i18nBeanFactory;
		this.eventPublisher = eventPublisher;
	}
	
	
	private void createSubTask(Issue parentIssue, Map args) {
		int priority;
		MutableIssue issueObject = issueFactory.getIssue();
		issueObject.setProjectId(parentIssue.getProjectObject().getId());
		issueObject.setIssueTypeId((String)args.get("field.subIssueTypeId"));
		
		// issueObject.setSummary((String)args.get("field.subIssueOverview"));
		priority = Integer.parseInt((String) args.get("field.subIssuePriorityId"));
		if (priority == 0) {
			issueObject.setPriority(parentIssue.getPriority());
		} else {
			issueObject.setPriorityId((String)args.get("field.subIssuePriorityId"));
		}
		issueObject.setDescription((String)args.get("field.subIssueDescription"));
		issueObject.setComponents(parentIssue.getComponents());

		/*
		 * Versions
		 */
		Boolean subIssueInheritVersion = Boolean.parseBoolean((String) args.get("field.subIssueInheritVersion"));
		if (subIssueInheritVersion) {
			issueObject.setAffectedVersions(parentIssue.getAffectedVersions());
			issueObject.setFixVersions(parentIssue.getFixVersions());
		}

		/*
		 * Assignee and Reporter
		 */
		
		AssignMode assignTo = AssignMode.valueOf((String)args.get("field.subIssueAssignTo"));
		switch (assignTo) {
		case ASSIGN_TO_REPORTER: 
			issueObject.setReporter(parentIssue.getAssignee());
			issueObject.setAssignee(parentIssue.getReporter());
			break;
			
		case ASSIGN_TO_ASSIGNEE: 
			issueObject.setReporter(parentIssue.getReporter());
			issueObject.setAssignee(parentIssue.getAssignee());
			break;

		case ASSIGN_TO_PROJECTLEAD:
			issueObject.setReporter(parentIssue.getReporter());
			issueObject.setAssignee(parentIssue.getProjectObject().getLead());
			break;

		case ASSIGN_TO_UNASSIGNED:
			issueObject.setReporter(parentIssue.getReporter());
			break;

		case ASSIGN_TO_USER_FIELD:

			String customAssigneeId = (String) args.get("field.subIssueAssigneeField");

			if (customAssigneeId == null || customAssigneeId.length() == 0)
				throw new IllegalArgumentException("Function has not been configured propertly");

			CustomField selectedAssigneeField = customFieldManager.getCustomFieldObject(customAssigneeId);
			User selectedAssigneeValue = (User) parentIssue.getCustomFieldValue(selectedAssigneeField);

			issueObject.setReporter(parentIssue.getReporter());
			issueObject.setAssignee(selectedAssigneeValue);
			break;
		}

		/*
		 * Estimated Time
		 */

		String customEstimateId = (String) args.get("field.subIssueEstimateField");

		if (customEstimateId != null && customEstimateId.length() > 0) {

			CustomField selectedEstimateField = customFieldManager.getCustomFieldObject(customEstimateId);
			String selectedFieldValue = (String) parentIssue.getCustomFieldValue(selectedEstimateField);

			// selectedFieldValue in timetracking format
			JiraDurationUtils durationUtils = new JiraDurationUtils(applicationProperties, authenticationContext,
					new TimeTrackingConfiguration.PropertiesAdaptor(applicationProperties), eventPublisher, 
					i18nBeanFactory);

			try {
				// estimate string -> seconds
				Long originalEstimate = durationUtils.parseDuration(selectedFieldValue);
				issueObject.setOriginalEstimate(originalEstimate);
				issueObject.setEstimate(originalEstimate);
			} catch (InvalidDurationException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		}

		/*
		 * Summary
		 */

		String summaryInput = "";
		String summary = "";
		Object summaryObj = args.get("field.subIssueOverview");

		if (summaryObj != null)
			summaryInput = summaryObj.toString();
		String customFieldName = "", internalFieldName = "";
		int index1, index2;
		index1 = index2 = -1;
		index1 = summaryInput.indexOf("%");
		if (index1 != -1) {
			index2 = summaryInput.indexOf("%", index1 + 1);
			summary = summaryInput.substring(0, index1);
		} else
			summary = summaryInput;
		if (index2 != -1) {
			customFieldName = summaryInput.substring(index1 + 1, index2);
			CustomField customField = customFieldManager.getCustomFieldObjectByName(customFieldName);
			if (customField != null)
				summary += "[" + parentIssue.getCustomFieldValue(customField) + "]:";

		}
		while (index1 != -1 && index2 != -1) {
			index1 = summaryInput.indexOf("%", index2 + 1);
			if (index1 != -1) {
				index2 = summaryInput.indexOf("%", index1 + 1);
				if (index2 != -1) {
					customFieldName = summaryInput.substring(index1 + 1, index2);
					CustomField customField = customFieldManager.getCustomFieldObjectByName(customFieldName);
					if (customField != null)
						summary += parentIssue.getCustomFieldValue(customField);

				}
			}
		}
		index1 = index2 = 0;
		Method method = null;
		String methodName = "";
		try {
			while (index1 != -1 && index2 != -1) {
				index1 = summaryInput.indexOf("%.", index2);
				if (index1 != -1) {
					index2 = summaryInput.indexOf("%", index1 + 1);
					if (index2 != -1) {
						internalFieldName = summaryInput.substring(index1 + 2, index2);
						internalFieldName = internalFieldName.toLowerCase();
						char[] c = new char[1];
						c[0] = internalFieldName.charAt(0);
						methodName = "get" + (new String(c)).toUpperCase() + internalFieldName.substring(1);
						method = parentIssue.getClass().getMethod(methodName, new Class[] {});
						summary += method.invoke(parentIssue, (Object[]) null);
					}
				}

			}

		} catch (NoSuchMethodException e) {
			log.error("Cannnot find method", e);
		} catch (InvocationTargetException e) {
			log.error("InvocationTargetException", e);
		} catch (IllegalAccessException e) {
			log.error("IllegalAccessException", e);
		}

		issueObject.setSummary(summary);
		Map<String, Object> params = MapBuilder.build("issue", (Object)issueObject);
		
		try {
			org.ofbiz.core.entity.GenericValue subTask = issueManager.createIssue(authenticationContext.getUser(),
					params);
			subTaskManager.createSubTaskIssueLink(parentIssue.getGenericValue(), subTask, authenticationContext
					.getUser());
			ImportUtils.setIndexIssues(true);

			ManagerFactory.getCacheManager().flush(CacheManager.ISSUE_CACHE, subTask);  // Takes
																						// GenericValue
																						// and
																						// MutableIssue
																						// both
																						// as
																						// paremater;
			ComponentManager.getInstance().getIndexManager().reIndex(subTask);
			// Now, flip it back off
			ImportUtils.setIndexIssues(false);
		} catch (CreateException e) {
			log.error("Could not create sub-task", e);
		} catch (IndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		Issue parentIssue = getIssue(transientVars);
		
		// explore the field.subIssueExistsCondition

		Boolean subIssueExistsCondition = Boolean.parseBoolean((String) args.get("field.subIssueExistsCondition"));
		String issueType = (String) args.get("field.subIssueTypeId");

		if (subIssueExistsCondition && parentIssue.getSubTaskObjects() != null) {
			// create only if no sub-task of the same type exists
			for (Issue singletask : parentIssue.getSubTaskObjects()) {

				if (singletask.getIssueTypeObject().getId().equals(issueType)) {
					log.info("Another subtask of the same type exists. No subtask are created.");
					return;
				}
			}
		}

		// no condition or condition verified
		createSubTask(parentIssue, args);

	}

	public enum AssignMode {
		ASSIGN_TO_REPORTER("parent issue's reporter"), 
		ASSIGN_TO_ASSIGNEE("parent issue's assignee"), 
		ASSIGN_TO_PROJECTLEAD("project lead"), 
		ASSIGN_TO_UNASSIGNED("*unassigned*"), 
		ASSIGN_TO_USER_FIELD("user field");
		
		private final String text;
		
		private AssignMode(String text) {
			this.text = text;
		}
		
		public String getText() {
			return text;
		}	
	}
	
}
