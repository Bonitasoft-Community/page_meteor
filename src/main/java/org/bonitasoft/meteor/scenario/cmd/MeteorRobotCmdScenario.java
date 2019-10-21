package org.bonitasoft.meteor.scenario.cmd;

import java.util.List;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.scenario.Scenario;

public class MeteorRobotCmdScenario extends MeteorRobot {

	public Scenario meteorScenario;

	public MeteorRobotCmdScenario(MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
		super(meteorSimulation, apiAccessor);
	}

	public void setScenario(final Scenario meteorScenario) {
		this.meteorScenario = meteorScenario;
	}

	/**
	 * @param args
	 */

	@Override
	public void executeRobot() {
		// please call setNumberTotalOperation( long nbOperation) and
		// setOperationIndex( long indexOperation)
		List<BEvent> listEvents = meteorScenario.decodeScenario();
		if (BEventFactory.isError(listEvents)) {
			for (BEvent event : listEvents)
				mLogExecution.addEvent(event);
			return;
		}

		setOperationTotal(meteorScenario.mNumberOfExecutions * meteorScenario.listSentences.size());

		for (int i = 0; i < meteorScenario.mNumberOfExecutions; i++) {
			logger.info("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + mRobotId + " ------ Advancement " + i + " / " + meteorScenario.mNumberOfExecutions);

			final long timeStart = System.currentTimeMillis();
			for (int j = 0; j < meteorScenario.listSentences.size(); j++) {
				setOperationIndex(i * meteorScenario.listSentences.size() + j);
				listEvents = meteorScenario.listSentences.get(j).execute(mRobotId, mLogExecution);
			}
			final long timeEnd = System.currentTimeMillis();
			mCollectPerformance.collectOneStep(timeEnd - timeStart);
		}
	}

}
