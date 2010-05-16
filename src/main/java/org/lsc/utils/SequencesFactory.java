package org.lsc.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.jndi.JndiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to manage numeric sequences
 * via LDAP Directory entries storage
 * @author Sebastien Bahloul &gt;sbahloul@linagora.com&lt;
 */
public class SequencesFactory {

	/** the factory instance */
	private static SequencesFactory instance;
	/** the sequences cache */
	private Map<String, Sequence> sequences;
	/** the local Log4J logger */
	private static Logger LOGGER = LoggerFactory.getLogger(SequencesFactory.class);

	/**
	 * The local constructor
	 */
	private SequencesFactory() {
		sequences = new HashMap<String, Sequence>();
	}

	/**
	 * Get the factory instance (if needed create and initialize it)
	 * @return the instance
	 */
	public static SequencesFactory getInstance() {
		if (instance == null) {
			LOGGER.info("Initializing the sequences factory.");
			instance = new SequencesFactory();
		}
		return instance;
	}

	/**
	 * Get the next value for this sequence
	 * @param attributeName the sequence name
	 * @return the next value, a negative value means an error
	 */
	public static int getNextValue(String dn, String attributeName) {
		String hash = getHash(dn, attributeName);
		LOGGER.debug("Getting the next value for the following sequence {}", hash);

		Sequence sq = getInstance().getSequence(dn, attributeName, hash);
		if (sq != null) {
			return sq.getNextValue();
		} else {
			LOGGER.debug("Couldn't get the sequence {}. Returning 0.", attributeName);
			return -1;
		}
	}

	/**
	 * Get the next value for this sequence
	 * @param sequenceName the sequence name
	 * @return the next value, a negative value means an error
	 */
	public static int getActualValue(String dn, String attributeName) {
		String hash = getHash(dn, attributeName);
		LOGGER.debug("Getting the actual value for the following sequence {}", hash);

		Sequence sq = getInstance().getSequence(dn, attributeName, hash);
		if (sq != null) {
			return sq.getActualValue();
		} else {
			return -1;
		}
	}

	/**
	 * Private local method to get the next value
	 * @param sequenceName the sequence name
	 * @return the next value
	 */
	private Sequence getSequence(String dn, String attributeName, String hash) {
		if (sequences.containsKey(hash)) {
			return (Sequence) sequences.get(hash);
		} else {
			try {
				Sequence seq = new Sequence();
				seq.load(dn, attributeName, 0);
				sequences.put(hash, seq);
				return seq;
			} catch (NamingException ne) {
				LOGGER.error("Unable to load sequence");
				LOGGER.debug(ne.toString(), ne);
			}
		}
		return null;
	}

	private static String getHash(String dn, String attributeName) {
		if (dn == null || attributeName == null) {
			return null;
		}
		return attributeName + "/" + dn;
	}
}

class Sequence {

	private static Logger LOGGER = LoggerFactory.getLogger(Sequence.class);

	/** The entry distinguish name */
	private String dn;
	/** The attribute name */
	private String attributeName;
	/** The value */
	private int value;

	public boolean load(String dn, String attributeName, int serialNumber) throws NamingException {
		if (attributeName == null || dn == null || dn.indexOf('=') == -1) {
			return false;
		}
		setAttributeName(attributeName);
		setDn(dn);

		SearchResult sr = JndiServices.getDstInstance().readEntry(dn, false);
		if (sr.getAttributes().get(attributeName) != null && sr.getAttributes().get(attributeName).size() > 0) {
			value = Integer.parseInt((String) sr.getAttributes().get(attributeName).get());
		} else {
			return false;
		}
		return true;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getAttributeName() {
		return attributeName;
	}

	/**
	 * The setter
	 * @param value
	 */
	private void setDn(String value) {
		dn = value;
	}

	public int getActualValue() {
		return value;
	}

	/**
	 * Return the updated in directory new value
	 * @return
	 */
	public synchronized int getNextValue() {
		int actualValue = 0;
		int newValue = 0;
		try {
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.OBJECT_SCOPE);
			sc.setReturningAttributes(new String[]{attributeName});
			SearchResult sr = JndiServices.getDstInstance().readEntry(getDn(), "objectclass=*", false, sc);
			if (sr.getAttributes().get(attributeName) != null) {
				actualValue = Integer.parseInt((String) sr.getAttributes().get(attributeName).get());
			} else {
				LOGGER.error("Failed to get the current value for the sequence {}/{}", dn, attributeName);
				return 0;
			}
			newValue = actualValue + 1;
			Attribute newValueAttribute = new BasicAttribute(attributeName);
			Attributes newValueAttributes = new BasicAttributes();
			newValueAttributes.put(newValueAttribute);
			newValueAttribute.clear();
			newValueAttribute.add("" + newValue);

			// prepare modifications to be written to the directory
			JndiModifications jm = new JndiModifications(JndiModificationType.MODIFY_ENTRY);
			jm.setDistinguishName(getDn());
			List<ModificationItem> mi = new ArrayList<ModificationItem>();
			mi.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, newValueAttribute));
			jm.setModificationItems(mi);

			JndiServices.getDstInstance().apply(jm);
		} catch (NamingException e) {
			LOGGER.error("Failed to get the current value for the sequence {}/{}", dn, attributeName);
			return -1;
		}

		value = newValue;
		return newValue;
	}

	/**
	 * The getter accessor the sequence name. This is normally
	 * composed of "attribute-objectclass" 
	 * @return the sequence name
	 */
	public String getDn() {
		return dn;
	}
}
