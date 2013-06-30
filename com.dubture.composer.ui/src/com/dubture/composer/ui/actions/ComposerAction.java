package com.dubture.composer.ui.actions;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.ICommandService;

import com.dubture.composer.ui.editor.composer.ComposerFormEditor;

abstract public class ComposerAction extends Action {

	protected String id;
	protected Command command;
	protected IProject project;
	protected IWorkbenchPartSite site;
	
	public ComposerAction(IProject project, IWorkbenchPartSite site, String commandId) {
		this.project = project;
		this.site = site;
		id = commandId;
		command = ((ICommandService) site.getService(ICommandService.class)).getCommand(id);
	}

	@Override
	public String getToolTipText() {
		try {
			return command.getDescription();
		} catch (NotDefinedException e) {
			return null;
		}
	}
	
	protected void ensureSaved() {
		ComposerFormEditor editor = null;
		IEditorPart part = site.getPage().getActiveEditor();
		
		if (part instanceof ComposerFormEditor) {
			editor = (ComposerFormEditor)part;
		}

		if (editor != null && editor.isDirty()) {
			editor.doSave(new NullProgressMonitor());
		}
	}
}
