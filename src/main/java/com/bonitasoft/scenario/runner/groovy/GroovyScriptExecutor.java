package com.bonitasoft.scenario.runner.groovy;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.runtime.InvokerHelper;

import com.bonitasoft.scenario.accessor.Constants;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

public class GroovyScriptExecutor {

    static public String SCENARIO_GS = "ScenarioGS";

    private static int counter;

    protected static synchronized String generateScriptName() {
        return SCENARIO_GS + (++counter) + Constants.EXTENSION_GROOVY;
    }

    public static Serializable evaluate(final String name, final String scriptContent, final Binding binding) {
        return evaluate(name, scriptContent, binding, new HashMap<String, byte[]>());
    }

    public static Serializable evaluate(final String name, final String scriptContent, final Binding binding, final Map<String, byte[]> dependencies) {
        final GroovyCodeSource gcs = AccessController.doPrivileged(new PrivilegedAction<GroovyCodeSource>() {

            public GroovyCodeSource run() {
                return new GroovyCodeSource(scriptContent, generateScriptName(), GroovyShell.DEFAULT_CODE_BASE);
            }
        });

        final GroovyShell shell = new GroovyShell(new ScenarioClassLoader(Thread.currentThread().getContextClassLoader(), dependencies));
        final Script script = InvokerHelper.createScript(shell.getClassLoader().parseClass(gcs, true), binding);
        return (Serializable) script.run();
    }
}
