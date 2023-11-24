package de.jutzig.github.release.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHReleaseBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;

/**
 * Goal which attaches a file to a GitHub release
 */
@Mojo(name = "release", defaultPhase = LifecyclePhase.DEPLOY)
public class UploadMojo extends AbstractMojo implements Contextualizable{

	private static final String PUBLIC_GITUHB_API_ENDPOINT = "https://api.github.com";
	/**
	 * Server id for github access.
	 */
	@Parameter(defaultValue = "github")
	private String serverId;

	/**
	 * The tag name this release is based on.
	 */
	@Parameter(defaultValue = "${project.version}")
	private String tag;

	/**
	 * The name of the release
	 */
	@Parameter(property = "relase.name")
	private String releaseName;

	/**
	 * The release description
	 */
	@Parameter
	private String description;

	/**
	 * The commitish to use
	 */
	@Parameter(property = "github.commitish")
	private String commitish;

	/**
	 * Whether or not the release should be draft
	 */
	@Parameter(property = "github.draft")
	private Boolean draft;

	/**
	 * The github id of the project. By default initialized from the project scm connection
	 */
	@Parameter(property = "release.repositoryId", defaultValue = "${project.scm.connection}", required = true)
	private String repositoryId;

	 /**
	 * The Maven settings
	 */
	@Parameter(defaultValue = "${settings}", readonly = true, required = true)
	private Settings settings;

	/**
	 * The file to upload to the release. Default is ${project.build.directory}/${project.artifactId}-${project.version}.${project.packaging} (the main artifact)
	 */
	@Parameter(property = "release.artifact", defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}.${project.packaging}")
	private String artifact;

	/**
	 * A specific <code>fileSet</code> rule to select files and directories for upload to the release.
	 */
	@Parameter
	private FileSet fileSet;

	/**
	 * A list of <code>fileSet</code> rules to select files and directories for upload to the release.
	 */
	@Parameter
	private List<FileSet> fileSets;

    /**
     * Flag to indicate to overwrite the asset in the release if it already exists. Default is false
     */
	@Parameter(defaultValue = "false")
    private Boolean overwriteArtifact;

	/**
	 * Flag to indicate whether to remove releases, if they exist, before re-creating them. Default is false
	 */
	@Parameter(defaultValue = "false")
	private Boolean deleteRelease;

	@Requirement
	private PlexusContainer container;

	/**
	 * If this is a prerelease. Will be set by default according to ${project.version} (see {@link #guessPreRelease(String)}.
	 */
	@Parameter
	private Boolean prerelease;

	/**
	 * Fail plugin execution if release already exists.
	 */
	@Parameter(defaultValue = "false")
	private Boolean failOnExistingRelease;

	@Component
	private MavenProject project;

	public void execute() throws MojoExecutionException {
		if(releaseName==null)
			releaseName = tag;
		if(prerelease==null)
			prerelease = guessPreRelease(tag);
		repositoryId = computeRepositoryId(repositoryId);
		GHRelease release = null;
		try {
			GitHub gitHub = createGithub(serverId);
			GHRepository repository = gitHub.getRepository(repositoryId);
			release = findRelease(repository,releaseName);
			if (release != null) {
				String message = "Release " + releaseName + " already exists. Not creating";

				if (failOnExistingRelease) {
					throw new MojoExecutionException(message);
				}

				if (deleteRelease) {
					getLog().info("Removing existing release " + release.getName() + "...");

					release.delete();

					getLog().info("Release " + release.getName() + " removed successfully.");
					release = null;
				} else {
					getLog().info(message);
				}
			}
			if (release == null) {
				getLog().info("Creating release "+releaseName);
				GHReleaseBuilder builder = repository.createRelease(tag);
				builder.generateReleaseNotes(true);
				if(description!=null) {
					builder.body(description);
				}
				if (commitish!=null) {
					builder.commitish(commitish);
				}
				if (draft!=null) {
					builder.draft(draft);
				}
	
				builder.prerelease(prerelease);
				builder.name(releaseName);
				release = builder.create();
			}
		} catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Failed to create release", e);
        }

