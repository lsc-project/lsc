package org.lsc.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.junit.Before;
import org.junit.Test;
import org.lsc.Task;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceException;
import org.lsc.jndi.SimpleJndiDstService;
import org.lsc.jndi.SimpleJndiSrcService;

import mockit.Mocked;
import mockit.NonStrictExpectations;


public class GroovyEvaluatorTest {

	private ScriptableEvaluator evaluator;
	
	@Mocked Task task;

	@Before
	public void setUp() {
		evaluator = new GroovyEvaluator(new GroovyScriptEngineFactory().getScriptEngine());
	}
	
	@Test
	public void test1() throws LscServiceException {

		new NonStrictExpectations() {
			{
				TaskType taskConf = LscConfiguration.getTask("ldap2ldapTestTask");
				task.getSourceService(); result = new SimpleJndiSrcService(taskConf);
				task.getDestinationService(); result = new SimpleJndiDstService(taskConf);
			}
		};
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("a", "b");
		params.put("b", "a");
		assertEquals("Hello b", evaluator.evalToString(task,  	"'Hello ' + a", params));
		assertEquals(Arrays.asList(new String[] {"Hello b"}), evaluator.evalToObjectList(task, "'Hello ' + a", params));
		
		params.put("a", new String[] { "b", "c" } );
		assertEquals(Arrays.asList(new String[] {"Hello [b, c]"}), evaluator.evalToObjectList(task, "'Hello ' + a", params));
		
		String complexExpression = "def dataToStringEquality = { a, b -> \n" +
		  	" return a.toString() == b.toString() \n" + 
		  	"}\n" +
		  	"dataToStringEquality a, b";
		assertEquals(false, evaluator.evalToBoolean(task, complexExpression, params));
	}
}
