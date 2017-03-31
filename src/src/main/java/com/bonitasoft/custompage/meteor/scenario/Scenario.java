package com.bonitasoft.custompage.meteor.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

import com.bonitasoft.custompage.meteor.MeteorToolbox;
import com.bonitasoft.custompage.meteor.scenario.cmd.Sentence;

/**
 * scenario
 */

public class Scenario {

    private static Logger logger = Logger.getLogger(Scenario.class.getName());

    private static BEvent EventSyntaxMissingParenthese = new BEvent("org.bonitasoft.custompage.meteor.MeteorRobotScenario", 1, Level.APPLICATIONERROR,
            "A parenthesis is expected", "Check the sentence",
            "This sentence will not be executed", "Check the sentence");

    public static String cstHtmlScenario = "scenario";
    public static String cstHtmlName = "name";
    public static String cstHtmlNumberOfRobots = "nbrob";
    public static String cstHtmlType = "type";

    public String mScenario = "";
    public String mName;
    public long mNumberOfRobots;

    public enum TYPESCENARIO {
        GRV, CMD
    };

    public TYPESCENARIO mType; // expected GRV or CMD
    public List<Sentence> listSentences = new ArrayList<Sentence>();

    APIAccessor apiAccessor;

    public Scenario(final APIAccessor apiAccessor)
    {
        this.apiAccessor = apiAccessor;
    }


    public void fromMap(final Map<String, String> mapScenario)
    {

        // attention : the processdefinitionId is very long it has to be set in STRING else JSON will do an error
        mNumberOfRobots = MeteorToolbox.getParameterLong(mapScenario, cstHtmlNumberOfRobots, 0);
        mName = MeteorToolbox.getParameterString(mapScenario, cstHtmlName, "");
        mScenario = MeteorToolbox.getParameterString(mapScenario, cstHtmlScenario, "");
        try
        {
            mType = TYPESCENARIO.valueOf(MeteorToolbox.getParameterString(mapScenario, cstHtmlType, TYPESCENARIO.CMD.toString()));
        } catch (final Exception e)
        {
            logger.info("Unknow typeScenario [" + MeteorToolbox.getParameterString(mapScenario, cstHtmlType, "") + "] use CMD (expected "
                    + TYPESCENARIO.GRV.toString() + "," + TYPESCENARIO.CMD.toString() + "]");
        }
    }

    public void addInScenario(final String oneSentence)
    {
        mScenario += oneSentence + ";";
    }
    /**
     * decode
     *
     * @return
     */
    public List<BEvent> decodeScenario()
    {
        final List<BEvent> listEvents = new ArrayList<BEvent>();

        final StringTokenizer st = new StringTokenizer(mScenario, ";");
        while (st.hasMoreTokens())
        {
            final String sentenceSt = st.nextToken();
            // format : createCase( p1,p2,p3 );
            final int startPar = sentenceSt.indexOf("(");
            if (startPar == -1)
            {
                listEvents.add(new BEvent(EventSyntaxMissingParenthese, "missing ( : " + sentenceSt));
                continue;
            }
            final String verb = sentenceSt.substring(0, startPar);
            // decode the parameter
            String param = sentenceSt.substring(startPar + 1);
            if (!param.endsWith(")"))
            {
                listEvents.add(new BEvent(EventSyntaxMissingParenthese, "expected ) : " + sentenceSt));
                continue;
            }
            param = param.substring(0, param.length() - 1);
            final List<String> listParams = new ArrayList<String>();
            final StringTokenizer stParam = new StringTokenizer(param, ",");
            while (stParam.hasMoreTokens())
            {
                listParams.add(stParam.nextToken());
            }
            final Sentence sentence = Sentence.getInstance(verb, listParams, apiAccessor);

            listEvents.addAll(sentence.decodeSentence());
            listSentences.add(sentence);
        }
        return listEvents;
    }

}
