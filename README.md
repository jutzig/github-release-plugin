github-release-plugin
=====================

uses the github release api to upload files

To use the plugin you need to configure it in your `pom.xml` like so

```
<plugin>
    <groupId>de.jutzig</groupId>
    <artifactId>github-release-plugin</artifactId>
    <version>1.1.1</version>
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

By default, the plugin will look for your github credentials in your maven `settings.xml`. Example
```
<servers>
    <server>
        <id>github</id>
        <username>GITHUB_USERNAME</username>
        <password>GITHUB_PASSWORD</password>
    </server>
</servers>
```

These credentials can be overridden by setting `username` and `password` as system properties.

Thanks to a contribution from rowanseymour you can also use your API token by adding it as `<privateKey>` to your server definition in the `settings.xml`.

Additional Parameters:

 * @-Dgithub.draft=true@ creates the release in draft state
 * @-Dgithub.commitish=release/1.0.0@ allows to specify a commitsh

The plugin is available on Maven central
