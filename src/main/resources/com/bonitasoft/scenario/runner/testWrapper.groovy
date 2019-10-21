result.put("TEST_NAME", null);

try {
	accessor.log([message:"TEST_NAME - TEST START"])
	
	try {
		accessor.log([message:"TEST_NAME - TEST BEFORE Execution", level:Level.FINE])
		beforeEachTest();
	} catch(Throwable e) {
		throw new Exception("TEST_NAME/BEFORE_TEST: ", e);
	}
	try {
		accessor.log([message:"TEST_NAME - TEST Execution", level:Level.FINE])
		TEST_SCENARIO

	} catch(Throwable e) {
		throw new Exception("TEST_NAME/TEST: ", e);
	}
	try {
		accessor.log([message:"TEST_NAME - TEST AFTER Execution", level:Level.FINE])
		afterEachTest();
	} catch(Throwable e) {
		throw new Exception("TEST_NAME/AFTER_TEST: ", e);
	}
} catch(Throwable globalError) {
	result.put("TEST_NAME", globalError);
}

log([message:"TEST_NAME - TEST END"])
