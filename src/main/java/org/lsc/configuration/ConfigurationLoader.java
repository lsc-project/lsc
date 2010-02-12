/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.lsc.configuration;

import com.thoughtworks.xstream.XStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.lsc.configuration.objects.Audit;
import org.lsc.configuration.objects.Connection;
import org.lsc.configuration.objects.LscConfiguration;
import org.lsc.configuration.objects.Service;
import org.lsc.configuration.objects.Task;

/**
 *
 * @author rschermesser
 */
public class ConfigurationLoader {

	private String filename;
	private XStream stream;
	private LscConfiguration configuration;


	public ConfigurationLoader(String filename) throws FileNotFoundException {
		stream = new XStream();
		stream.alias("lsc", LscConfiguration.class);
		stream.alias("connection", Connection.class);
		stream.alias("audit", Audit.class);
		stream.alias("task", Task.class);
		stream.alias("service", Service.class);
		configuration = (LscConfiguration)stream.fromXML(new FileInputStream(filename));
	}

	public LscConfiguration getConfiguration() {
		return configuration;
	}

}
