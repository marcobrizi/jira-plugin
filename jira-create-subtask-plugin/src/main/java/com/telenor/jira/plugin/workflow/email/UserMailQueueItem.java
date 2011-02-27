// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   UserMailQueueItem.java

package com.telenor.jira.plugin.workflow.email;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.mail.JiraMailQueueUtils;
import com.atlassian.mail.Email;
import com.atlassian.mail.MailException;
import com.atlassian.mail.queue.AbstractMailQueueItem;
import com.atlassian.mail.queue.SingleMailQueueItem;

public class UserMailQueueItem extends AbstractMailQueueItem {

	private static final long serialVersionUID = 1L;

	public UserMailQueueItem(String emails, String subject, String template, Map paramsMap) {
		super(subject);
		this.template = template;
		this.emails = emails;
		this.paramsMap = paramsMap;
	}

	public int getSendCount() {
		return 0;
	}

	public boolean hasError() {
		return false;
	}

	public void send() throws MailException {
		try {
			Map params = getUserContextParamsBody();
			String body = ManagerFactory.getVelocityManager().getEncodedBody("", template,
					ManagerFactory.getApplicationProperties().getString("jira.baseurl"),
					applicationProperties.getString("webwork.i18n.encoding"), params);
			Email email = new Email(emails);
			email.setSubject(getSubject());
			email.setBody(body);
			email.setMimeType("text/plain");
			email.setEncoding(applicationProperties.getString("webwork.i18n.encoding"));
			ManagerFactory.getMailQueue().addItem(new SingleMailQueueItem(email));
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new MailException(ex);
		}
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	protected Map getUserContextParamsBody() {
		return JiraMailQueueUtils.getContextParamsBody(paramsMap);
	}

	private String template;
	private String emails;
	private Map paramsMap;
	private final ApplicationProperties applicationProperties = ManagerFactory.getApplicationProperties();


}
