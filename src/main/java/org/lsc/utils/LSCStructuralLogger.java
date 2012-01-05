package org.lsc.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is done to reference all the structural loggers which
 * record all the problems which need special attention
 */
public interface LSCStructuralLogger {

	public static final Logger DESTINATION = LoggerFactory.getLogger("lsc.destination");

	public static final Logger GLOBAL = LoggerFactory.getLogger("lsc.global");

	public static final Logger SOURCE = LoggerFactory.getLogger("lsc.source");

	public static final Logger TESTS = LoggerFactory.getLogger("lsc.tests");

}