		try {
			if(artifact != null && !artifact.trim().isEmpty()) {
				File asset = new File(artifact);
				if(asset.exists()) {
					uploadAsset(release, asset);
				}
			}

			if(fileSet != null)
				uploadAssets(release, fileSet);

			if(fileSets != null)
				for (FileSet set : fileSets)
					uploadAssets(release, set);

		} catch (IOException e) {
		    
			getLog().error(e);
			throw new MojoExecutionException("Failed to upload assets", e);
		}
	}

	private void uploadAsset(GHRelease release, File asset) throws IOException {
		getLog().info("Processing asset "+asset.getPath());

		List<GHAsset> existingAssets = release.getAssets();
		for ( GHAsset a : existingAssets ){
			if (a.getName().equals( asset.getName() )){
				if(overwriteArtifact) {
					getLog().info("  Deleting existing asset");
					a.delete();	
				}
				else
				{
					getLog().warn("Asset "+asset.getName()+" already exists. Skipping");
					return;
				}
			}
		}

		getLog().info("  Upload asset");
		// for some reason this doesn't work currently
		release.uploadAsset(asset, "application/zip");
	}

	private void uploadAssets(GHRelease release, FileSet fileset) throws IOException {
		List<File> assets = FileUtils.getFiles(
				new File(fileset.getDirectory()),
				StringUtils.join(fileset.getIncludes(), ','),
				StringUtils.join(fileset.getExcludes(), ',')
		);
		for (File asset : assets)
			uploadAsset(release, asset);
	}

	private GHRelease findRelease(GHRepository repository, String releaseNameToFind) throws IOException {
        PagedIterable<GHRelease> releases = repository.listReleases();
		for (GHRelease ghRelease : releases) {
			if (releaseNameToFind.equals(ghRelease.getName())) {
				return ghRelease;
			}
		}
		return null;
	}

	/**
	 * @see <a href="https://maven.apache.org/scm/scm-url-format.html">SCM URL Format</a>
	 */
	private static final Pattern REPOSITORY_PATTERN = Pattern.compile(
			"^(scm:git[:|])?" +								//Maven prefix for git SCM
			"(https?://[\\w\\d.-]+/|git@[\\w\\d.-]+:)" +	//GitHub prefix for HTTP/HTTPS/SSH/Subversion scheme
			"([^/]+/[^/\\.]+)" +							//Repository ID
			"(\\.git)?" +									//Optional suffix ".git"
			"(/.*)?$"										//Optional child project path
	, Pattern.CASE_INSENSITIVE);

	public static String computeRepositoryId(String id) {
		Matcher matcher = REPOSITORY_PATTERN.matcher(id);
		if (matcher.matches()) {
			return matcher.group(3);
		} else {
			return id;
		}
	}

	static String computeGithubApiEndpoint(Scm scm) {
		if (scm == null || StringUtils.isEmpty(scm.getConnection())) {
			return PUBLIC_GITUHB_API_ENDPOINT;
		}
		Matcher matcher = REPOSITORY_PATTERN.matcher(scm.getConnection());
		if (!matcher.matches()) {
			return PUBLIC_GITUHB_API_ENDPOINT;
		}
		String githubApiEndpoint = matcher.group(2);
		if (githubApiEndpoint.contains("github.com")) {
			return PUBLIC_GITUHB_API_ENDPOINT;
		}

		if (githubApiEndpoint.startsWith("git@")) {
			// According to the regex pattern above, the matched group would be in a form of git@hostname:
			githubApiEndpoint = githubApiEndpoint.substring(4, githubApiEndpoint.length() - 1);
		}

		githubApiEndpoint = StringUtils.removeEnd(githubApiEndpoint, "/");
		if (!githubApiEndpoint.startsWith("http")) {
			githubApiEndpoint = "https://" + githubApiEndpoint;
		}
		// See https://docs.github.com/en/enterprise-server@3.10/rest/overview/resources-in-the-rest-api?apiVersion=2022-11-28#schema
		return githubApiEndpoint + "/api/v3";
    }

	public GitHub createGithub(String serverId) throws MojoExecutionException, IOException {
		String usernameProperty = System.getProperty("username");
		String passwordProperty = System.getProperty("password");
		String githubApiEndpoint = computeGithubApiEndpoint(project.getScm());
		GitHubBuilder gitHubBuilder = new GitHubBuilder().withEndpoint(githubApiEndpoint);
		if(usernameProperty!=null && passwordProperty!=null)
		{
			getLog().debug("Using server credentials from system properties 'username' and 'password'");
			return gitHubBuilder.withPassword(usernameProperty, passwordProperty).build();
		}

		Server server = getServer(settings, serverId);
		if (server == null)
			throw new MojoExecutionException(MessageFormat.format("Server ''{0}'' not found in settings", serverId));

		getLog().debug(MessageFormat.format("Using ''{0}'' server credentials", serverId));

		try {
			SettingsDecrypter settingsDecrypter = container.lookup(SettingsDecrypter.class);
			SettingsDecryptionResult result = settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(server));
			server = result.getServer();
		} catch (ComponentLookupException cle) {
			throw new MojoExecutionException("Unable to lookup SettingsDecrypter: " + cle.getMessage(), cle);
		}

		String serverUsername = System.getProperty("github.username", server.getUsername());
		String serverPassword = System.getProperty("github.password", server.getPassword());
		String serverAccessToken = System.getProperty("github.apitoken", server.getPrivateKey());
		if (StringUtils.isNotEmpty(serverUsername) && StringUtils.isNotEmpty(serverPassword))
			return gitHubBuilder.withPassword(serverUsername, serverPassword).build();
		else if (StringUtils.isNotEmpty(serverAccessToken))
			return gitHubBuilder.withOAuthToken(serverAccessToken).build();
		else
			throw new MojoExecutionException("Configuration for server " + serverId + " has no login credentials");
	}

	/**
	 * Get server with given id
	 * 
	 * @param settings
	 * @param serverId
	 *            must be non-null and non-empty
	 * @return server or null if none matching
	 */
	protected Server getServer(final Settings settings, final String serverId) {
		if (settings == null)
			return null;
		List<Server> servers = settings.getServers();
		if (servers == null || servers.isEmpty())
			return null;

		for (Server server : servers)
			if (serverId.equals(server.getId()))
				return server;
		return null;
	}

	public void contextualize(Context context) throws ContextException {
		container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
	}

	/**
	 * Guess if a version defined in POM should be considered as {@link #prerelease}.
	 */
	static boolean guessPreRelease(String version) {
		boolean preRelease = version.endsWith("-SNAPSHOT")
				|| StringUtils.containsIgnoreCase(version, "-alpha")
				|| StringUtils.containsIgnoreCase(version, "-beta")
				|| StringUtils.containsIgnoreCase(version, "-RC")
				|| StringUtils.containsIgnoreCase(version, ".RC")
				|| StringUtils.containsIgnoreCase(version, ".M")
				|| StringUtils.containsIgnoreCase(version, ".BUILD_SNAPSHOT");
		return preRelease;
	}
}
