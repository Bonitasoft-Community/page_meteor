package org.bonitasoft.meteor.scenario;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.MeteorToolbox;
import org.bonitasoft.meteor.scenario.cmd.Sentence;
import org.bonitasoft.meteor.scenario.cmd.SentenceAssert;
import org.bonitasoft.meteor.scenario.cmd.SentenceCreateCase;
import org.bonitasoft.meteor.scenario.cmd.SentenceExecuteTask;
import org.bonitasoft.meteor.scenario.cmd.SentenceSleep;
import org.json.simple.JSONValue;

/**
 * scenario
 */

public class Scenario {

	private static Logger logger = Logger.getLogger(Scenario.class.getName());

	private static BEvent EventSyntaxMissingParenthese = new BEvent("org.bonitasoft.custompage.meteor.MeteorRobotScenario", 1, Level.APPLICATIONERROR, "A parenthesis is expected", "Check the sentence", "This sentence will not be executed", "Check the sentence");
	private static BEvent EventUnknowSentence = new BEvent("org.bonitasoft.custompage.meteor.MeteorRobotScenario", 2, Level.APPLICATIONERROR, "The sentence is unknow",
			"Check the sentence. Accepted are " + SentenceCreateCase.Verb + ", " + SentenceExecuteTask.Verb + ", " + SentenceAssert.Verb + ", " + SentenceSleep.Verb, "This sentence will not be executed", "Check the sentence");
	private static BEvent EventJsonScenarioMapExpected = new BEvent("org.bonitasoft.custompage.meteor.MeteorRobotScenario", 3, Level.APPLICATIONERROR, "A Map is expected in JSON", "The JSON object must be a JSON map, like { \"verb\":\"" + SentenceCreateCase.Verb + "\" }",
			"This sentence is not accepted", "Check your scenario");

	private static BEvent EventJsonScenarioMapOrListExpected = new BEvent("org.bonitasoft.custompage.meteor.MeteorRobotScenario", 4, Level.APPLICATIONERROR, "A Map or a LIST is expected in JSON",
			"The JSON object must be a JSON map, like { \"verb\":\"" + SentenceCreateCase.Verb + "\"] or a List of Map, like [{...};{...}]", "This sentence is not accepted", "Check your scenario");

	public static String cstHtmlScenario = "scenario";
	public static String cstHtmlName = "name";
	public static String cstHtmlNumberOfRobots = "nbrob";
	public static String cstHtmlNumberOfExecutions = "nbexec";
	public static String cstHtmlType = "type";

	public String mScenario = "";
	public String mName;
	public long mNumberOfRobots;
	public long mNumberOfExecutions;

	public enum TYPESCENARIO {
		GRV, CMD
	};

	public TYPESCENARIO mType; // expected GRV or CMD
	public List<Sentence> listSentences = new ArrayList<Sentence>();

	APIAccessor apiAccessor;
	long tenantId;

	public Scenario(final APIAccessor apiAccessor, final long tenantId) {
		this.apiAccessor = apiAccessor;
		this.tenantId = tenantId;
	}

	public void fromMap(final Map<String, Object> mapScenario) {

		// attention : the processdefinitionId is very long it has to be set in
		// STRING else JSON will do an error
		mNumberOfRobots = MeteorToolbox.getParameterLong(mapScenario, cstHtmlNumberOfRobots, 0);
		mNumberOfExecutions = MeteorToolbox.getParameterLong(mapScenario, cstHtmlNumberOfExecutions, 1);

		mName = MeteorToolbox.getParameterString(mapScenario, cstHtmlName, "");
		mScenario = MeteorToolbox.getParameterString(mapScenario, cstHtmlScenario, "");
		try {
			mType = TYPESCENARIO.valueOf(MeteorToolbox.getParameterString(mapScenario, cstHtmlType, TYPESCENARIO.CMD.toString()));
		} catch (final Exception e) {
			logger.info("Unknow typeScenario [" + MeteorToolbox.getParameterString(mapScenario, cstHtmlType, "") + "] use CMD (expected " + TYPESCENARIO.GRV.toString() + "," + TYPESCENARIO.CMD.toString() + "]");
		}
	}

	public void addInScenario(final String oneSentence) {
		mScenario += oneSentence + ";";
	}

