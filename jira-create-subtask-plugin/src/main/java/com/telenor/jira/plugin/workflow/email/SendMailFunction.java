// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   SendMailFunction.java

package com.telenor.jira.plugin.workflow.email;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;


import com.atlassian.core.user.UserUtils;

import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.mail.DummyUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.user.EntityNotFoundException;
import com.opensymphony.user.Group;
import com.opensymphony.user.User;
import com.opensymphony.workflow.WorkflowException;


// Referenced classes of package com.telenor.jira.plugin.workflow.email:
//            UserMailQueueItem

public class SendMailFunction extends AbstractJiraFunctionProvider {

	private static final Integer PADSIZE = new Integer(20);
	private final UserManager userManager;
	private final ConstantsManager constantsManager;
	
	public SendMailFunction(UserManager userManager, ConstantsManager constantsManager) {
		this.userManager = userManager;
		this.constantsManager = constantsManager;
	}

	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		Issue issue = getIssue(args);
		// GenericValue issue = ii.getGenericValue();
		try {
			UserMailQueueItem item = new UserMailQueueItem(getEmails(issue, args), computeSubject(issue, args),
					"sendmail-function-mail.vm", createParamsMap(issue, transientVars));
			item.send();
		} catch (Exception e) {
			throw new WorkflowException("Failed to send email", e);
		}
	}

	private Map createParamsMap(Issue issue, Map transientVars) throws EntityNotFoundException {
		Map<String,Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put("issue", issue);
		try {
			paramsMap.put("assignee", issue.getString("assignee") == null ? null : ((Object) (UserUtils.getUser(issue
					.getString("assignee")))));
		} catch (EntityNotFoundException e) {
			paramsMap.put("assignee", new DummyUser(issue.getString("assignee")));
		}
		try {
			paramsMap.put("reporter", issue.getString("reporter") == null ? null : ((Object) (UserUtils.getUser(issue
					.getString("reporter")))));
		} catch (EntityNotFoundException e) {
			paramsMap.put("reporter", new DummyUser(issue.getString("reporter")));
		}
		paramsMap.put("comment", (String) transientVars.get("comment"));

		// this are added due to difference in versions.
		I18nHelper i18nBean = new I18nBean((UserUtils.getUser(issue.getString("assignee"))));
		paramsMap.put("i18n", i18nBean);
		paramsMap.put("padSize", PADSIZE);
		paramsMap.put("stringUtils", new StringUtils());

		return paramsMap;
	}

	private String computeSubject(Issue issue, Map args) {
		StringBuffer subject = new StringBuffer();
		IssueConstant issueStatus = constantsManager.getConstantObject("Status", issue.getString("status"));
		subject.append(issueStatus.getName());
		subject.append(": (");
		subject.append((String) issue.getKey());// ("key"));
		subject.append(")");
		subject.append(" ");
		subject.append((String) issue.getSummary());// ("summary"));
		return subject.toString();
	}

	private String getEmails(Issue issue, Map args) throws EntityNotFoundException {
		StringBuffer emails = new StringBuffer();
		User reporter = userManager.getUser(issue.getReporterId());// ("reporter"));
		emails.append(reporter.getEmail());
		emails.append(',');
		User assignee = userManager.getUser(issue.getAssigneeId());// ("assignee"));
		emails.append(assignee.getEmail());
		String groupEmails = (String) args.get("field.groupEmails");
		String groupToken;
		for (StringTokenizer groupTokens = new StringTokenizer(groupEmails, ","); groupTokens.hasMoreTokens(); emails
				.append(getEmailsFromGroup(groupToken))) {
			groupToken = groupTokens.nextToken();
			emails.append(',');
		}

		String individualEmails = (String) args.get("field.individualEmails");
		User user;
		for (StringTokenizer tokens = new StringTokenizer(individualEmails, ", "); tokens.hasMoreTokens(); emails
				.append(user.getEmail())) {
			String token = tokens.nextToken();
			emails.append(',');
			user = userManager.getUser(token);
		}

		return emails.toString();
	}

	private String getEmailsFromGroup(String groupName) throws EntityNotFoundException {
		StringBuffer emails = new StringBuffer();
		Group group = userManager.getGroup(groupName);
		for (Iterator<String> users = group.getUsers().iterator(); users.hasNext();) {
			User user = userManager.getUser(users.next());
			emails.append(user.getEmail());
			if (users.hasNext())
				emails.append(',');
		}

		return emails.toString();
	}
}
