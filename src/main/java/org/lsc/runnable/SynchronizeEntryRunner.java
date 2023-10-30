package org.lsc.runnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.lsc.AbstractSynchronize;
import org.lsc.beans.InfoCounter;
import org.lsc.LscDatasets;
import org.lsc.LscModificationType;
import org.lsc.LscModifications;
import org.lsc.Task;
import org.lsc.beans.BeanComparator;
import org.lsc.beans.IBean;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.utils.ScriptingEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.Hooks;
import org.lsc.beans.syncoptions.ISyncOptions.OutputFormat;

/**
 * @author sbahloul
 */
public class SynchronizeEntryRunner extends AbstractEntryRunner {

	static final Logger LOGGER = LoggerFactory.getLogger(SynchronizeEntryRunner.class);
	private boolean fromSource;
	private Hooks hooks;

	public SynchronizeEntryRunner(final Task task, InfoCounter counter,
			AbstractSynchronize abstractSynchronize,
			Entry<String, LscDatasets> id,
			boolean fromSource) {
		super(task, counter, abstractSynchronize, id);
		this.fromSource = fromSource;
		this.hooks = new Hooks();
	}
	
	@Override
	public void run() {
		counter.incrementCountAll();
		try {
			run(abstractSynchronize.getBean(task, fromSource ? task.getSourceService() : task.getDestinationService(), id.getKey(), id.getValue(), true, fromSource));
		} catch (RuntimeException e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(null, id.getValue(), e);

			if (e.getCause() instanceof LscServiceCommunicationException) {
				LOGGER.error("Connection lost! Aborting.");
			}
		} catch (Exception e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(null, id.getValue(), e);
		}
	}

	public boolean run(IBean entry) {

		LscModifications lm = null;
		IBean dstBean = null;
		/** Hash table to pass objects into JavaScript condition */
		Map<String, Object> conditionObjects = null;

		try {
			/*
			 * Log an error if the source object could not be retrieved! This
			 * shouldn't happen.
			 */
			if (entry == null) {
				counter.incrementCountError();
				StringBuilder sb = new StringBuilder("Cannot synchronize entry");
				if (id != null) {
					sb.append(" ").append(id.getKey());
				}
				sb.append(": no matching object found in source");
				if (id != null && id.getValue() != null && !id.getValue().getDatasets().isEmpty()) {
					sb.append(" with pivots ").append(id.getValue().toString());
				}
				LOGGER.error(sb.toString());
				return false;
			}

			// Search destination for matching object
			if(id != null) {
				dstBean = abstractSynchronize.getBean(task, task.getDestinationService(), id.getKey(), id.getValue(), ! fromSource, fromSource);
			} else {
				LscDatasets entryDatasets = new LscDatasets();
				for(String datasetName: entry.datasets().getAttributesNames()) {
					entryDatasets.getDatasets().put(datasetName, entry.getDatasetById(datasetName));
				}
				dstBean = abstractSynchronize.getBean(task, task.getDestinationService(), entry.getMainIdentifier(), entryDatasets, ! fromSource, fromSource);
			}

			// Calculate operation that would be performed
			LscModificationType modificationType = BeanComparator.calculateModificationType(task, entry, dstBean);

			// Retrieve condition to evaluate before creating/updating
			Boolean applyCondition = null;
			String conditionString = task.getSyncOptions().getCondition(modificationType);

			// Don't use JavaScript evaluator for primitive cases
			if (conditionString.matches("true")) {
				applyCondition = true;
			} else if (conditionString.matches("false")) {
				applyCondition = false;
			} else {
				conditionObjects = new HashMap<String, Object>();
				conditionObjects.put("dstBean", dstBean);
				conditionObjects.put("srcBean", entry);
				conditionObjects.putAll(task.getScriptingVars());
				if (task.getCustomLibraries() != null) {
					conditionObjects.put("custom", task.getCustomLibraries());
				}

				// Evaluate if we have to do something
				applyCondition = ScriptingEvaluator.evalToBoolean(task, conditionString, conditionObjects);
			}

			if (applyCondition) {
				lm = BeanComparator.calculateModifications(task, entry, dstBean);

				// if there's nothing to do, skip to the next object
				if (lm == null) {
					return true;
				}

				counter.incrementCountModifiable();

				// no modification: log action for debugging purposes and forget
				if ((modificationType == LscModificationType.CREATE_OBJECT && abstractSynchronize.nocreate)
						|| (modificationType == LscModificationType.UPDATE_OBJECT && abstractSynchronize.noupdate)
						|| (modificationType == LscModificationType.CHANGE_ID && (abstractSynchronize.nomodrdn || abstractSynchronize.noupdate))) {
					abstractSynchronize.logShouldAction(lm, syncName);
					return true;
				}

			} else {
				return true;
			}

			// if we got here, we have a modification to apply - let's do it!
			if (task.getDestinationService().apply(lm)) {
				// Retrieve posthook for the current operation
				hooks.postSyncHook(	task.getSyncOptions().getPostHook(modificationType),
							task.getSyncOptions().getPostHookOutputFormat(),
							lm);
				counter.incrementCountCompleted();
				abstractSynchronize.logAction(lm, id, syncName);
				return true;
			} else {
				counter.incrementCountError();
				abstractSynchronize.logActionError(lm, (id != null ? id.getValue() : entry.getMainIdentifier()), new Exception("Technical problem while applying modifications to the destination"));
				return false;
			}
		} catch (RuntimeException e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(lm, (id != null ? id.getValue() : ( entry != null ? entry.getMainIdentifier() : e.toString())), e);

			if (e.getCause() instanceof LscServiceCommunicationException) {
				LOGGER.error("Connection lost! Aborting.");
			}
			return false;
		} catch (Exception e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(lm, (id != null ? id.getValue() : entry.getMainIdentifier()), e);
			return false;
		}
	}

}

