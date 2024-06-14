package com.example.extension;

import com.gradle.develocity.agent.maven.api.DevelocityApi;
import com.gradle.develocity.agent.maven.api.DevelocityListener;
import com.gradle.develocity.agent.maven.api.cache.MojoMetadataProvider;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.artifact.Artifact;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Component(role = DevelocityListener.class)
public class GoalExecutionListener implements DevelocityListener {

    @Override
    public void configure(DevelocityApi api, MavenSession session) {
        api.getBuildCache().registerMojoMetadataProvider(context -> {
            context.withPlugin("maven-deploy-plugin", () -> {
                if ("org.apache.maven.plugins".equals(context.getMojoExecution().getPlugin().getGroupId())) {
                    handleDeployMojoInputs(context);
                    configureDeployCachingFromCompleteJar(session.getCurrentProject(), context);
                }
            });
        });
    }

    private void configureDeployCachingFromCompleteJar(MavenProject project, MojoMetadataProvider.Context context) {
        File pomArtifact = RepositoryUtils.toArtifact(new ProjectArtifact(project)).getFile();
        File projectArtifact = RepositoryUtils.toArtifact(project.getArtifact()).getFile();
        List<File> attachedArtifacts = project.getAttachedArtifacts().stream()
                .map(RepositoryUtils::toArtifact)
                .map(Artifact::getFile)
                .collect(Collectors.toList());

        context.inputs(inputs -> {
            inputs.fileSet( "pom", pomArtifact, fileSet ->
                    fileSet.normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH));
            inputs.fileSet( "projectArtifact", projectArtifact, fileSet ->
                    fileSet.normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.CLASSPATH));
            inputs.fileSet( "attachedArtifacts", attachedArtifacts, fileSet ->
                    fileSet.normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH));
        });
    }

    private void handleDeployMojoInputs(MojoMetadataProvider.Context context) {
        Object deployMojo = context.getUnderlyingObject();

        try {
            if (getBooleanField(deployMojo, "deployAtEnd")) {
                context.outputs(outputs -> outputs.notCacheableBecause("deployAtEnd is true"));
                return;
            }
        } catch (ReflectiveOperationException e) {
            context.outputs(outputs -> outputs.notCacheableBecause("could not determine value of deployAtEnd"));
            return;
        }

        context.inputs(inputs -> {
            inputs.properties(
                    "allowIncompleteProjects",
                    "altDeploymentRepository",
                    "altReleaseDeploymentRepository",
                    "altSnapshotDeploymentRepository",
                    "offline",
                    "skip"
            );
            inputs.ignore(
                    "deployAtEnd",
                    "pluginDescriptor",
                    "project",
                    "reactorProjects",
                    "retryFailedDeploymentCount",
                    "session"
            );
        });

        context.outputs(outputs -> outputs.cacheable("This SNAPSHOT jar may previously have been published to an artifact repository."));
    }

    private boolean getBooleanField(Object mojo, String fieldName) throws ReflectiveOperationException {
        java.lang.reflect.Field field = mojo.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (boolean) field.get(mojo);
    }
}
