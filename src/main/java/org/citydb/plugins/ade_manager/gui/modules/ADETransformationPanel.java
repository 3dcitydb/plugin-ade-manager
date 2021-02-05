/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2020
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.citydb.plugins.ade_manager.gui.modules;

import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DatabaseOperationType;
import org.citydb.event.Event;
import org.citydb.gui.components.common.TitledPanel;
import org.citydb.gui.util.GuiUtil;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citydb.plugins.ade_manager.gui.popup.StatusDialog;
import org.citydb.plugins.ade_manager.gui.table.ADESchemaNamespaceRow;
import org.citydb.plugins.ade_manager.gui.table.TableModel;
import org.citydb.plugins.ade_manager.transformation.TransformationController;
import org.citydb.plugins.ade_manager.transformation.TransformationException;
import org.citydb.plugins.ade_manager.util.GlobalConstants;
import org.citydb.plugins.ade_manager.util.Translator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ADETransformationPanel extends OperationModuleView {	
	private JPanel component;	
	private TitledPanel browseXMLSchemaPanel;
	private final JTextField browseXMLSchemaText = new JTextField();
	private final JButton browseXMLSchemaButton = new JButton();
	private final JButton readXMLSchemaButton = new JButton();
	private JTable schemaTable;
	private TitledPanel namePanel;
	private final JTextField nameInputField = new JTextField();
	private TitledPanel descriptionPanel;
	private final JTextField descriptionInputField = new JTextField();
	private TitledPanel versionPanel;
	private final JTextField versionInputField = new JTextField();
	private TitledPanel dbPrefixPanel;
	private final JTextField dbPrefixInputField = new JTextField();
	private TitledPanel initObjectClassIdPanel;
	private final JFormattedTextField initObjectClassIdInputField = new JFormattedTextField(new DecimalFormat("##########"));
	private TitledPanel transformationOutputPanel;
	private final JTextField browseOutputText = new JTextField();
	private final JButton browserOutputButton = new JButton();
	private final JButton transformAndExportButton = new JButton();
	private final TableModel<ADESchemaNamespaceRow> schemaTableModel = new TableModel<>(ADESchemaNamespaceRow.getColumnNames());

	private final TransformationController adeTransformer;

	public ADETransformationPanel(ADEManagerPanel parentPanel, ConfigImpl config) {
		super(parentPanel, config);		
		this.adeTransformer = new TransformationController(config);					
		initGui();
	}

	private void initGui() {	
		readXMLSchemaButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		transformAndExportButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
	
		// Input panel
		browseXMLSchemaPanel = new TitledPanel().withMargin(new Insets(BORDER_THICKNESS,0,BORDER_THICKNESS,0));
		JPanel browseXMLSchemaContentPanel = new JPanel();
		browseXMLSchemaContentPanel.setLayout(new GridBagLayout());
		browseXMLSchemaContentPanel.add(browseXMLSchemaText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,0,BORDER_THICKNESS,BORDER_THICKNESS));
		browseXMLSchemaContentPanel.add(browseXMLSchemaButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,0));
		browseXMLSchemaPanel.build(browseXMLSchemaContentPanel);
		browseXMLSchemaPanel.remove(browseXMLSchemaContentPanel);
		browseXMLSchemaPanel.add(browseXMLSchemaContentPanel, GuiUtil.setConstraints(0, 1, 1, 1, GridBagConstraints.BOTH, 0, 0, BORDER_THICKNESS, 0));

		// Schema table panel
		schemaTable = new JTable(schemaTableModel);
		schemaTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		schemaTable.setCellSelectionEnabled(false);
		schemaTable.setColumnSelectionAllowed(false);
		schemaTable.setRowSelectionAllowed(true);
		schemaTable.setRowHeight(20);
		JScrollPane schemaPanel = new JScrollPane(schemaTable);
		schemaPanel.setPreferredSize(new Dimension(browseXMLSchemaText.getPreferredSize().width, 200));

		// metadata parameters
		namePanel = new TitledPanel().withMargin(new Insets(0,BORDER_THICKNESS,BORDER_THICKNESS,0));
		JPanel nameContentPanel = new JPanel();
		nameContentPanel.setLayout(new GridBagLayout());
		nameContentPanel.add(nameInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, 0));
		nameInputField.setColumns(10);
		namePanel.build(nameContentPanel);
		namePanel.remove(nameContentPanel);
		namePanel.add(nameContentPanel, GuiUtil.setConstraints(0, 1, 1, 1, GridBagConstraints.BOTH, 0, 0, BORDER_THICKNESS, 0));

		descriptionPanel = new TitledPanel().withMargin(new Insets(0,BORDER_THICKNESS,BORDER_THICKNESS,0));
		JPanel descriptionContentPanel = new JPanel();
		descriptionContentPanel.setLayout(new GridBagLayout());
		descriptionContentPanel.add(descriptionInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, 0));
		descriptionInputField.setColumns(10);
		descriptionPanel.build(descriptionContentPanel);
		descriptionPanel.remove(descriptionContentPanel);
		descriptionPanel.add(descriptionContentPanel, GuiUtil.setConstraints(0, 1, 1, 1, GridBagConstraints.BOTH, 0, 0, BORDER_THICKNESS, 0));
		
		versionPanel = new TitledPanel().withMargin(new Insets(0,BORDER_THICKNESS,BORDER_THICKNESS,0));
		JPanel versionContentPanel = new JPanel();
		versionContentPanel.setLayout(new GridBagLayout());
		versionContentPanel.add(versionInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, 0));
		versionInputField.setColumns(10);
		versionPanel.build(versionContentPanel);
		versionPanel.remove(versionContentPanel);
		versionPanel.add(versionContentPanel, GuiUtil.setConstraints(0, 1, 1, 1, GridBagConstraints.BOTH, 0, 0, BORDER_THICKNESS, 0));
		
		dbPrefixPanel = new TitledPanel().withMargin(new Insets(0,BORDER_THICKNESS,BORDER_THICKNESS,0));
		JPanel dbPrefixContentPanel = new JPanel();
		dbPrefixContentPanel.setLayout(new GridBagLayout());
		dbPrefixContentPanel.add(dbPrefixInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, 0));
		dbPrefixInputField.setColumns(10);
		dbPrefixPanel.build(dbPrefixContentPanel);
		dbPrefixPanel.remove(dbPrefixContentPanel);
		dbPrefixPanel.add(dbPrefixContentPanel, GuiUtil.setConstraints(0, 1, 1, 1, GridBagConstraints.BOTH, 0, 0, BORDER_THICKNESS, 0));

		initObjectClassIdPanel = new TitledPanel().withMargin(new Insets(0,BORDER_THICKNESS,BORDER_THICKNESS,0));
		JPanel initObjectClassIdContentPanel = new JPanel();
		initObjectClassIdContentPanel.setLayout(new GridBagLayout());
		initObjectClassIdContentPanel.add(initObjectClassIdInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, 0));
		initObjectClassIdInputField.setColumns(10);
		initObjectClassIdPanel.build(initObjectClassIdContentPanel);
		initObjectClassIdPanel.remove(initObjectClassIdContentPanel);
		initObjectClassIdPanel.add(initObjectClassIdContentPanel, GuiUtil.setConstraints(0, 1, 1, 1, GridBagConstraints.BOTH, 0, 0, BORDER_THICKNESS, 0));

		Box box = Box.createVerticalBox();	
		box.add(namePanel);
		box.add(descriptionPanel);
		box.add(versionPanel);
		box.add(dbPrefixPanel);
		box.add(initObjectClassIdPanel);
		
		JPanel metadataInputPanel = new JPanel(new BorderLayout());
		metadataInputPanel.add(box, BorderLayout.NORTH);
		
		JPanel schemaAndMetadataPanel = new JPanel();
		schemaAndMetadataPanel.setLayout(new GridBagLayout());
		schemaAndMetadataPanel.add(schemaPanel, GuiUtil.setConstraints(0,0,0.8,0,GridBagConstraints.BOTH,BORDER_THICKNESS,0,BORDER_THICKNESS,BORDER_THICKNESS));
		schemaAndMetadataPanel.add(metadataInputPanel, GuiUtil.setConstraints(2,0,0.2,0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,0));
	
		// Export panel
		transformationOutputPanel = new TitledPanel().withMargin(new Insets(BORDER_THICKNESS,0,BORDER_THICKNESS,0));
		JPanel transformationOutputContentPanel = new JPanel();
		transformationOutputContentPanel.setLayout(new GridBagLayout());
		transformationOutputContentPanel.add(browseOutputText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,0,BORDER_THICKNESS,BORDER_THICKNESS));
		transformationOutputContentPanel.add(browserOutputButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,0));
		transformationOutputPanel.build(transformationOutputContentPanel);
		transformationOutputPanel.remove(transformationOutputContentPanel);
		transformationOutputPanel.add(transformationOutputContentPanel, GuiUtil.setConstraints(0, 1, 1, 1, GridBagConstraints.BOTH, 0, 0, BORDER_THICKNESS, 0));

		component = new JPanel();
		component.setLayout(new GridBagLayout());
		
		int index = 0;		
		component.add(browseXMLSchemaPanel, GuiUtil.setConstraints(0,index++,0.0,0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS*2,BORDER_THICKNESS));
		component.add(readXMLSchemaButton, GuiUtil.setConstraints(0,index++,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS*2,BORDER_THICKNESS));
		component.add(schemaAndMetadataPanel, GuiUtil.setConstraints(0,index++,0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS*3,BORDER_THICKNESS,BORDER_THICKNESS*2,BORDER_THICKNESS));
		component.add(transformationOutputPanel, GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS*2,BORDER_THICKNESS));
		component.add(transformAndExportButton, GuiUtil.setConstraints(0,index++,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS*3,BORDER_THICKNESS,BORDER_THICKNESS*2,BORDER_THICKNESS));

		browseXMLSchemaButton.addActionListener(e -> browserXMLSchemaFile());
		
		readXMLSchemaButton.addActionListener(e -> {
			Thread thread = new Thread(() -> parseADESchema());
			thread.setDaemon(true);
			thread.start();
		});
		
		browserOutputButton.addActionListener(e -> browseTransformationOutputDirectory());
				
		transformAndExportButton.addActionListener(e -> {
			Thread thread = new Thread(() -> transformADESchema());
			thread.setDaemon(true);
			thread.start();
		});
		
		setEnabledMetadataSettings(false);
	}

	// localized Labels and Strings
	public void doTranslation() {	
		browseXMLSchemaPanel.setTitle(Translator.I18N.getString("ade_manager.transformationPanel.browseXMLSchemaPanel.border"));

		namePanel.setTitle(
				MessageFormat.format(Translator.I18N.getString("ade_manager.transformationPanel.namePanel.border"),
				new Object[] { String.valueOf(GlobalConstants.MAX_ADE_NAME_LENGTH) }));

		descriptionPanel.setTitle(
				MessageFormat.format(Translator.I18N.getString("ade_manager.transformationPanel.descriptionPanel.border"),
						new Object[] { String.valueOf(GlobalConstants.MAX_ADE_DESCRIPTION_LENGTH) }));

		versionPanel.setTitle(
				MessageFormat.format(Translator.I18N.getString("ade_manager.transformationPanel.versionPanel.border"),
						new Object[] { String.valueOf(GlobalConstants.MAX_ADE_VERSION_LENGTH) }));
		
		dbPrefixPanel.setTitle(
				MessageFormat.format(Translator.I18N.getString("ade_manager.transformationPanel.dbPrefixPanel.border"),
						new Object[] { GlobalConstants.MAX_DB_PREFIX_LENGTH }));

		initObjectClassIdPanel.setTitle(
				MessageFormat.format(Translator.I18N.getString("ade_manager.transformationPanel.initObjectClassIdPanel.border"),
						new Object[] { String.valueOf(GlobalConstants.MIN_ADE_OBJECTCLASSID) }));
		
		browseXMLSchemaButton.setText(Language.I18N.getString("common.button.browse"));
		readXMLSchemaButton.setText(Translator.I18N.getString("ade_manager.transformationPanel.button.readXMLSchema"));
		transformationOutputPanel.setTitle(Translator.I18N.getString("ade_manager.transformationPanel.transformationOutputPanel.border"));
		browserOutputButton.setText(Language.I18N.getString("common.button.browse"));
		transformAndExportButton.setText(Translator.I18N.getString("ade_manager.transformationPanel.button.transformAndExport"));
	}

	public void loadSettings() {
		browseXMLSchemaText.setText(config.getXMLschemaInputPath());
		browseOutputText.setText(config.getTransformationOutputPath());
		nameInputField.setText(config.getAdeName());
		descriptionInputField.setText(config.getAdeDescription());
		versionInputField.setText(config.getAdeVersion());
		dbPrefixInputField.setText(config.getAdeDbPrefix());
		initObjectClassIdInputField.setValue(config.getInitialObjectclassId());
	}

	public void setSettings() {
		config.setXMLschemaInputPath(browseXMLSchemaText.getText());
		config.setTransformationOutputPath(browseOutputText.getText());
		config.setAdeName(nameInputField.getText());
		config.setAdeDescription(descriptionInputField.getText());
		config.setAdeVersion(versionInputField.getText());
		config.setAdeDbPrefix(dbPrefixInputField.getText());
		config.setInitialObjectclassId(((Number)initObjectClassIdInputField.getValue()).intValue());
	}

	private void browserXMLSchemaFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setDialogTitle(Translator.I18N.getString("ade_manager.transformationPanel.inputFileChooser.title"));
		
		FileNameExtensionFilter filter = new FileNameExtensionFilter(Translator.I18N.getString("ade_manager.transformationPanel.inputFileChooser.filter.description"), "xsd");
		chooser.addChoosableFileFilter(filter);
		chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
		chooser.setFileFilter(filter);
	
		if (!browseXMLSchemaText.getText().trim().isEmpty())
			chooser.setCurrentDirectory(new File(browseXMLSchemaText.getText()));
	
		int result = chooser.showOpenDialog(parentPanel.getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		browseXMLSchemaText.setText(chooser.getSelectedFile().toString());
	}
	
	private void setEnabledMetadataSettings(boolean enable) {
		nameInputField.setEnabled(enable);
		descriptionInputField.setEnabled(enable);
		versionInputField.setEnabled(enable);
		dbPrefixInputField.setEnabled(enable);
		initObjectClassIdInputField.setEnabled(enable);
		browseOutputText.setEnabled(enable);
		browserOutputButton.setEnabled(enable);
		transformAndExportButton.setEnabled(enable);
	}

	private void browseTransformationOutputDirectory() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(Translator.I18N.getString("ade_manager.transformationPanel.outputFileChooser.title"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setCurrentDirectory(new File(browseOutputText.getText()));
	
		int result = chooser.showSaveDialog(parentPanel.getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		String browseString = chooser.getSelectedFile().toString();
		if (!browseString.isEmpty())
			browseOutputText.setText(browseString);
	}

	private void parseADESchema() {
		viewController.clearConsole();
		schemaTableModel.reset();
		
		final StatusDialog statusDialog = new StatusDialog(viewController.getTopFrame(), 
				"ADE-Parse",
				"Reading and parsing ADE XML schema...");
		
		SwingUtilities.invokeLater(() -> {
			statusDialog.setLocationRelativeTo(viewController.getTopFrame());
			statusDialog.setVisible(true);
		});
		
		LOG.info("Start parsing ADE XML schema...");
		String xmlSchemaPath = browseXMLSchemaText.getText();
		try {			
			List<String> adeNamespaces = adeTransformer.getADENamespacesFromXMLSchema(xmlSchemaPath);		
			for (String schemaNamespace : adeNamespaces) {
				ADESchemaNamespaceRow schemaColumn = new ADESchemaNamespaceRow(schemaNamespace);
				schemaTableModel.addNewRow(schemaColumn);
			}			
			LOG.info("Parsing ADE XML schema completed.");
			
			setEnabledMetadataSettings(true);
		} catch (TransformationException e) {
			printErrorMessage("Failed to read and parse ADE schema", e);
		} finally {
			SwingUtilities.invokeLater(() -> statusDialog.dispose());
		}
	}

	private void transformADESchema() {	
		setSettings();
	
		String adeName = config.getAdeName();
		if (adeName.trim().length() == 0) {
			viewController.errorMessage("Incomplete Information", "Please enter a name for the ADE");
			return;
		}
		
		if (adeName.trim().length() > GlobalConstants.MAX_ADE_NAME_LENGTH) {
			viewController.errorMessage("Incorrect Information",
					"The ADE name should not exceed " + GlobalConstants.MAX_ADE_NAME_LENGTH + " characters");
			return;
		}
		
		String adeDescription = config.getAdeDescription();
		if (adeDescription.trim().length() == 0) {
			viewController.errorMessage("Incomplete Information", "Please enter a description for the ADE");
			return;
		}
		
		if (adeDescription.trim().length() > GlobalConstants.MAX_ADE_NAME_LENGTH) {
			viewController.errorMessage("Incorrect Information",
					"The ADE description should not exceed " + GlobalConstants.MAX_ADE_DESCRIPTION_LENGTH + " characters");
			return;
		}
		
		String adeVersion = config.getAdeVersion();
		if (adeVersion.trim().length() == 0) {
			viewController.errorMessage("Incomplete Information", "Please enter a version for the ADE");
			return;
		}
		
		if (adeVersion.trim().length() > GlobalConstants.MAX_ADE_VERSION_LENGTH) {
			viewController.errorMessage("Incorrect Information",
					"The ADE version should not exceed " + GlobalConstants.MAX_ADE_VERSION_LENGTH + " characters");
			return;
		}
		
		String dbPrefix = config.getAdeDbPrefix();
		if (dbPrefix.trim().length() == 0) {
			viewController.errorMessage("Incomplete Information", "Please enter a DB_Prefix for the target ADE");
			return;
		}
		
		if (Character.isDigit(dbPrefix.trim().charAt(0))) {
			viewController.errorMessage("Incorrect Information", "The DB_Prefix should not start with a digit");
			return;
		}
		
		if (!Pattern.compile("[a-zA-Z0-9]*").matcher(dbPrefix.trim()).matches()) {
			viewController.errorMessage("Incorrect Information", "The DB_Prefix should not contain a special character");
			return;
		}
		
		if (dbPrefix.trim().length() > 4) {
			viewController.errorMessage("Incorrect Information", "The DB_Prefix should not exceed 4 characters");
			return;
		}		
		
		int initialObjectClassId = config.getInitialObjectclassId();
		if (initialObjectClassId < GlobalConstants.MIN_ADE_OBJECTCLASSID) {
			viewController.errorMessage("Incorrect Information", "Then initial objectclass ID must be larger than or equal to 10000");
			return;
		}	

		int[] selectedRowNums = schemaTable.getSelectedRows();
		if (selectedRowNums.length == 0) {
			viewController.errorMessage("Incomplete Information", "Please select a schema namespace");
			return;
		}
		
		List<String> namespaces = new ArrayList<>();
		for (int rowNum : selectedRowNums) {
			namespaces.add(schemaTableModel.getColumn(rowNum).getValue(0));
		}
		
		File outputFile = new File(config.getTransformationOutputPath().trim());
		if (!(outputFile.isDirectory() && outputFile.exists())) {
			viewController.errorMessage("Incomplete Information", "Please select a valid output folder for transformation");
			return;
		}	
	
		final StatusDialog statusDialog = new StatusDialog(viewController.getTopFrame(), 
				"ADE Transformation",
				"Deriving ADE database schema...");
		
		SwingUtilities.invokeLater(() -> {
			statusDialog.setLocationRelativeTo(viewController.getTopFrame());
			statusDialog.setVisible(true);
		});
		
		try {
			adeTransformer.doProcess(namespaces);
			LOG.info("Transformation finished");
		} catch (TransformationException e) {
			printErrorMessage(e);
		} finally {
			SwingUtilities.invokeLater(() -> statusDialog.dispose());
		}
	}

	public void handleEvent(Event event) throws Exception {

	}

	@Override
	public DatabaseOperationType getType() {
		return null;
	}

	@Override
	public void setEnabled(boolean enable) {

	}

	@Override
	public String getLocalizedTitle() {
		return Translator.I18N.getString("ade_manager.transformationPanel.title");
	}

	@Override
	public Component getViewComponent() {
		return component;
	}

	@Override
	public String getToolTip() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

}
