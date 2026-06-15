package org.lsc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

public class StringLengthComparatorTest {

	@Test
	public void testSortByLengthDescending() {
		List<String> list = new ArrayList<>();
		list.add("a");
		list.add("ccc");
		list.add("bb");
		list.add("dddd");

		list.sort(Comparator.comparingInt(String::length).reversed());

		assertEquals("dddd", list.get(0));
		assertEquals("ccc", list.get(1));
		assertEquals("bb", list.get(2));
		assertEquals("a", list.get(3));
	}

	@Test
	public void testSameLengthStable() {
		List<String> list = new ArrayList<>();
		list.add("ab");
		list.add("cd");

		list.sort(Comparator.comparingInt(String::length).reversed());

		assertEquals("ab", list.get(0));
		assertEquals("cd", list.get(1));
	}
}
