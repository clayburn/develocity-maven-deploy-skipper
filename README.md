# Unique Deployment Skipping

This is an example project showing how to use the Develocity Maven cache to prevent deployments that have already been deployed.

This works by creating a cacheable goal that writes out a marker file as an untracked output.

## Project Layout

- plugin - The Maven plugin that provides the cacheable goal that writes the marker file
- extension - Contains a DevelocityListener that defines the cacheability of the custom goal
- example - An example project that demonstrates the functionality

## To run

- Install both the plugin and extension to Maven Local
- Run the example project twice, observing the cache hits on the second run