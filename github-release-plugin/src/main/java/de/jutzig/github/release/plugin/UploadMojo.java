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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

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
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

/**
 * Goal which touches a timestamp file.
 * 
 * @goal touch
 * 
 * @phase process-sources
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
     * The Maven settings
     *
     * @parameter expression="${settings}
     */
	private Settings settings;
	
    /**
     * The Maven session
     *
     * @parameter expression="${session}
     */
    private MavenSession session;

	@Requirement
	private PlexusContainer container;

	public void execute() throws MojoExecutionException {
		try {
			GitHub gitHub = createGithub(serverId);
			GHRepository repository = gitHub.getRepository("jutzig/github-release-plugin");
			repository.createRelease("");
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to upload assets", e);
		}

	}
	
	public GitHub createGithub(String serverId) throws MojoExecutionException, IOException {
		String serverUsername = null;
		String serverPassword = null;

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

		serverUsername = server.getUsername();
		serverPassword = server.getPassword();
		GitHub gitHub = GitHub.connectUsingPassword(serverUsername, serverPassword);
		return gitHub;
		
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
