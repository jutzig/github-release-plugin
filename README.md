github-release-plugin
=====================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.jutzig/github-release-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.jutzig/github-release-plugin)

uses the github release api to upload files

To use the plugin you need to configure it in your `pom.xml` like so

```
<plugin>
    <groupId>de.jutzig</groupId>
    <artifactId>github-release-plugin</artifactId>
    <version>1.5.0</version>
    <configuration>
        <description>Description of your release</description>
        <releaseName>1.0 Final</releaseName>
        <tag>${project.version}</tag>
        
        <!-- If your project has additional artifacts, such as ones produced by
             the maven-assembly-plugin, you can define the following
             (requires version 1.1.1 of the plugin or higher): -->
        <fileSets>
            <fileSet>
                <directory>${project.build.directory}</directory>
                <includes>
                    <include>${project.artifactId}*.tar.gz</include>
                    <include>${project.artifactId}*.zip</include>
                </includes>
            </fileSet>
        </fileSets>
    </configuration>
</plugin>
```

Unless otherwise specified, the plugin will upload the main artifact of your project and take the github repository url from the `<scm>` section.

By default, the plugin will look for your github credentials in your maven `settings.xml`. Credentials can be privated as an API token, or username and password. Example
```
<servers>
    <server>
        <id>github</id>
        <privateKey>API_TOKEN</privateKey>
        <username>GITHUB_USERNAME</username>
        <password>GITHUB_PASSWORD</password>
    </server>
</servers>
```

These credentials can be overridden by setting `username` and `password` as system properties.

Additional Parameters:

 * `-Dgithub.draft=true` creates the release in draft state
 * `-Dgithub.commitish=release/1.0.0` allows to specify a commitsh
 * `-Dgithub.apitoken=API_TOKEN` allows to set github api token instead of providing it in settings.xml
 * `-Dgithub.username=GITHUB_USERNAME` allows to set github username instead of providing it in settings.xml
 * `-Dgithub.password=GITHUB_PASSWORD` allows to set github password instead of providing it in settings.xml

The plugin is available on Maven central

## Note on the GitHub API endpoints
The endpoint for GitHub API is inferred from `<scm>` connection string. When missing, it by default would use the public endpoint at https://api.github.com.
If you want to upload to a GitHub enterprise instance, then a respective `<scm>` connection string must be specified.
