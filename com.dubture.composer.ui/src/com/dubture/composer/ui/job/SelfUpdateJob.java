package com.dubture.composer.ui.job;

import java.io.IOException;

import org.apache.commons.exec.ExecuteException;
import org.eclipse.core.resources.IProject;
import org.pdtextensions.core.launch.ScriptLauncher;

public class SelfUpdateJob extends ComposerJob {

	public SelfUpdateJob(IProject project) {
		super(project, "Self-Updating composer...");
	}

	@Override
	protected void launch(ScriptLauncher launcher) throws ExecuteException,
			IOException, InterruptedException {
		launcher.launch("selfupdate");
	}
}
