package org.lsc.configuration.objects.services;

import org.apache.tapestry5.beaneditor.Validate;
import org.lsc.jndi.PullableJndiSrcService;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("ldapSrcService")
public class SrcLdap extends Ldap {

	/**
	 * Contains the LDAP filter used to list recently updated objects 
	 * An simple example is (modifytimestamp>={0})
	 */
	@Validate("required")
	protected String filterAsync;
	
	/**
	 * Contains the string to set the date format to use to inject into filterAsync as first parameter
	 * This filter must be compatible with Java object java.text.SimpleDateFormat
	 * For example, use "yyyyMMddHHmmss'Z'"
	 */
	@Validate("required")
	protected String dateFormat;
	
	/**
	 * This value is the interval between directory search for updated objects in seconds
	 * Must be an positive integer. Default to 60
	 */
	protected String interval;
	
	@Override
	public Class<?> getImplementation() {
		return PullableJndiSrcService.class;
	}

	public String getFilterAsync() {
		return filterAsync;
	}

	public void setFilterAsync(String filterAsync) {
		this.filterAsync = filterAsync;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public String getInterval() {
		return interval;
	}

	public void setInterval(String interval) {
		this.interval = interval;
	}
}
