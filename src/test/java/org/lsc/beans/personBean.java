/*
 * Generated - please do not edit manually
 */
package org.lsc.beans;

import java.lang.reflect.InvocationTargetException;

import javax.naming.NamingException;

import org.lsc.jndi.JndiModifications;
import org.lsc.objects.person;
import org.lsc.objects.top;

/**
 * @deprecated
 * 		This class was used in LSC 1.1 projects, and is no longer
 * 		necessary, but kept for reverse compatibility. It will be
 * 		removed in LSC 1.3.
 */
@SuppressWarnings("serial")
public class personBean extends AbstractBean implements IBean {

	public static personBean getInstance(top myclass) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NamingException {
		personBean bean = new personBean() ;
		AbstractBean.mapper(personBean.class, bean, myclass);
		bean.generateDn();
		return bean;
	}

	public personBean() {
		super();
	}

	//public static void mapSn(person soc, IBean doc, List values) throws NamingException {
		// Do nothing because it is generated through other map methods !
		//if (values != null && values.size() > 0) {
		//	Vector<String> v = new Vector<String>();
		//	Iterator valuesIter = values.iterator();
		//	while (valuesIter.hasNext()) {
		//		String value = (String) valuesIter.next();
		//		if (value != null && value.trim().length() > 0) {
		//			mapString(doc, "sn", Filters.filterString(value));
		//			generateSn(soc, doc);
		//		}
		//	}
		//}
	//}

	//public static void generateSn(person soc, IBean doc) throws NamingException {
		// to be completed
		//String value = "";
		//Attribute attr = new BasicAttribute("sn");
		//attr.add(value);
		//doc.setAttribute(attr);
	//}

	//public static void mapCn(person soc, IBean doc, List values) throws NamingException {
		// Do nothing because it is generated through other map methods !
		//if (values != null && values.size() > 0) {
		//	Vector<String> v = new Vector<String>();
		//	Iterator valuesIter = values.iterator();
		//	while (valuesIter.hasNext()) {
		//		String value = (String) valuesIter.next();
		//		if (value != null && value.trim().length() > 0) {
		//			mapString(doc, "cn", Filters.filterString(value));
		//			generateCn(soc, doc);
		//		}
		//	}
		//}
	//}

	//public static void generateCn(person soc, IBean doc) throws NamingException {
		// to be completed
		//String value = "";
		//Attribute attr = new BasicAttribute("cn");
		//attr.add(value);
		//doc.setAttribute(attr);
	//}

	//public static void mapUserPassword(person soc, IBean doc, List values) throws NamingException {
		// Do nothing because it is generated through other map methods !
		//if (values != null && values.size() > 0) {
		//	Vector<String> v = new Vector<String>();
		//	Iterator valuesIter = values.iterator();
		//	while (valuesIter.hasNext()) {
		//		String value = (String) valuesIter.next();
		//		if (value != null && value.trim().length() > 0) {
		//			mapString(doc, "userPassword", Filters.filterString(value));
		//			generateUserPassword(soc, doc);
		//		}
		//	}
		//}
	//}

	//public static void generateUserPassword(person soc, IBean doc) throws NamingException {
		// to be completed
		//String value = "";
		//Attribute attr = new BasicAttribute("userPassword");
		//attr.add(value);
		//doc.setAttribute(attr);
	//}

	//public static void mapTelephoneNumber(person soc, IBean doc, List values) throws NamingException {
		// Do nothing because it is generated through other map methods !
		//if (values != null && values.size() > 0) {
		//	Vector<String> v = new Vector<String>();
		//	Iterator valuesIter = values.iterator();
		//	while (valuesIter.hasNext()) {
		//		String value = (String) valuesIter.next();
		//		if (value != null && value.trim().length() > 0) {
		//			mapString(doc, "telephoneNumber", Filters.filterString(value));
		//			generateTelephoneNumber(soc, doc);
		//		}
		//	}
		//}
	//}

	//public static void generateTelephoneNumber(person soc, IBean doc) throws NamingException {
		// to be completed
		//String value = "";
		//Attribute attr = new BasicAttribute("telephoneNumber");
		//attr.add(value);
		//doc.setAttribute(attr);
	//}

	//public static void mapSeeAlso(person soc, IBean doc, List values) throws NamingException {
		// Do nothing because it is generated through other map methods !
		//if (values != null && values.size() > 0) {
		//	Vector<String> v = new Vector<String>();
		//	Iterator valuesIter = values.iterator();
		//	while (valuesIter.hasNext()) {
		//		String value = (String) valuesIter.next();
		//		if (value != null && value.trim().length() > 0) {
		//			mapString(doc, "seeAlso", Filters.filterString(value));
		//			generateSeeAlso(soc, doc);
		//		}
		//	}
		//}
	//}

	//public static void generateSeeAlso(person soc, IBean doc) throws NamingException {
		// to be completed
		//String value = "";
		//Attribute attr = new BasicAttribute("seeAlso");
		//attr.add(value);
		//doc.setAttribute(attr);
	//}

	//public static void mapDescription(person soc, IBean doc, List values) throws NamingException {
		// Do nothing because it is generated through other map methods !
		//if (values != null && values.size() > 0) {
		//	Vector<String> v = new Vector<String>();
		//	Iterator valuesIter = values.iterator();
		//	while (valuesIter.hasNext()) {
		//		String value = (String) valuesIter.next();
		//		if (value != null && value.trim().length() > 0) {
		//			mapString(doc, "description", Filters.filterString(value));
		//			generateDescription(soc, doc);
		//		}
		//	}
		//}
	//}

	//public static void generateDescription(person soc, IBean doc) throws NamingException {
		// to be completed
		//String value = "";
		//Attribute attr = new BasicAttribute("description");
		//attr.add(value);
		//doc.setAttribute(attr);
	//}

	public JndiModifications[] checkDependenciesWithperson(person soc, JndiModifications jm) {
		// to be completed
		return new JndiModifications[] {};
	}

	//public void generateDn() throws NamingException {
		//setDistinguishName("uid=" + getAttributeById("uid").get() + "," + Configuration.DN_PEOPLE);
		//throw new UnsupportedOperationException("Complete this to make it available !");
	//}

}
