package de.jutzig.github.release.plugin;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.plugin.testing.resources.TestResources;

public class UploadMojoTest {

    @Rule
    public MojoRule rule = new MojoRule() {
      @Override
      protected void before() throws Throwable { }

      @Override
      protected void after() { }
    };

    @Rule
    public TestResources resources = new TestResources();

	private Map<String, String> computeRepositoryIdData_github;
    private Map<String, String> computeRepositoryIdData_github_enterprise;
    private Map<String, String> ensureNoTrailingSlash;

    @Before
	public void setUp() throws Exception {
		computeRepositoryIdData_github = new HashMap<String, String>();

		computeRepositoryIdData_github.put("scm:git:https://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github.put("scm:git|https://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github.put("https://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");

		computeRepositoryIdData_github.put("scm:git:http://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github.put("scm:git|http://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github.put("http://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");

		computeRepositoryIdData_github.put("scm:git:git@github.com:jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github.put("scm:git|git@github.com:jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github.put("git@github.com:jutzig/github-release-plugin.git", "jutzig/github-release-plugin");

		computeRepositoryIdData_github.put("scm:git:https://github.com/jutzig/github-release-plugin", "jutzig/github-release-plugin");
		computeRepositoryIdData_github.put("scm:git|https://github.com/jutzig/github-release-plugin", "jutzig/github-release-plugin");
		computeRepositoryIdData_github.put("https://github.com/jutzig/github-release-plugin", "jutzig/github-release-plugin");

        computeRepositoryIdData_github_enterprise = new HashMap<String, String>();

		computeRepositoryIdData_github_enterprise.put("scm:git:https://some-github-enterprise-server.domain/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github_enterprise.put("scm:git|https://some-github-enterprise-server.domain/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github_enterprise.put("https://some-github-enterprise-server.domain/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");

		computeRepositoryIdData_github_enterprise.put("scm:git:http://some-github-enterprise-server.domain/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github_enterprise.put("scm:git|http://some-github-enterprise-server.domain/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github_enterprise.put("http://some-github-enterprise-server.domain/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");

		computeRepositoryIdData_github_enterprise.put("scm:git:git@some-github-enterprise-server.domain:jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github_enterprise.put("scm:git|git@some-github-enterprise-server.domain:jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData_github_enterprise.put("git@some-github-enterprise-server.domain:jutzig/github-release-plugin.git", "jutzig/github-release-plugin");

		computeRepositoryIdData_github_enterprise.put("scm:git:https://some-github-enterprise-server.domain/jutzig/github-release-plugin", "jutzig/github-release-plugin");
		computeRepositoryIdData_github_enterprise.put("scm:git|https://some-github-enterprise-server.domain/jutzig/github-release-plugin", "jutzig/github-release-plugin");
		computeRepositoryIdData_github_enterprise.put("https://some-github-enterprise-server.domain/jutzig/github-release-plugin", "jutzig/github-release-plugin");

        ensureNoTrailingSlash = new HashMap<String, String>();

        ensureNoTrailingSlash.put(null, null);
        ensureNoTrailingSlash.put("", "");
        ensureNoTrailingSlash.put("no trailing slash", "no trailing slash");
        ensureNoTrailingSlash.put("trailing slash/", "trailing slash");
        ensureNoTrailingSlash.put("no trailing slash trimmed     ", "no trailing slash trimmed");
        ensureNoTrailingSlash.put("trimmed trailing slash/    ", "trimmed trailing slash");
	}

	@Test
	public void testComputeRepositoryId_github_com() throws Exception {

        File testProjectDir = this.resources.getBasedir( "github.com" );
        assertNotNull( testProjectDir );
        assertTrue( testProjectDir.exists() );

        UploadMojo mojo = (UploadMojo) rule.lookupConfiguredMojo( testProjectDir , "release");
        assertNotNull(mojo);

		for (String source : computeRepositoryIdData_github.keySet()) {
            String expected = computeRepositoryIdData_github.get(source);
            assertEquals(source, expected, mojo.computeRepositoryId(source));
		}
	}

    @Test
	public void testComputeRepositoryId_github_enterprise() throws Exception {

        File testProjectDir = this.resources.getBasedir( "github.enterprise" );
        File pom  = new File(testProjectDir, "pom.xml");
        assertNotNull( pom );
        assertTrue( pom.exists() );

        UploadMojo mojo = (UploadMojo) rule.lookupMojo("release", pom);
        assertNotNull(mojo);

		for (String source : computeRepositoryIdData_github_enterprise.keySet()) {
            String expected = computeRepositoryIdData_github_enterprise.get(source);
            assertEquals(source, expected, mojo.computeRepositoryId(source));
		}
	}

    @Test
    public void testEnsureNoTrailingSlash() throws Exception {

        File testProjectDir = this.resources.getBasedir( "github.com" );
        assertNotNull( testProjectDir );
        assertTrue( testProjectDir.exists() );

        UploadMojo mojo = (UploadMojo) rule.lookupConfiguredMojo( testProjectDir , "release");
        assertNotNull(mojo);

		for (String str : ensureNoTrailingSlash.keySet()) {
            String expected = ensureNoTrailingSlash.get(str);
            assertEquals(str, expected, mojo.ensureNoTrailingSlash(str));
		}

    }

}
