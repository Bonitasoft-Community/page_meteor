package com.bonitasoft.test

import java.lang.AssertionError

FUNCTIONS

def putUp() {
	PUT_UP
}

def beforeEachTest() {
	BEFORE_EACH_TEST
}

def afterEachTest() {
	AFTER_EACH_TEST
}

def tearDown() {
	TEAR_DOWN
}

Map<String, Throwable> result = new HashMap<String, Throwable>();

accessor.log([message:"PUTTING UP"])

putUp();

try {
	TESTS
} catch (Throwable e) {}

log([message:"TEARING DOWN"])

tearDown();

return result;