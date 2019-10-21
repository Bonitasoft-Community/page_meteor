} catch(AssertionFailureException assertionFailureException) {
	// Do nothing as it is coming from the error block and it has already been handled
} catch(AssertionError globalAssertionError) {
	accessor.scenarioResult.addError(globalAssertionError, "GS Scenario assertion failure")
}