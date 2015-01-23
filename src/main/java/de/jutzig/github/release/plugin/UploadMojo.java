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
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHReleaseBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

/**
 * Goal which attaches a file to a GitHub release
 * 
 * @goal release
 * 
 * @phase deploy
 */
public class UploadMojo extends AbstractMojo implements Contextualizable{

	/**
	 * Server id for github access.
	 * 
	 * @parameter expression="github"
	 * @required
	 */
	private String serverId;

	/**
	 * The tag name this release is based on.
	 * 
	 * @parameter expression="${project.version}"
	 */
	private String tag;

	/**
	 * The name of the release
	 * 
	 * @parameter expression="${release.name}"
	 */
	private String releaseName;

	/**
	 * The release description
	 * 
	 * @parameter expression="${project.description}"
	 */
	private String description;

	/**
	 * The github id of the project. By default initialized from the project scm connection
	 * 
	 * @parameter default-value="${project.scm.connection}" expression="${release.repositoryId}"
	 * @required
	 */
	private String repositoryId;

	 /**
	 * The Maven settings
	 *
	 * @parameter expression="${settings}
	 */
	private Settings settings;

	/**
	 * The Maven session
	 *
	 * @parameter expression="${session}"
	 */
	private MavenSession session;

	/**
	 * The file to upload to the release. Default is ${project.build.directory}/${project.artifactId}-${project.version}.${project.packaging} (the main artifact)
	 *
	 * @parameter default-value="${project.build.directory}/${project.artifactId}-${project.version}.${project.packaging}" expression="${release.artifact}"
	 * @required
	 */
	private String artifact;
	
	/**
     * Flag to indicate to overwrite the asset in the release if it already exists. Default is false
     *
     * @parameter default-value=false
     */
    private Boolean overwriteArtifact;

	@Requirement
	private PlexusContainer container;

	/**
	 * If this is a prerelease. By default it will use <code>true</code> if the tag ends in -SNAPSHOT
	 *
	 * @parameter
	 * 
	 */
	private Boolean prerelease;

	public void execute() throws MojoExecutionException {
		if(releaseName==null)
			releaseName = tag;
		if(prerelease==null)
			prerelease = tag.endsWith("-SNAPSHOT");
		repositoryId = computeRepositoryId(repositoryId);
		GHRelease release = null;
		try {
			GitHub gitHub = createGithub(serverId);
			GHRepository repository = gitHub.getRepository(repositoryId);
			release = findRelease(repository,releaseName);
			if(release==null) {
				getLog().info("Creating release "+releaseName);
				GHReleaseBuilder builder = repository.createRelease(tag);
				if(description!=null)
					builder.body(description);
				builder.prerelease(prerelease);
				builder.name(releaseName);
				release = builder.create();
			}
			else {
				getLog().info("Release "+releaseName+" already exists. Not creating");
			}
		} catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Failed to create release", e);
        }
		try {
			File asset = new File(artifact);
			URL url = new URL(MessageFormat.format("https://uploads.github.com/repos/{0}/releases/{1}/assets?name={2}",repositoryId,Long.toString(release.getId()),asset.getName()));
			
			List<GHAsset> existingAssets = release.getAssets();
			for ( GHAsset a : existingAssets ){
			    if (a.getName().equals( asset.getName() ) && overwriteArtifact){
			        getLog().info("Deleting exisiting asset");
			        a.delete();
			    }
			}

			// for some reason this doesn't work currently
			release.uploadAsset(asset, "application/zip");
			
		} catch (IOException e) {
		    
			getLog().error(e);
			throw new MojoExecutionException("Failed to upload assets", e);
		}

	}

	private GHRelease findRelease(GHRepository repository, String releaseName2) throws IOException {
		List<GHRelease> releases = repository.getReleases();
		for (GHRelease ghRelease : releases) {
			if(ghRelease.getName().equals(releaseName2)) {
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
			"(https?://github\\.com/|git@github\\.com:)" +	//GitHub prefix for HTTP/HTTPS/SSH/Subversion scheme
			"([^/]+/[^/]*?)" +								//Repository ID
			"(\\.git)?$"									//Optional suffix ".git"
	, Pattern.CASE_INSENSITIVE);

	public static String computeRepositoryId(String id) {
		Matcher matcher = REPOSITORY_PATTERN.matcher(id);
		if (matcher.matches()) {
			return matcher.group(3);
		} else {
			return id;
		}
	}

	public GitHub createGithub(String serverId) throws MojoExecutionException, IOException {
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

		String serverUsername = server.getUsername();
		String serverPassword = server.getPassword();
		String serverAccessToken = server.getPrivateKey();
		if (StringUtils.isNotEmpty(serverUsername) && StringUtils.isNotEmpty(serverPassword))
			return GitHub.connectUsingPassword(serverUsername, serverPassword);
		else if (StringUtils.isNotEmpty(serverAccessToken))
			return GitHub.connectUsingOAuth(serverAccessToken);
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
}
