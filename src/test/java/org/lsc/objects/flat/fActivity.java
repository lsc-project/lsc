/*
 * Generated - please do not edit
 */
package org.lsc.objects.flat;

import java.util.GregorianCalendar;

import org.lsc.utils.DateUtils;

/**
 * LDAP Activity objectClass representation
 */
@SuppressWarnings("deprecation")
public class fActivity extends fAlias {
	
	public fActivity() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.add(GregorianCalendar.YEAR, 20);
        endOfValidity = DateUtils.format(gc.getTime());
	}

	/** Monovalued attribute : uidInterne */
	protected String uid;

	/** Monovalued attribute : endOfValidity */
	protected String endOfValidity;

	/** Monovalued attribute : info */
	protected String info;

	/** Attribute use to set the DN */
	protected String ou;

	/**
	 * uid getter
	 * 
	 * @return uid value
	 */
	public String getUid() {
		return uid;
	}

	/**
	 * uid setter
	 * 
	 * @param value
	 *            uid value
	 */
	public void setUid(String value) {
		this.uid = value;
	}

	/**
	 * endOfValidity getter
	 * 
	 * @return endOfValidity value
	 */
	public String getEndOfValidity() {
		return endOfValidity;
	}

	/**
	 * endOfValidity setter
	 * 
	 * @param value
	 *            endOfValidity value
	 */
	public void setEndOfValidity(String value) {
		this.endOfValidity = value;
	}

	/**
	 * info getter
	 * 
	 * @return info value
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * info setter
	 * 
	 * @param value
	 *            info value
	 */
	public void setInfo(String value) {
		this.info = value;
	}

	public String getOu() {
		return ou;
	}

	public void setOu(String ou) {
		this.ou = ou;
	}

}
