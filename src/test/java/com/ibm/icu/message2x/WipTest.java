package com.ibm.icu.message2x;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WipTest {

	@Test(expected = IllegalArgumentException.class)
	public void testNullInput() {
		Parser.parse(null);
	}

	@Test
	public void test() {
		String [] someTests = {
//				"",
				"Hello world",
				"{{Hello world}}",
				"|Hello world|",
				"|Hello\tworld|", // real tab, does not get to the parser 
				"|Hello\tworld|", // escaped tab, gets to the parser
				"|Hello\\|world|",
				"Hello {user}",
				"-1",
				"0",
				"1",
				"+1",
				"-1234",
				"1234",
				"+1234",
				"0.00",
				"-1.00",
				"+1.00",
				"-3.1416",
				"3.1416",
				"7e12",
				"7e-12",
				"-7e12",
				"-7e-12",
				".input foo = {$expiration}",
				".input foo = {$expiration :date}",
		};
		for (String test : someTests) {
			System.out.println("======================");
			System.out.println(Utilities.str(test));
			Parser.parse(test);
		}
	}
}
