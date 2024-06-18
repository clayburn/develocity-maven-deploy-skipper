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
public class UniqueDeploymentMarkerCachingListener implements DevelocityListener {

    @Override
    public void configure(DevelocityApi api, MavenSession session) {
        api.getBuildCache().registerMojoMetadataProvider(context -> {
            context.withPlugin("develocity-maven-goal-skipping-plugin", () -> {
                if ("com.example".equals(context.getMojoExecution().getPlugin().getGroupId())) {
                    handleInputs(session.getCurrentProject(), context);
                }
            });
        });
    }

    private void handleInputs(MavenProject project, MojoMetadataProvider.Context context) {
        context.inputs(inputs -> inputs.ignore("buildDirectory"));
        context.outputs(outputs -> outputs.cacheable("This goal should avoid writing out the marker file if the project artifact changed."));

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
}
