package com.dubture.composer.internal.core.resources;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IScriptProject;

import com.dubture.composer.core.log.Logger;
import com.dubture.composer.core.resources.IComposerProject;
import com.dubture.getcomposer.core.ComposerConstants;
import com.dubture.getcomposer.core.ComposerPackage;
import com.dubture.getcomposer.core.collection.ComposerPackages;
import com.dubture.getcomposer.core.collection.Psr;
import com.dubture.getcomposer.core.objects.Autoload;
import com.dubture.getcomposer.core.objects.Namespace;

public class ComposerProject implements IComposerProject {

	private IProject project;
	private IScriptProject scriptProject;
	private ComposerPackage composer = null;
	private IFile json = null;
	private String vendorDir = null;
	private IPath vendorPath = null;
	
	public ComposerProject(IProject project) {
		this.project = project;
		IFile file = project.getFile(ComposerConstants.COMPOSER_JSON);
		
		if (file != null && file.exists()) {
			composer = new ComposerPackage();
			try {
				composer.fromJson(file.getLocation().toFile());
			} catch (Exception e) {
			}
		}
	}
	
	public ComposerProject(IScriptProject project) {
		this(project.getProject());
		scriptProject = project;
	}
	
	@Override
	public IPath getFullPath() {
		return project.getFullPath();
	}
	
	@Override
	public String getVendorDir() {
		if (vendorDir == null) {
			if (composer != null && composer.getConfig() != null) {
				vendorDir = composer.getConfig().getVendorDir();	
			}
			
			if (vendorDir == null || vendorDir.trim().isEmpty()) {
				vendorDir = ComposerConstants.VENDOR_DIR_DEFAULT; // default
			}
		}
		
		return vendorDir;
	}

	@Override
	public IPath getVendorPath() {
		if (vendorPath == null) {
			IPath root = project.getLocation();
			String vendor = getVendorDir();

			if (root == null || root.segmentCount() <= 1) {
				throw new RuntimeException("Error getting composer vendor path");
			}

			vendorPath = root.removeLastSegments(1).addTrailingSeparator().append(vendor);
		}
		return vendorPath;
	}

	@Override
	public IFile getComposerJson() {
		if (json == null) {
			json = project.getFile(ComposerConstants.COMPOSER_JSON);
		}
		return json;
	}

	@Override
	public ComposerPackage getComposerPackage() {
		if (composer == null) {
			try {
				IFile json = getComposerJson();
				if (json == null) {
					return null;
				}
				composer = new ComposerPackage(json.getLocation().toFile());
			} catch (Exception e) {
			}
		}
		
		return composer;
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public IScriptProject getScriptProject() {
		if (scriptProject == null) {
			scriptProject = DLTKCore.create(project);
		}
		return scriptProject;
	}

	@Override
	public ComposerPackages getInstalledPackages() {
		String vendor = getVendorDir();
		ComposerPackages packages = new ComposerPackages();
		
		IFile installed = project.getFile(vendor + "/composer/installed.json");
		if (installed != null && installed.exists()) {
			packages.addAll(loadInstalled(installed));
		}
		
		return packages;
	}

	@Override
	public boolean isValidComposerJson() {
		IFile json = getComposerJson();
		if (json != null && json.exists()) {
			try {
				new ComposerPackage(json.getLocation().toFile());
				return true;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

//	@Override
//	public ComposerPackages getInstalledDevPackages() {
//		String vendor = getVendorDir();
//		ComposerPackages packages = new ComposerPackages();
//		
//		IFile installedDev = project.getFile(vendor + "/composer/installed_dev.json");
//		if (installedDev != null && installedDev.exists()) {
//			packages.addAll(loadInstalled(installedDev));
//		}
//		
//		return packages;
//	}
//
//	@Override
//	public ComposerPackages getAllInstalledPackages() {
//		ComposerPackages packages = getInstalledPackages();
//		packages.addAll(getInstalledDevPackages());
//		return packages;
//	}
	
	protected ComposerPackages loadInstalled(IFile installed) {
		try {
			if (installed.getLocation() != null) {
				return new ComposerPackages(installed.getLocation().toFile());
			}
		} catch (Exception e) {
			Logger.logException(e);
		}		
		
		return new ComposerPackages();
	}

	@Override
	public String getNamespace(IPath path) {
		Autoload autoload = getComposerPackage().getAutoload();

		// look for psr4 first
		String namespace = getPsrNamespace(path, autoload.getPsr4());
		
		if (namespace == null) {
			namespace = getPsrNamespace(path, autoload.getPsr0());
		}
		
		return namespace;
	}
	
	private String getPsrNamespace(IPath path, Psr psr) {
		IPath appendix = new Path("");
		while (!path.isEmpty()) {
			Namespace namespace = psr.getNamespaceForPath(path.addTrailingSeparator().toString());
			if (namespace == null) {
				namespace = psr.getNamespaceForPath(path.removeTrailingSeparator().toString());
			}
			
			if (namespace != null) {
				String nmspc = namespace.getNamespace();
				IPath nmspcPath = new Path(nmspc.replace("\\", "/"));
				
				int match = nmspcPath.matchingFirstSegments(appendix);
				appendix = appendix.removeFirstSegments(match);

				if (appendix.segmentCount() > 0) {
					nmspc += (!nmspc.isEmpty() ? "\\" : "") +
						appendix.removeTrailingSeparator().toString().replace("/", "\\"); 
				}
				
				
				nmspc = nmspc.replace("\\\\", "\\");
				if (nmspc.endsWith("\\")) {
					nmspc = nmspc.substring(0, nmspc.length() - 1);
				}
				
				return nmspc;
			}
			
			appendix = new Path(path.lastSegment() + "/" + appendix.toString());
			path = path.removeLastSegments(1);
		}
		
		return null;
	}
}
