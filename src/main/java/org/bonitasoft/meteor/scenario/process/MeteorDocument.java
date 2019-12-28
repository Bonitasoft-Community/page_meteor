package org.bonitasoft.meteor.scenario.process;

import java.io.ByteArrayOutputStream;

/* Manage a Document
 * 
 */
public class MeteorDocument {

	MeteorDefProcess mMeteorProcess = null;
	MeteorDefActivity mMeteorActivity = null;

	public String mDocumentName = "";
	public int mIndice;
	public String mFileName = "";
	public ByteArrayOutputStream mContent;

	public MeteorDocument(MeteorDefProcess meteorProcess, MeteorDefActivity meteorActivity, final int indice) {
		mMeteorProcess = meteorProcess;
		mMeteorActivity = meteorActivity;
		mIndice = indice;
	}

	public String getHtmlId() {
		return MeteorScenarioProcess.cstHtmlPrefixDocument + (mMeteorProcess == null ? "" : mMeteorProcess.mProcessDefinitionId.toString()) + "_" + (mMeteorActivity == null ? "" : mMeteorActivity.mActivityDefinitionId.toString()) + "_" + mIndice;

	}
}