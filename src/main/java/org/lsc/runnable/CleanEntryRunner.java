package org.lsc.runnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.naming.CommunicationException;

import org.lsc.AbstractSynchronize;
import org.lsc.LscDatasetModification;
import org.lsc.LscDatasets;
import org.lsc.LscModificationType;
import org.lsc.LscModifications;
import org.lsc.Task;
import org.lsc.LscDatasetModification.LscDatasetModificationType;
import org.lsc.beans.IBean;
import org.lsc.beans.InfoCounter;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.exception.LscServiceException;
import org.lsc.utils.ScriptingEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.Hooks;
import org.lsc.beans.syncoptions.ISyncOptions.OutputFormat;

/**
 * @author sfroger
 */
public class CleanEntryRunner extends AbstractEntryRunner {

	static final Logger LOGGER = LoggerFactory.getLogger(CleanEntryRunner.class);

	private Hooks hooks;

	public CleanEntryRunner(final Task task, InfoCounter counter,
			AbstractSynchronize abstractSynchronize,
			Entry<String, LscDatasets> id) {
		super(task, counter, abstractSynchronize, id);
		this.hooks = new Hooks();
	}

	@Override
	public void run() {
		counter.incrementCountAll();
		ISyncOptions syncOptions = task.getSyncOptions();
		LscModifications lm = null;
		
		try {
			// Search for the corresponding object in the source
			IBean taskBean = abstractSynchronize.getBean(task, task.getSourceService(), id.getKey(), id.getValue(), false, false);

			// If we didn't find the object in the source, delete it in the
			// destination
			if (taskBean == null) {
				// Retrieve condition to evaluate before deleting
				Boolean doDelete = null;
				String conditionString = syncOptions.getDeleteCondition();

				// Don't use JavaScript evaluator for primitive cases
				if (conditionString.matches("true")) {
					doDelete = true;
				} else if (conditionString.matches("false")) {
					doDelete = false;
				} else {
					IBean dstBean = abstractSynchronize.getBean(task, task.getDestinationService(), id.getKey(), id.getValue(), true, false);
					// Log an error if the bean could not be retrieved!
					// This shouldn't happen.
					if (dstBean == null) {
						LOGGER.error("Could not retrieve the object {} from the directory!", id.getKey());
						counter.incrementCountError();
						return;
					}

					// Put the bean in a map to pass to JavaScript
					// evaluator
					Map<String, Object> conditionObjects = new HashMap<String, Object>();
					conditionObjects.put("dstBean", dstBean);
					conditionObjects.putAll(task.getScriptingVars());
					if (task.getCustomLibraries() != null) {
						conditionObjects.put("custom", task.getCustomLibraries());
					}

					// Evaluate if we have to do something
					doDelete = ScriptingEvaluator.evalToBoolean(task, conditionString, conditionObjects);
				}
				
				if (doDelete) {
					lm = new LscModifications(LscModificationType.DELETE_OBJECT, task.getName());
					lm.setMainIdentifer(id.getKey());

					List<LscDatasetModification> attrsMod = new ArrayList<LscDatasetModification>();
					for (Entry<String,Object> attr : id.getValue().getDatasets().entrySet()) {
						attrsMod.add(new LscDatasetModification(LscDatasetModificationType.DELETE_VALUES, attr.getKey(), Collections.singletonList(attr.getValue())));
					}
					lm.setLscAttributeModifications(attrsMod);

					counter.incrementCountModifiable();

					// if "nodelete" was specified in command line options,
					// log action for debugging purposes and continue
					if (abstractSynchronize.nodelete) {
						abstractSynchronize.logShouldAction(lm, task.getName());
						return;
					}
				} else {
					return;
				}

				// if we got here, we have a modification to apply - let's
				// do it!
				if (task.getDestinationService().apply(lm)) {
					// Retrieve posthook for the current operation
					hooks.postSyncHook(	syncOptions.getDeletePostHook(),
								syncOptions.getPostHookOutputFormat(),
								lm);
					counter.incrementCountCompleted();
					abstractSynchronize.logAction(lm, id, task.getName());
				} else {
					counter.incrementCountError();
					abstractSynchronize.logActionError(lm, id.getValue(), new Exception("Technical problem while applying modifications to destination service"));
				}
			}
		} catch (LscServiceException e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(lm, id.getValue(), e);
			if(e.getCause().getClass().isAssignableFrom(CommunicationException.class)) {
				// we lost the connection to the source or destination, stop
				// everything!
				LOGGER.error("Connection lost! Aborting.");
				return;
			} else {
				LOGGER.error("Unable to delete object {} ({})", id.getKey(), e.toString());
				return;
			}
		}
	}
}
