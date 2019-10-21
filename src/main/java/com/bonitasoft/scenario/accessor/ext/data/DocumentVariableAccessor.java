package com.bonitasoft.scenario.accessor.ext.data;

import java.io.Serializable;
import java.util.Map;

import org.bonitasoft.engine.bpm.document.impl.ArchivedDocumentImpl;
import org.bonitasoft.engine.bpm.document.impl.DocumentImpl;

import com.bonitasoft.scenario.accessor.Accessor;

public class DocumentVariableAccessor extends ReferencedVariableAccessor {
	public DocumentVariableAccessor(Long instanceId, String name) {
		super(instanceId, name);
	}

	@Override
	public Serializable getValue(Accessor accessor) throws Exception {
		// Retrieve the execution context
		Map<String, Serializable> executionContext = getExecutionContext(accessor);

		// Retrieve the right business variable
		Serializable documentVariable = executionContext.get(name + ReferencedVariableAccessor.REF_SUFFIX);

		if (documentVariable != null) {
			// Get the document content
			if (documentVariable instanceof DocumentImpl) {
				return accessor.getDefaultProcessAPI().getDocumentContent(((DocumentImpl) documentVariable).getContentStorageId());
			} else if (documentVariable instanceof ArchivedDocumentImpl) {
				return accessor.getDefaultProcessAPI().getDocumentContent(((ArchivedDocumentImpl) documentVariable).getContentStorageId());
			} else {
				throw new Exception("The document value type is not supported by the Scenario library: " + documentVariable.getClass().getName());
			}
		}

		return null;
	}
}
