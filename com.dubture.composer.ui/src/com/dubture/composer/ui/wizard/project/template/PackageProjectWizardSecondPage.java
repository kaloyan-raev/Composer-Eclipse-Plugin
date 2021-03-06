package com.dubture.composer.ui.wizard.project.template;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Observable;
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

import com.dubture.composer.core.log.Logger;
import com.dubture.composer.ui.ComposerUIPlugin;
import com.dubture.composer.ui.job.CreateProjectJob;
import com.dubture.composer.ui.job.CreateProjectJob.JobListener;
import com.dubture.composer.ui.wizard.AbstractWizardFirstPage;
import com.dubture.composer.ui.wizard.AbstractWizardSecondPage;
import com.dubture.getcomposer.core.ComposerPackage;

/**
 * 
 * @author Robert Gruendler <r.gruendler@gmail.com>
 *
 */
@SuppressWarnings("restriction")
public class PackageProjectWizardSecondPage extends AbstractWizardSecondPage implements IShellProvider, PackageFilterChangedListener {

	private PackageFilterViewer filter;

	public PackageProjectWizardSecondPage(AbstractWizardFirstPage mainPage, String title) {
		super(mainPage, title);
		setPageComplete(false);
	}
	
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		
		if (filter.getSelectedPackage() == null || filter.getSelectedPackage().getSelectedVersion() == null) {
			setPageComplete(false);
		}
	}
	
	@Override
	public void createControl(Composite parent) {
		filter = new PackageFilterViewer();
		filter.createControl(parent);
		filter.addChangeListener(this);
		filter.setMinimumHeight(400);
		setControl(filter.getControl());
		setPageComplete(false);
		setHelpContext(filter.getControl());
	}

	@Override
	public void update(Observable o, Object arg) {

	}

	@Override
	protected String getPageTitle() {
		return "Select package";
	}

	@Override
	protected String getPageDescription() {
		return "Search for a package to be used as the startingpoint for your new Composer project.";
	}
	
	@Override
	protected void beforeFinish(IProgressMonitor monitor) throws Exception {

		PackageFilterItem filterItem = filter.getSelectedPackage();
		final CountDownLatch latch = new CountDownLatch(1);
		
		monitor.beginTask("Initializing composer project", 2);
		monitor.worked(1);
		
		File file = new File(firstPage.getLocationURI());
		IPath location = new Path(file.toString());
		
		// let the create-project command handle folder creation
		if (firstPage.isInLocalServer()) {
			location = location.removeLastSegments(1);
		}
		
		CreateProjectJob projectJob = new CreateProjectJob(location, firstPage.nameGroup.getName(), filterItem.getPackage().getName(), filterItem.getSelectedVersion());
		projectJob.setJobListener(new JobListener() {
			@Override
			public void jobStarted() {
				latch.countDown();
			}

			@Override
			public void jobFinished(String projectName) {
				latch.countDown();
				refreshProject(projectName);
			}

			@Override
			public void jobFailed() {
				latch.countDown();
			}
		});
		
		projectJob.schedule();
		
		// we need to wait until the first page has started the 
		// create-project composer command and the command actually
		// wrote something to disk, otherwise the command will fail
		//
		// Note: The composer guys do not accept pull requests
		// to allow the create-project command be run on target paths
		// with files in it, so we have to use this workaround.
		try {
			latch.await();
		} catch (InterruptedException e) {
			
		}
		
		monitor.worked(1);
	}

	@Override
	protected void finishPage(IProgressMonitor monitor) throws Exception {
		try {
			PackageProjectWizardFirstPage page = (PackageProjectWizardFirstPage) firstPage;
			if (page.doesOverrideComposer()) {
				monitor.beginTask("Updating composer.json with new values", 1);
				ComposerPackage package1 = firstPage.getPackage();
				IFile file = getProject().getFile(new Path("composer.json"));
				if (file != null && file.exists()) {
					ByteArrayInputStream is = new ByteArrayInputStream(package1.toJson().getBytes());
					file.setContents(is, IResource.FORCE, monitor);
				}
			}
		} catch (Exception e) {
			Logger.logException(e);
		} finally {
			monitor.worked(1);
		}
	}
	
	
	@Override
	public void filterChanged(PackageFilterItem item) {
		
		if (item != null && item.getSelectedVersion() != null) {
			setPageComplete(true);
			return;
		}
		
		setPageComplete(false);
	}


	@Override
	protected void setHelpContext(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, ComposerUIPlugin.PLUGIN_ID + "." + "help_context_wizard_template_secondpage");
	}	
}
