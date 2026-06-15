package org.lsc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.junit.jupiter.api.Test;

public class CaseIgnoreStringHashMapTest {

	@Test
	public void testPutAndGetCaseInsensitive() {
		CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
		map.put("Key", "value");
		assertEquals("value", map.get("key"));
		assertEquals("value", map.get("KEY"));
		assertEquals("value", map.get("Key"));
	}

	@Test
	public void testContainsKeyCaseInsensitive() {
		CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
		map.put("Hello", "world");
		assertTrue(map.containsKey("hello"));
		assertTrue(map.containsKey("HELLO"));
		assertTrue(map.containsKey("Hello"));
	}

	@Test
	public void testRemoveCaseInsensitive() {
		CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
		map.put("Test", "data");
		map.remove("test");
		assertNull(map.get("Test"));
	}

	@Test
	public void testPutOverwritesCaseInsensitive() {
		CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
		map.put("key", "first");
		map.put("KEY", "second");
		assertEquals("second", map.get("key"));
		assertEquals(1, map.size());
	}

	@Test
	public void testNullKey() {
		CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
		map.put(null, "value");
		assertEquals("value", map.get(null));
	}
}
