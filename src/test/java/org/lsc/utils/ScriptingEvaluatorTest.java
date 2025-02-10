package org.lsc.utils;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.lsc.Task;
import org.lsc.configuration.*;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.jndi.SimpleJndiSrcService;

@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(
    name = "DSWithPartitionAndServer",
    loadedSchemas =
        {
            @LoadSchema(name = "other", enabled = true)
        },
    partitions =
        {
            @CreatePartition(
                name = "lsc-project",
                suffix = "dc=lsc-project,dc=org",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=lsc-project,dc=org\n" +
                        "dc: lsc-project\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "dc"),
                        @CreateIndex(attribute = "ou")
                })
    })
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP", port = 33389),
            @CreateTransport(protocol = "LDAPS", port = 33636)
    })
@ApplyLdifs(
        {
            // Entry # 0
            "dn: cn=Directory Manager,ou=system",
            "objectClass: person",
            "objectClass: top",
            "cn: Directory Manager",
            "description: Directory Manager",
            "sn: Directory Manager",
            "userpassword: secret"
        })
@ApplyLdifFiles({"lsc-schema.ldif","lsc-project.ldif"})
public class ScriptingEvaluatorTest extends AbstractLdapTestUnit {

    Task task = mock(Task.class);

    @BeforeEach
    public void setUp() throws LscServiceConfigurationException {
        TaskType taskConf = LscConfiguration.getTask("ldap2ldapTestTask");
        when(task.getSourceService()).thenReturn(new SimpleJndiSrcService(taskConf));
    }

    @Test
    public void testString() throws LscServiceException {
        String expression = "gjs:dn='ou=test-user' + ',ou=people,dc=example,dc=com'";
        String stringOutput = ScriptingEvaluator.evalToString(task, expression, new HashMap<>());
        assertEquals("ou=test-user,ou=people,dc=example,dc=com", stringOutput);
    }

    @Test
    public void testBoolean() throws LscServiceException {
        String expression = "gjs:booleanVariable=true";
        boolean booleanOutput = ScriptingEvaluator.evalToBoolean(task, expression, new HashMap<>());
        assertTrue(booleanOutput);
    }
}