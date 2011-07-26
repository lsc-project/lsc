package org.lsc.configuration.objects.syncoptions;

import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.configuration.objects.SyncOptions;
import org.lsc.exception.LscConfigurationException;

/**
 * A pass through implementation to return always a FORCE 
 * synchronization options
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class ForceSyncOptions extends SyncOptions {


	public Class<? extends ISyncOptions> getImplementation() {
		return org.lsc.beans.syncoptions.ForceSyncOptions.class;
	}

	@Override
	public void validate() throws LscConfigurationException {
		// Always validate
	}
}
