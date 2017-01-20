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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mockit.Mocked;
import mockit.NonStrictExpectations;



public class GroovyEvaluatorTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(GroovyEvaluatorTest.class);
	
	private ScriptableEvaluator evaluator;
	
	@Mocked Task task;

	@Before
	public void setUp() {
		evaluator = new GroovyEvaluator(new GroovyScriptEngineFactory().getScriptEngine());
	}
	
	@Test
	public void test1() throws LscServiceException {

		try {
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
			
			LOGGER.info("Hello b => " + evaluator.evalToString(task,  	"'Hello ' + a", params));
			
			assertEquals("Hello b", evaluator.evalToString(task,  	"'Hello ' + a", params));
			
			LOGGER.info("[Hello b] => " + evaluator.evalToStringList(task, "'Hello ' + a", params));
			
			assertEquals(Arrays.asList(new String[] {"Hello b"}), evaluator.evalToStringList(task, "'Hello ' + a", params));
			
			params.put("a", new String[] { "b", "c" } );
			
			LOGGER.info("[Hello [b, c]] => " + evaluator.evalToStringList(task, "'Hello ' + a", params));
			
			assertEquals(Arrays.asList(new String[] {"Hello [b, c]"}), evaluator.evalToStringList(task, "'Hello ' + a", params));
			
			String complexExpression = "def dataToStringEquality = { a, b -> \n" +
				" return a.toString() == b.toString() \n" + 
				"}\n" +
				"dataToStringEquality a, b";
			
			LOGGER.info("false => " + evaluator.evalToBoolean(task, complexExpression, params));
			
			assertEquals(false, evaluator.evalToBoolean(task, complexExpression, params));
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			throw new RuntimeException(e);
		}
	}
}
