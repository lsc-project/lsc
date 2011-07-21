package org.lsc.configuration.objects.syncoptions;

import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.configuration.objects.SyncOptions;

public class ForceSyncOptions extends SyncOptions {


	public Class<? extends ISyncOptions> getImplementation() {
		return org.lsc.beans.syncoptions.ForceSyncOptions.class;
	}
}