	/**
	 * decode
	 *
	 * @return
	 */
	public List<BEvent> decodeScenario() {
		final List<BEvent> listEvents = new ArrayList<BEvent>();
		Map<String, Object> collectMapParams = new HashMap<String, Object>();
		int lineNumber = 1;

		final StringTokenizer st = new StringTokenizer(mScenario, ";");
		while (st.hasMoreTokens()) {

			final String sentenceSt = st.nextToken();

			String sentenceStWithoutReturn = sentenceSt.replaceAll("\\n", "").trim();

			// ---------------- verb or JSON ? decode the parameter
			Object jsonScenario = null;
			try {
				jsonScenario = JSONValue.parse(sentenceSt);

			} catch (Exception e) {
				// not a JSON scenario
				jsonScenario = null;
			}
			if (jsonScenario != null) {
				// listOf
				if (jsonScenario instanceof Map) {
					listEvents.addAll(decodeJsonSentence((Map) jsonScenario, lineNumber));

				} else if (jsonScenario instanceof List) {
					for (Object item : (List) jsonScenario) {
						if (item instanceof Map)
							listEvents.addAll(decodeJsonSentence((Map) item, lineNumber));
						else
							listEvents.add(new BEvent(EventJsonScenarioMapExpected, "line " + lineNumber));
					}
				} else
					listEvents.add(new BEvent(EventJsonScenarioMapOrListExpected, "line " + lineNumber));

			} else {
				// ------------------------------------ One Sentence
				// format : createCase( p1,p2,p3 );
				final int startPar = sentenceStWithoutReturn.indexOf("(");
				if (startPar == -1) {
					listEvents.add(new BEvent(EventSyntaxMissingParenthese, "missing ( at line " + lineNumber + " " + sentenceStWithoutReturn));
					continue;
				}
				final String verb = sentenceStWithoutReturn.substring(0, startPar);

				String param = sentenceStWithoutReturn.substring(startPar + 1);
				if (!param.endsWith(")")) {
					listEvents.add(new BEvent(EventSyntaxMissingParenthese, "expected ) : at line " + lineNumber + " " + sentenceStWithoutReturn));
					continue;
				}
				param = param.substring(0, param.length() - 1);
				// so, we have to split on the , BUT some parameters are JSON
				// and will contains , so we have to build a new StringTokenizer
				Map<String, Object> mapParams = decomposeParam("{" + param + "}");
				// overwirte the collectMapParams
				if (mapParams != null) {
					for (String key : mapParams.keySet())
						collectMapParams.put(key, mapParams.get(key));
				}
				// now complete the current mapParam
				if (mapParams == null)
					mapParams = new HashMap<String, Object>();
				for (String key : collectMapParams.keySet())
					if (!mapParams.containsKey(key))
						mapParams.put(key, collectMapParams.get(key));

				final Sentence sentence = Sentence.getInstance(verb, mapParams, apiAccessor);
				if (sentence == null) {
					listEvents.add(new BEvent(EventUnknowSentence, "line " + lineNumber + " " + verb));
				} else {
					listEvents.addAll(sentence.decodeSentence(lineNumber));
					listSentences.add(sentence);
				}
			}

			// how many lines return in this sentence ?
			StringTokenizer stLine = new StringTokenizer("\\n", sentenceSt);
			lineNumber += stLine.countTokens();
		}
		return listEvents;
	}

	/**
	 * decode the JSON Sentence
	 * 
	 * @param jsonSentence
	 * @param lineNumber
	 * @return
	 */
	private List<BEvent> decodeJsonSentence(Map<String, Object> jsonSentence, int lineNumber) {
		final List<BEvent> listEvents = new ArrayList<BEvent>();
		String verb = MeteorToolbox.getParameterString(jsonSentence, "verb", "");
		final Sentence sentence = Sentence.getInstance(verb, jsonSentence, apiAccessor);
		if (sentence == null) {
			listEvents.add(new BEvent(EventUnknowSentence, "line " + lineNumber + " " + verb));
		} else {
			listEvents.addAll(sentence.decodeSentence(lineNumber));
			listSentences.add(sentence);
		}
		return listEvents;
	}

	public long getTenantId() {
		return tenantId;
	}

	/**
	 * input is " a,b, " this is, a school isn't ?", e return is an array of 4:
	 * a b "this is, a school isn't ?" e
	 * 
	 * @param param
	 * @return
	 */
	public Map<String, Object> decomposeParam(String param) {
		Object json = JSONValue.parse(param);
		if (json instanceof Map)
			return (Map<String, Object>) json;
		return null;
	}

	public List<String> decomposeParam2(String param) {
		final List<String> listParams = new ArrayList<String>();
		int index = 0;
		while (index < param.length()) {
			int nextComma = param.indexOf(",", index);
			if (nextComma == -1) { // this is the last param in fact !
				listParams.add(param.substring(index));
				return listParams;

			}
			int nextQuot = param.indexOf("'", index);
			if (nextQuot == -1 || nextQuot > nextComma) {
				// simple situation here
				listParams.add(param.substring(index, nextComma).trim());
				index = nextComma + 1;
			} else {
				// search the end of the nextQuot
				int endQuot = param.indexOf("'", nextQuot + 1);
				// and then, the next comma after the endQuot
				if (endQuot == -1) {
					// syntaxe error : miss the last quot
					listParams.add(param.substring(index));
					return listParams;
				}
				nextComma = param.indexOf(",", endQuot);
				if (nextComma == -1) { // this is the last param in fact !
					listParams.add(param.substring(index));
					return listParams;

				}
				listParams.add(param.substring(index, nextComma));
				index = nextComma + 1;
			}

		}

		return listParams;
	}

	/**
	 * register this scenario to the simulation
	 * 
	 * @param meteorSimulation
	 * @param apiAccessor
	 * @return
	 */
	public List<BEvent> registerInSimulation(final MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
		final List<BEvent> listEvents = new ArrayList<BEvent>();
		meteorSimulation.addScenario(this, apiAccessor);
		return listEvents;
	}
}
