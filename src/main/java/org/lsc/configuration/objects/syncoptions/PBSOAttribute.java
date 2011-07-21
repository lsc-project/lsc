package org.lsc.configuration.objects.syncoptions;

import java.util.ArrayList;
import java.util.List;

import org.lsc.beans.syncoptions.ISyncOptions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("attribute")
public class PBSOAttribute {
	
	private String name;
	
	private ISyncOptions.STATUS_TYPE policy;
	
	private List<String> defaultValues;
	
	private List<String> forceValues;
	
	private List<String> createValues;
	
	private String delimiter;
	
	public PBSOAttribute() {
		defaultValues = new ArrayList<String>();
		createValues = new ArrayList<String>();
		forceValues = new ArrayList<String>();
		policy = ISyncOptions.STATUS_TYPE.UNKNOWN;
	}

	public PBSOAttribute(String name) {
		setName(name);
		defaultValues = new ArrayList<String>();
		createValues = new ArrayList<String>();
		forceValues = new ArrayList<String>();
		policy = ISyncOptions.STATUS_TYPE.UNKNOWN;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ISyncOptions.STATUS_TYPE getPolicy() {
		return policy;
	}

	public void setPolicy(ISyncOptions.STATUS_TYPE policy) {
		this.policy = policy;
	}
	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public List<String> getDefaultValues() {
		return defaultValues;
	}

	public void setDefaultValues(List<String> defaultValues) {
		this.defaultValues = defaultValues;
	}

	public List<String> getForceValues() {
		return forceValues;
	}

	public void setForceValues(List<String> forceValues) {
		this.forceValues = forceValues;
	}

	public List<String> getCreateValues() {
		return createValues;
	}

	public void setCreateValues(List<String> createValues) {
		this.createValues = createValues;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("PBSOAttribute name=").append(name);
		sb.append(", delimiter='").append(this.getDelimiter()).append("'");
		sb.append(", policy=").append(this.getPolicy());
		return sb.append("\n").toString();
	}
}
