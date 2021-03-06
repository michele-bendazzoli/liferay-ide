/*******************************************************************************
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
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

package com.liferay.ide.eclipse.layouttpl.ui.wizard;

import com.liferay.ide.eclipse.core.util.CoreUtil;
import com.liferay.ide.eclipse.layouttpl.core.operation.INewLayoutTplDataModelProperties;
import com.liferay.ide.eclipse.layouttpl.ui.LayoutTplUI;
import com.liferay.ide.eclipse.layouttpl.ui.util.LayoutTplUtil;
import com.liferay.ide.eclipse.ui.util.SWTUtil;
import com.liferay.ide.eclipse.ui.wizard.LiferayDataModelWizardPage;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.frameworks.datamodel.DataModelEvent;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelListener;

/**
 * @author Greg Amerson
 */
@SuppressWarnings("restriction")
public class NewLayoutTplWizardPage extends LiferayDataModelWizardPage implements INewLayoutTplDataModelProperties {

	protected Text id;

	protected Text name;

	protected String projectName;

	protected Combo projectNameCombo;

	protected Label projectNameLabel;

	protected Text templateFile;

	protected Text thumbnailFile;

	protected Text wapTemplateFile;

	public NewLayoutTplWizardPage(IDataModel dataModel, String pageName) {
		super(dataModel, pageName, "Create Layout Template", LayoutTplUI.imageDescriptorFromPlugin(
			LayoutTplUI.PLUGIN_ID, "/icons/wizban/layout_template_wiz.png"));

		setDescription("Create a Liferay layout template.");
	}

	protected void createProjectNameGroup(Composite parent) {
		projectNameLabel = new Label(parent, SWT.NONE);
		projectNameLabel.setText("Layout plugin project:"); //$NON-NLS-1$
		projectNameLabel.setLayoutData(new GridData());

		// set up project name entry field
		projectNameCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 300;
		data.horizontalSpan = 1;
		data.grabExcessHorizontalSpace = true;
		projectNameCombo.setLayoutData(data);
		synchHelper.synchCombo(projectNameCombo, PROJECT_NAME, null);

		String initialProjectName = initializeProjectList(projectNameCombo, model);
		if (projectName == null && initialProjectName != null) {
			projectName = initialProjectName;
		}
	}

	protected void createTemplateInfoGroup(Composite parent) {
		SWTUtil.createLabel(parent, SWT.RIGHT, "Name:", 1);

		this.name = SWTUtil.createText(parent, 1);
		this.synchHelper.synchText(name, LAYOUT_TEMPLATE_NAME, null);
		SWTUtil.createLabel(parent, "", 1);

		SWTUtil.createLabel(parent, SWT.RIGHT, "Id:", 1);

		this.id = SWTUtil.createText(parent, 1);
		this.synchHelper.synchText(id, LAYOUT_TEMPLATE_ID, null);
		SWTUtil.createLabel(parent, "", 1);

		SWTUtil.createLabel(parent, SWT.RIGHT, "Template file:", 1);

		this.templateFile = SWTUtil.createText(parent, 1);
		this.synchHelper.synchText(templateFile, LAYOUT_TEMPLATE_FILE, null);

		Button templateFileBrowse = SWTUtil.createPushButton(parent, "Browse...", null);

		templateFileBrowse.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleFileBrowseButton(
					NewLayoutTplWizardPage.this.templateFile, "Template file selection", "Choose a template file: ");
			}

		});

		SWTUtil.createLabel(parent, SWT.RIGHT, "WAP template file:", 1);

		this.wapTemplateFile = SWTUtil.createText(parent, 1);
		this.synchHelper.synchText(wapTemplateFile, LAYOUT_WAP_TEMPLATE_FILE, null);

		Button wapTemplateFileBrowse = SWTUtil.createPushButton(parent, "Browse...", null);

		wapTemplateFileBrowse.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleFileBrowseButton(
					NewLayoutTplWizardPage.this.wapTemplateFile, "WAP template file selection",
					"Choose a WAP template file: ");
			}

		});

		SWTUtil.createLabel(parent, SWT.RIGHT, "Thumbnail file:", 1);

		this.thumbnailFile = SWTUtil.createText(parent, 1);
		this.synchHelper.synchText(thumbnailFile, LAYOUT_THUMBNAIL_FILE, null);

		Button thumbnailFileBrowse = SWTUtil.createPushButton(parent, "Browse...", null);

		thumbnailFileBrowse.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleFileBrowseButton(
					NewLayoutTplWizardPage.this.thumbnailFile, "WAP template file selection",
					"Choose a thumbnail file: ");
			}

		});

		synchHelper.getDataModel().addListener(new IDataModelListener() {

			public void propertyChanged(DataModelEvent event) {
				if (LAYOUT_TEMPLATE_NAME.equals(event.getPropertyName()) ||
					LAYOUT_TEMPLATE_ID.equals(event.getPropertyName())) {

					synchHelper.synchAllUIWithModel();
				}
			}
		});
	}

	@Override
	protected Composite createTopLevelComposite(Composite parent) {
		Composite topComposite = SWTUtil.createTopComposite(parent, 3);

		createProjectNameGroup(topComposite);

		SWTUtil.createSeparator(topComposite, 3);

		createTemplateInfoGroup(topComposite);

		return topComposite;
	}

	@Override
	protected void enter() {
		super.enter();

		validatePage(true);
	}

	@Override
	protected IFolder getDocroot() {
		return CoreUtil.getDocroot(getDataModel().getStringProperty(PROJECT_NAME));
	}

	@Override
	protected String[] getValidationPropertyNames() {
		return new String[] {
			PROJECT_NAME, LAYOUT_TEMPLATE_NAME, LAYOUT_TEMPLATE_ID, LAYOUT_TEMPLATE_FILE, LAYOUT_WAP_TEMPLATE_FILE,
			LAYOUT_THUMBNAIL_FILE
		};
	}

	@Override
	protected boolean isProjectValid(IProject project) {
		return LayoutTplUtil.isLayoutTplProject(project);
	}

	@Override
	protected boolean showValidationErrorsOnEnter() {
		return true;
	}

}
