package com.bonitasoft.scenario.accessor.ext.data;

import java.io.Serializable;

import com.bonitasoft.scenario.accessor.Accessor;

abstract class DataAccessor implements Serializable {

    protected Long instanceId = null;
    protected String name = null;

    protected DataAccessor(Long instanceId, String name) {
        this.instanceId = instanceId;
        this.name = name;
    }

    abstract public Serializable getValue(Accessor accessor) throws Exception;
}
