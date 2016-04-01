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
    public MojoRule rule = new MojoRule()
    {
      @Override
      protected void before() throws Throwable
      {
      }

      @Override
      protected void after()
      {
      }
    };

    @Rule
    public TestResources resources = new TestResources();

	private Map<String, String> computeRepositoryIdData;

    @Before
	public void setUp() throws Exception {
		computeRepositoryIdData = new HashMap<String, String>();

		computeRepositoryIdData.put("scm:git:https://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("scm:git|https://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("https://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");

		computeRepositoryIdData.put("scm:git:http://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("scm:git|http://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("http://github.com/jutzig/github-release-plugin.git", "jutzig/github-release-plugin");

		computeRepositoryIdData.put("scm:git:git@github.com:jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("scm:git|git@github.com:jutzig/github-release-plugin.git", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("git@github.com:jutzig/github-release-plugin.git", "jutzig/github-release-plugin");

		computeRepositoryIdData.put("scm:git:https://github.com/jutzig/github-release-plugin", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("scm:git|https://github.com/jutzig/github-release-plugin", "jutzig/github-release-plugin");
		computeRepositoryIdData.put("https://github.com/jutzig/github-release-plugin", "jutzig/github-release-plugin");
        //        computeRepositoryIdData.put("http://some-github-enterprise-server.domain/user/repository.git", "user/repository");
	}

	@Test
	public void testComputeRepositoryId() throws Exception {

        File projectCopy = this.resources.getBasedir( "github.com-project" );
        File pom = new File(projectCopy, "pom.xml");
        assertNotNull( pom );
        assertTrue( pom.exists() );

        UploadMojo mojo = (UploadMojo) rule.lookupMojo( "", pom);
        assertNotNull(mojo);

		for (String source : computeRepositoryIdData.keySet()) {
			String expected = computeRepositoryIdData.get(source);
			assertEquals(source, expected, mojo.computeRepositoryId(source));
		}

        mojo.execute();
	}
}
