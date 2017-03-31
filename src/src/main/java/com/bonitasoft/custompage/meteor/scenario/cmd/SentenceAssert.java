package com.bonitasoft.custompage.meteor.scenario.cmd;

import java.util.List;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;

public class SentenceAssert extends Sentence {

    public static String Verb = "ASSERT";

    public SentenceAssert(final List<String> listParams, final APIAccessor apiAccessor) {
        super(listParams, apiAccessor);
    }

    @Override
    public List<BEvent> decodeSentence() {
        return null;
    }

    @Override
    public List<BEvent> execute() {
        return null;
    }

}
