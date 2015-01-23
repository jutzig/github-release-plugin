package de.jutzig.github.release.plugin;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class UploadMojoTest {

	private Map<String, String> computeRepositoryIdData;

	@Before
	public void setUp() throws Exception {
		computeRepositoryIdData = new HashMap<String, String>();
		computeRepositoryIdData.put("scm:git:https://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("scm:git:http://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("scm:git:git@github.com:jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("scm:git:https://github.com/jutzig/github-release-plugin", "jutzig/github-release-plugin");
	}

	@Test
	public void testComputeRepositoryId() throws Exception {
		for (String source : computeRepositoryIdData.keySet()) {
			String expected = computeRepositoryIdData.get(source);
			assertEquals(source, expected, UploadMojo.computeRepositoryId(source));
		}
	}
}
