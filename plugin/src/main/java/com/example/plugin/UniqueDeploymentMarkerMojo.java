package com.example.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Mojo(name = "mark-unique-deployment", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class UniqueDeploymentMarkerMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private String buildDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Files.createFile(Paths.get(buildDirectory, "unique-deployment-marker"));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write deploy-cacher-marker file", e);
        }
    }
}
