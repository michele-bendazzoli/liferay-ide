/*******************************************************************************
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 *******************************************************************************/

package com.liferay.ide.eclipse.portlet.core;

import com.liferay.ide.eclipse.core.model.AbstractEditingModel;
import com.liferay.ide.eclipse.core.model.IModelChangedEvent;
import com.liferay.ide.eclipse.core.model.ModelChangedEvent;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;

/**
 * @author Greg Amerson
 */
@SuppressWarnings("restriction")
public class PluginPackageModel extends AbstractEditingModel implements IPluginPackageModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	PropertiesConfiguration pluginPackageProperties;

	public PluginPackageModel(IFile file, IDocument document, boolean isReconciling) {
		super(document, isReconciling);

		try {
			pluginPackageProperties = new PluginPropertiesConfiguration();

			pluginPackageProperties.load(new File(file.getLocation().toOSString()));
		}
		catch (Exception e) {
			PortletCore.logError(e);
		}
	}

	public void addPortalDependencyJar(String jar) {
		if (jar == null || jar.isEmpty()) {
			return;
		}

		String existingPortalJars = pluginPackageProperties.getString(PROPERTY_PORTAL_DEPENDENCY_JARS, "");

		String[] existingJars = existingPortalJars.split(",");

		for (String existingJar : existingJars) {
			if (jar.equals(existingJar.trim())) {
				return;
			}
		}

		String newPortalJars = null;

		if (existingPortalJars.isEmpty()) {
			newPortalJars = jar;
		}
		else {
			newPortalJars = existingPortalJars + "," + jar;
		}

		pluginPackageProperties.setProperty(PROPERTY_PORTAL_DEPENDENCY_JARS, newPortalJars);

		flushProperties();

		fireModelChanged(new ModelChangedEvent(
			this, IModelChangedEvent.INSERT, newPortalJars.split(","), PROPERTY_PORTAL_DEPENDENCY_JARS));
	}

	public void addPortalDependencyTld(String tldFile) {
		if (tldFile == null || tldFile.isEmpty()) {
			return;
		}

		String existingPortalTlds = pluginPackageProperties.getString(PROPERTY_PORTAL_DEPENDENCY_TLDS, "");

		String[] existingTlds = existingPortalTlds.split(",");

		for (String existingTld : existingTlds) {
			if (tldFile.equals(existingTld.trim())) {
				return;
			}
		}

		String newPortalTlds = null;

		if (existingPortalTlds.isEmpty()) {
			newPortalTlds = tldFile;
		}
		else {
			newPortalTlds = existingPortalTlds + "," + tldFile;
		}

		pluginPackageProperties.setProperty(PROPERTY_PORTAL_DEPENDENCY_TLDS, newPortalTlds);

		flushProperties();

		fireModelChanged(new ModelChangedEvent(
			this, IModelChangedEvent.INSERT, newPortalTlds.split(","), PROPERTY_PORTAL_DEPENDENCY_TLDS));
	}

	@Override
	public void adjustOffsets(IDocument document)
		throws CoreException {

	}

	public void dispose() {
		pluginPackageProperties = null;

	}

	public String getAuthor() {
		return getStringProperty(PROPERTY_AUTHOR);
	}

	public Boolean getBooleanProperty(String propertyName) {
		if (pluginPackageProperties != null) {
			if (pluginPackageProperties.getProperty(propertyName) != null) {
				return pluginPackageProperties.getBoolean(propertyName);
			}
		}

		return null;
	}

	public String getChangeLog() {
		return getStringProperty(PROPERTY_CHANGE_LOG);
	}

	public String getLicenses() {
		return getStringProperty(PROPERTY_LICENSES);
	}

	public String getModuleGroupId() {
		return getStringProperty(PROPERTY_MODULE_GROUP_ID);
	}

	public String getModuleIncrementalVersion() {
		return getStringProperty(PROPERTY_MODULE_INCREMENTAL_VERSION);
	}

	public String getName() {
		return getStringProperty(PROPERTY_NAME);
	}

	public String getPageUrl() {
		return getStringProperty(PROPERTY_PAGE_URL);
	}

	public String[] getPortalDependencyJars() {
		String portalJars = pluginPackageProperties.getString(PROPERTY_PORTAL_DEPENDENCY_JARS, null);

		if (portalJars != null) {
			return portalJars.split(",");
		}
		else {
			return new String[0];
		}
	}

	public String[] getPortalDependencyTlds() {
		String portalTlds = pluginPackageProperties.getString(PROPERTY_PORTAL_DEPENDENCY_TLDS, null);

		if (portalTlds != null) {
			return portalTlds.split(",");
		}
		else {
			return new String[0];
		}
	}

	public String getShortDescription() {
		return getStringProperty(PROPERTY_SHORT_DESCRIPTION);
	}

	public String getStringProperty(String propertyName) {
		if (pluginPackageProperties != null) {
			if (pluginPackageProperties.getProperty(propertyName) != null) {
				return pluginPackageProperties.getString(propertyName);
			}
		}

		return null;
	}

	public String getTags() {
		return getStringProperty(PROPERTY_TAGS);
	}

	public boolean isAdapterForType(Object type) {
		return type != null && INodeAdapter.class == type;
	}

	public Boolean isSpeedFiltersEnabled() {
		return getBooleanProperty(PROPERTY_SPEED_FILTERS_ENABLED);
	}

	public void load(InputStream source, boolean outOfSync)
		throws CoreException {

		pluginPackageProperties.clear();

		try {
			pluginPackageProperties.load(source);

			flushProperties();

			fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, null, null));
		}
		catch (ConfigurationException e) {
			throw new CoreException(PortletCore.createErrorStatus(e));
		}
	}

	public void removePortalDependencyJars(String[] removedJars) {
		String portalJars = pluginPackageProperties.getString(PROPERTY_PORTAL_DEPENDENCY_JARS, null);

		List<String> updatedJars = new ArrayList<String>();

		String[] jars = portalJars.split(",");

		for (String jar : jars) {
			for (String removedJar : removedJars) {
				if (!(jar.trim().equals(removedJar.trim()))) {
					updatedJars.add(jar);
				}
			}
		}

		pluginPackageProperties.setProperty(PROPERTY_PORTAL_DEPENDENCY_JARS, "");

		for (String updatedJar : updatedJars) {
			addPortalDependencyJar(updatedJar);
		}

		flushProperties();

		fireModelChanged(new ModelChangedEvent(
			this, IModelChangedEvent.REMOVE, updatedJars.toArray(), PROPERTY_PORTAL_DEPENDENCY_JARS));
	}

	public void removePortalDependencyTlds(String[] removedTlds) {
		String portalTlds = pluginPackageProperties.getString(PROPERTY_PORTAL_DEPENDENCY_TLDS, null);

		List<String> updatedTlds = new ArrayList<String>();

		String[] tlds = portalTlds.split(",");

		for (String tld : tlds) {
			for (String removedTld : removedTlds) {
				if (!(tld.trim().equals(removedTld.trim()))) {
					updatedTlds.add(tld);
				}
			}
		}

		pluginPackageProperties.setProperty(PROPERTY_PORTAL_DEPENDENCY_TLDS, "");

		for (String updatedTld : updatedTlds) {
			addPortalDependencyTld(updatedTld);
		}

		flushProperties();

		fireModelChanged(new ModelChangedEvent(
			this, IModelChangedEvent.REMOVE, updatedTlds.toArray(), PROPERTY_PORTAL_DEPENDENCY_TLDS));
	}

	public void setAuthor(String author) {
		setProperty(PROPERTY_AUTHOR, author);
	}

	public void setChangeLog(String changeLog) {
		setProperty(PROPERTY_CHANGE_LOG, changeLog);
	}

	public void setLicenses(String licenses) {
		setProperty(PROPERTY_LICENSES, licenses);
	}

	public void setModuleGroupId(String moduleGroupId) {
		setProperty(PROPERTY_MODULE_GROUP_ID, moduleGroupId);
	}

	public void setModuleIncrementalVersion(String moduleIncrementalVersion) {
		setProperty(PROPERTY_MODULE_INCREMENTAL_VERSION, moduleIncrementalVersion);
	}

	public void setName(String name) {
		setProperty(PROPERTY_NAME, name);
	}

	public void setPageUrl(String pageUrl) {
		setProperty(PROPERTY_PAGE_URL, pageUrl);
	}

	public void setProperty(String propertyName, Object propertyValue) {
		Object oldValue = pluginPackageProperties.getProperty(propertyName);

		pluginPackageProperties.setProperty(propertyName, propertyValue);

		flushProperties();

		fireModelChanged(new ModelChangedEvent(this, null, propertyName, oldValue, propertyValue));
	}

	public void setShortDescription(String desc) {
		setProperty(PROPERTY_SHORT_DESCRIPTION, desc);
	}

	public void setSpeedFiltersEnabled(boolean enabled) {
		Boolean oldValue = isSpeedFiltersEnabled();

		if (enabled) {
			pluginPackageProperties.setProperty(PROPERTY_SPEED_FILTERS_ENABLED, enabled);
		}
		else {
			pluginPackageProperties.clearProperty(PROPERTY_SPEED_FILTERS_ENABLED);
		}

		flushProperties();

		fireModelChanged(new ModelChangedEvent(this, null, PROPERTY_SPEED_FILTERS_ENABLED, oldValue, enabled));
	}

	public void setTags(String tags) {
		setProperty(PROPERTY_TAGS, tags);
	}

	protected void flushProperties() {
		StringWriter output = new StringWriter();

		try {
			pluginPackageProperties.save(output);

			this.getDocument().set(output.toString());
		}
		catch (ConfigurationException e) {
		}
	}
}