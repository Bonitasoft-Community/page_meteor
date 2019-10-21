package org.bonitasoft.meteor.scenario.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.contract.ContractDefinition;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorToolbox;
import org.bonitasoft.meteor.scenario.process.MeteorDefInputs.MeteorInputItem;

/* all elements in Process use the same pattern : 
 * - a number of robots
 * - a number of execution
 * - a toime to sleep between each execution
 * - a time to sleep before starts
 * - a contract to respect and a list of inputs
 * 
 */
public class MeteorDefBase {

	protected static BEvent EventGetContract = new BEvent(MeteorMain.class.getName(), 1, Level.ERROR, "Error while accessing contract", "Check error ", "The contact can't be accessed", "Check Exception");
	protected static BEvent EventNoInputs = new BEvent(MeteorMain.class.getName(), 2, Level.INFO, "No inputs", "The process/activity does not have any input");

	// this is the robot part : how many robot do we have to start on this
	// activity ?
	public long mNumberOfRobots;
	public long mTimeSleep;
	public long mDelaySleep;
	public long mNumberOfCases;

	public MeteorDefInputs mInputs =  new MeteorDefInputs();
	public List<BEvent> mListEvents = new ArrayList<BEvent>();

	public List<MeteorDocument> mListDocuments = new ArrayList<MeteorDocument>();
	/**
	 * to run a task, a contract is needed.
	 */
	
	public void setContractDefinition(ContractDefinition contractDefinition )
	{
		mInputs.setContractDefinition(contractDefinition);
		
	}
	
	/** return the list of document */
	public List<MeteorDocument> getListDocuments()
	{
		return mListDocuments;
	}
	
	
	public void prepareInputs()
	{
		mInputs.prepareInputs();
	}
	
	public void decodeFromMap(final Map<String, Object> oneProcess, ProcessAPI processAPI) {
		mNumberOfRobots = MeteorToolbox.getParameterLong(oneProcess, MeteorMain.cstHtmlNumberOfRobots, 0);
		mNumberOfCases = MeteorToolbox.getParameterLong(oneProcess, MeteorMain.cstJsonNumberOfCases, 0);
		mDelaySleep = MeteorToolbox.getParameterLong(oneProcess, MeteorMain.cstHtmlDelaySleep, 0);
		mTimeSleep = MeteorToolbox.getParameterLong(oneProcess, MeteorMain.cstHtmlTimeSleep, 0);
		mListEvents.addAll(mInputs.loadFromList(MeteorToolbox.getParameterList(oneProcess, MeteorMain.cstHtmlInputs, null)));
	}
	
	
	public void fullfillMap(final Map<String, Object> oneProcess) 
		{
			oneProcess.put(MeteorMain.cstHtmlNumberOfRobots, mNumberOfRobots);
			oneProcess.put(MeteorMain.cstJsonNumberOfCases, mNumberOfCases);
			oneProcess.put(MeteorMain.cstHtmlDelaySleep, mDelaySleep);
			oneProcess.put(MeteorMain.cstHtmlTimeSleep, mTimeSleep);
			// build a proposition to the input
			oneProcess.put(MeteorMain.cstHtmlInputProposeContent, mInputs.getProposeJson()); 

		}

}
