package de.jutzig.github.release.plugin;

import java.util.stream.Stream;

import org.apache.maven.model.Scm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import static org.junit.jupiter.api.Assertions.*;

class UploadMojoTest {
	@ParameterizedTest(name = "{0} should resolve to {1} repository id")
	@CsvSource({
		// Public
		"scm:git:https://github.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"scm:git|https://github.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"https://github.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",

		"scm:git:http://github.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"scm:git|http://github.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"http://github.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",

		"scm:git:git@github.com:jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"scm:git|git@github.com:jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"git@github.com:jutzig/github-release-plugin.git, jutzig/github-release-plugin",

		"scm:git:https://github.com/jutzig/github-release-plugin, jutzig/github-release-plugin",
		"scm:git|https://github.com/jutzig/github-release-plugin, jutzig/github-release-plugin",
		"https://github.com/jutzig/github-release-plugin, jutzig/github-release-plugin",

		"scm:git:http://github.com/jutzig/github-release-plugin.git/child, jutzig/github-release-plugin",
		"scm:git|http://github.com/jutzig/github-release-plugin.git/child, jutzig/github-release-plugin",
		"http://github.com/jutzig/github-release-plugin.git/child, jutzig/github-release-plugin",
		
		// Enterprise
		"scm:git:https://github.acme.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"scm:git|https://github.acme.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"https://github.acme.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",

		"scm:git:http://github.acme.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"scm:git|http://github.acme.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"http://github.acme.com/jutzig/github-release-plugin.git, jutzig/github-release-plugin",

		"scm:git:git@github.acme.com:jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"scm:git|git@github.acme.com:jutzig/github-release-plugin.git, jutzig/github-release-plugin",
		"git@github.acme.com:jutzig/github-release-plugin.git, jutzig/github-release-plugin",

		"scm:git:https://github.acme.com/jutzig/github-release-plugin, jutzig/github-release-plugin",
		"scm:git|https://github.acme.com/jutzig/github-release-plugin, jutzig/github-release-plugin",
		"https://github.acme.com/jutzig/github-release-plugin, jutzig/github-release-plugin",

		"scm:git:http://github.acme.com/jutzig/github-release-plugin.git/child, jutzig/github-release-plugin",
		"scm:git|http://github.acme.com/jutzig/github-release-plugin.git/child, jutzig/github-release-plugin",
		"http://github.acme.com/jutzig/github-release-plugin.git/child, jutzig/github-release-plugin"
	})
	void testComputeRepositoryId(String scmString, String expectedRepositoryId) {
		assertEquals(expectedRepositoryId, UploadMojo.computeRepositoryId(scmString));
	}

	@ParameterizedTest(name = "{0} should resolve to {1} endpoint")
	@MethodSource("scmFixture")
	void testGithubEndpoint(Scm scm, String expectedEndpoint) {
		assertEquals(expectedEndpoint, UploadMojo.computeGithubApiEndpoint(scm));
	}

	@Test
	void testGuessPreRelease() {
		assertTrue(UploadMojo.guessPreRelease("1.0-SNAPSHOT"));
		assertTrue(UploadMojo.guessPreRelease("1.0-alpha"));
		assertTrue(UploadMojo.guessPreRelease("1.0-alpha-1"));
		assertTrue(UploadMojo.guessPreRelease("1.0-beta"));
		assertTrue(UploadMojo.guessPreRelease("1.0-beta-1"));
		assertTrue(UploadMojo.guessPreRelease("1.0-RC"));
		assertTrue(UploadMojo.guessPreRelease("1.0-RC1"));
		assertTrue(UploadMojo.guessPreRelease("1.0-rc1"));
		assertTrue(UploadMojo.guessPreRelease("1.0-rc-1"));

		assertFalse(UploadMojo.guessPreRelease("1"));
		assertFalse(UploadMojo.guessPreRelease("1.0"));
	}

