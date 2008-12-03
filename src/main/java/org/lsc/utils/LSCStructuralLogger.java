package org.lsc.utils;

import org.apache.log4j.Logger;

/**
 * This class is done to reference all the structural loggers which
 * record all the problems which need special attention
 */
public interface LSCStructuralLogger {

	public static final Logger DESTINATION = Logger.getLogger("lsc.destination");

	public static final Logger GLOBAL = Logger.getLogger("lsc.global");

	public static final Logger GENERATION = Logger.getLogger("lsc.generation");

	public static final Logger SOURCE = Logger.getLogger("lsc.source");

	public static final Logger TESTS = Logger.getLogger("lsc.tests");

}
