package org.lsc.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.lsc.Task;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceException;
import org.lsc.jndi.SimpleJndiDstService;
import org.lsc.jndi.SimpleJndiSrcService;

@ExtendWith({ ApacheDSTestExtension.class })
@CreateDS(name = "DSWithPartitionAndServer", loadedSchemas = {
		@LoadSchema(name = "other", enabled = true) }, partitions = {
				@CreatePartition(name = "lsc-project", suffix = "dc=lsc-project,dc=org", contextEntry = @ContextEntry(entryLdif = "dn: dc=lsc-project,dc=org\n"
						+ "dc: lsc-project\n" + "objectClass: top\n" + "objectClass: domain\n\n"), indexes = {
								@CreateIndex(attribute = "objectClass"), @CreateIndex(attribute = "dc"),
								@CreateIndex(attribute = "ou") }) })
@CreateLdapServer(
		// allowAnonymousAccess = true,
		transports = { @CreateTransport(protocol = "LDAP", port = 33389),
				@CreateTransport(protocol = "LDAPS", port = 33636) })
@ApplyLdifs({
		// Entry # 0
		"dn: cn=Directory Manager,ou=system", "objectClass: person", "objectClass: top", "cn: Directory Manager",
		"description: Directory Manager", "sn: Directory Manager", "userpassword: secret" })
@ApplyLdifFiles({ "lsc-schema.ldif", "lsc-project.ldif" })
public class GroovyEvaluatorTest extends AbstractLdapTestUnit {

	private ScriptableEvaluator evaluator;

	Task task = mock(Task.class);

	@BeforeEach
	public void setUp() {
		evaluator = new GroovyEvaluator(new GroovyScriptEngineFactory().getScriptEngine());
	}

	@Test
	public void test1() throws LscServiceException {

		TaskType taskConf = LscConfiguration.getTask("ldap2ldapTestTask");
		when(task.getSourceService()).thenReturn(new SimpleJndiSrcService(taskConf));
		when(task.getDestinationService()).thenReturn(new SimpleJndiDstService(taskConf));

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("a", "b");
		params.put("b", "a");
		assertEquals("Hello b", evaluator.evalToString(task, "'Hello ' + a", params));
		assertEquals(Arrays.asList(new String[] { "Hello b" }),
				evaluator.evalToObjectList(task, "'Hello ' + a", params));

		params.put("a", new String[] { "b", "c" });
		assertEquals(Arrays.asList(new String[] { "Hello [b, c]" }),
				evaluator.evalToObjectList(task, "'Hello ' + a", params));

		String complexExpression = "def dataToStringEquality = { a, b -> \n" + " return a.toString() == b.toString() \n"
				+ "}\n" + "dataToStringEquality a, b";
		assertEquals(false, evaluator.evalToBoolean(task, complexExpression, params));
	}
}