	private static Stream<Arguments> scmFixture() {
		return Stream.of(
			// Public GitHub
			Arguments.of(scmWithConnectionString("scm:git:https://github.com/jutzig/github-release-plugin.git"), "https://api.github.com"),
			Arguments.of(scmWithConnectionString("scm:git|https://github.com/jutzig/github-release-plugin.git"), "https://api.github.com"),
			Arguments.of(scmWithConnectionString("https://github.com/jutzig/github-release-plugin.git"), "https://api.github.com"),

			Arguments.of(scmWithConnectionString("scm:git:http://github.com/jutzig/github-release-plugin.git"), "https://api.github.com"),
			Arguments.of(scmWithConnectionString("scm:git|http://github.com/jutzig/github-release-plugin.git"), "https://api.github.com"),
			Arguments.of(scmWithConnectionString("http://github.com/jutzig/github-release-plugin.git"), "https://api.github.com"),

			Arguments.of(scmWithConnectionString("scm:git:git@github.com:jutzig/github-release-plugin.git"), "https://api.github.com"),
			Arguments.of(scmWithConnectionString("scm:git|git@github.com:jutzig/github-release-plugin.git"), "https://api.github.com"),
			Arguments.of(scmWithConnectionString("git@github.com:jutzig/github-release-plugin.git"), "https://api.github.com"),

			Arguments.of(scmWithConnectionString("scm:git:https://github.com/jutzig/github-release-plugin"), "https://api.github.com"),
			Arguments.of(scmWithConnectionString("scm:git|https://github.com/jutzig/github-release-plugin"), "https://api.github.com"),
			Arguments.of(scmWithConnectionString("https://github.com/jutzig/github-release-plugin"), "https://api.github.com"),

			Arguments.of(scmWithConnectionString("scm:git:http://github.com/jutzig/github-release-plugin.git/child"), "https://api.github.com"),
			Arguments.of(scmWithConnectionString("scm:git|http://github.com/jutzig/github-release-plugin.git/child"), "https://api.github.com"),
			Arguments.of(scmWithConnectionString("http://github.com/jutzig/github-release-plugin.git/child"), "https://api.github.com"),

			// GitHub Enterprise
			Arguments.of(scmWithConnectionString("scm:git:https://github.acme.com/jutzig/github-release-plugin.git"), "https://github.acme.com/api/v3"),
			Arguments.of(scmWithConnectionString("scm:git|https://github.acme.com/jutzig/github-release-plugin.git"), "https://github.acme.com/api/v3"),
			Arguments.of(scmWithConnectionString("https://github.acme.com/jutzig/github-release-plugin.git"), "https://github.acme.com/api/v3"),

			Arguments.of(scmWithConnectionString("scm:git:http://github.acme.com/jutzig/github-release-plugin.git"), "http://github.acme.com/api/v3"),
			Arguments.of(scmWithConnectionString("scm:git|http://github.acme.com/jutzig/github-release-plugin.git"), "http://github.acme.com/api/v3"),
			Arguments.of(scmWithConnectionString("http://github.acme.com/jutzig/github-release-plugin.git"), "http://github.acme.com/api/v3"),

			Arguments.of(scmWithConnectionString("scm:git:git@github.acme.com:jutzig/github-release-plugin.git"), "https://github.acme.com/api/v3"),
			Arguments.of(scmWithConnectionString("scm:git|git@github.acme.com:jutzig/github-release-plugin.git"), "https://github.acme.com/api/v3"),
			Arguments.of(scmWithConnectionString("git@github.acme.com:jutzig/github-release-plugin.git"), "https://github.acme.com/api/v3"),

			Arguments.of(scmWithConnectionString("scm:git:https://github.acme.com/jutzig/github-release-plugin"), "https://github.acme.com/api/v3"),
			Arguments.of(scmWithConnectionString("scm:git|https://github.acme.com/jutzig/github-release-plugin"), "https://github.acme.com/api/v3"),
			Arguments.of(scmWithConnectionString("https://github.acme.com/jutzig/github-release-plugin"), "https://github.acme.com/api/v3"),

			Arguments.of(scmWithConnectionString("scm:git:http://github.acme.com/jutzig/github-release-plugin.git/child"), "http://github.acme.com/api/v3"),
			Arguments.of(scmWithConnectionString("scm:git|http://github.acme.com/jutzig/github-release-plugin.git/child"), "http://github.acme.com/api/v3"),
			Arguments.of(scmWithConnectionString("http://github.acme.com/jutzig/github-release-plugin.git/child"), "http://github.acme.com/api/v3"),

			// Fallback to public
			Arguments.of(null, "https://api.github.com")
		);
	}

	private static Scm scmWithConnectionString(String connection) {
		Scm scm = new Scm();
		scm.setConnection(connection);
		return scm;
	}
}
