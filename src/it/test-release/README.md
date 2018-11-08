# How this works

* The `maven-invoker-plugin` uses it's own `settings.xml` which is merged with the one from `~/.m2/settings.xml`.
* The credentials for deploying toe Github are under the `~/.m2/settings.xml`, so as to not having to add them
  as sources.
* The `repositoryId` and expected `serverId` in the `~/.m2/settings.xml` file are as follows
```
        <repositoryId>jutzig/github-release-plugin</repositoryId>
        <serverId>jutzig/github-release-plugin</serverId>

```
