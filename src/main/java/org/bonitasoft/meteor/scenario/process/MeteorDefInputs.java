package org.bonitasoft.meteor.scenario.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.bpm.contract.ContractDefinition;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.meteor.MeteorToolbox;
import org.bonitasoft.meteor.MeteorTransformInputs;

import org.json.simple.JSONValue;

/**
 * A Meteor process scenario define input. The input may be use for the
 * CreateCase or for the ExecuteTask. User ca define for an Input multiple JSON,
 * with a percentage of ratio to define which one to use
 *
 */

public class MeteorDefInputs {
	public List<MeteorInputItem> listInputs = new ArrayList<MeteorInputItem>();

	/**
	 * User is suppose to give a %, so total step should be 100, but who
	 * knows....
	 */
	public long totalSteps = 0;
	/**
	 * then the ratio is calculated. If user give to 100¨%, ratio should be 1
	 */
	public double ratioStepsToInput;

	public class MeteorInputItem {
		public int mIndex;
		public long mNbSteps;
		protected Map<String, Serializable> mContent=null;
		public Map<String, Serializable> getContent()
		{ return mContent;};

	}

	/**
	 * the Input is created from a Create Case or a TaskExecution. In any case,
	 * it should have a Contract Definition.
	 */
	public ContractDefinition mContractDefinition;

	public MeteorDefInputs() {
	}
	
	public void setContractDefinition( ContractDefinition contractDefinition )
	{
		mContractDefinition = contractDefinition;
	}
	public ContractDefinition getContractDefinition( )
	{
		return mContractDefinition;
	}
	
	

	/* ******************************************************************** */
	/*                                                                      */
	/* Manage Input															*/
	/*                                                                      */
	/*                                                                      */
	/* ******************************************************************** */


	public void setRunSteps(long nbSteps) {
		this.totalSteps = nbSteps;

		int totalInputSteps = 0;
		for (MeteorInputItem input : listInputs)
			totalInputSteps += input.mNbSteps;
		// we have totalInputStep declared for a nbSteps : calculated the ratio
		// now
		if (nbSteps == 0)
			return;
		// in that situation, we have no input in fact
		if (totalInputSteps == 0) {
			return;
		}
		ratioStepsToInput = ((double) totalInputSteps) / ((double) nbSteps);

	}

	public void prepareInputs( )
	{
		MeteorTransformInputs meteorTransformInput = new MeteorTransformInputs();
		for (MeteorInputItem meteorInput : listInputs) {
			 meteorTransformInput.transformJsonContentForBonitaInput( meteorInput.mContent,mContractDefinition);
		}
	}
	
	/**
	 * 
	 * @param step
	 *            the current execution step. Start at 0.
	 * @return
	 */
	public MeteorInputItem getInputAtStep(long step) {
		if (listInputs.size() == 0)
			return null;
		int inputStep = (int) (step * ratioStepsToInput);
		// search now the input relative to this one
		int sumInputStep = 0;
		for (MeteorInputItem meteorInput : listInputs) {
			sumInputStep += meteorInput.mNbSteps;
			if (inputStep < sumInputStep)
				return meteorInput;
		}
		// still not find one at this moment ? Ok, return the last one
		return listInputs.get(listInputs.size() - 1);
	}

	public void addContent(Map<String, Serializable> content) {
		MeteorInputItem oneItem = new MeteorInputItem();
		oneItem.mContent = content;
		listInputs.add(oneItem);
	}

	

	/* ******************************************************************** */
	/*                                                                      */
	/* Manage Input															*/
	/*                                                                      */
	/*                                                                      */
	/* ******************************************************************** */


	public void loadFromString(String listSt) {
		List<Object> listToLoad = (List<Object>) JSONValue.parse(listSt);
		loadFromList(listToLoad);
	}


	/**
	 * Load from the listOfObeject, and rework is needed the JSON to match the contract
	 * @param listToLoad
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<BEvent> loadFromList(List<Object> listToLoad) {
		MeteorTransformInputs meteorTransformInput = new MeteorTransformInputs();

		if (listToLoad == null)
			return new ArrayList<BEvent>();

		for (Object itemLoad : listToLoad) {
			MeteorInputItem oneItem = new MeteorInputItem();
			oneItem.mNbSteps = MeteorToolbox.getParameterLong((Map<String, Object>) itemLoad, MeteorMain.cstHtmlInputPercent, 1);
			if (oneItem.mNbSteps < 1)
				oneItem.mNbSteps = 1;

			String contentSt = MeteorToolbox.getParameterString((Map<String, Object>) itemLoad, MeteorMain.cstHtmlInputContent, "");
			if ((contentSt != null) && (contentSt.length() > 0)) {
				oneItem.mContent = (Map<String, Serializable>) JSONValue.parse(contentSt);
			
				
			}
			oneItem.mIndex = listInputs.size() + 1;
			listInputs.add(oneItem);
		}
		return meteorTransformInput.getEventsTransformation();
	}
	
	/**
	 * propose a JSON to match the contract
	 * @return
	 */
	public String getProposeJson()
	{
		MeteorTransformInputs meteorTransformInput = new MeteorTransformInputs();
		return meteorTransformInput.getProposeJson( mContractDefinition);
	}
	
	
}