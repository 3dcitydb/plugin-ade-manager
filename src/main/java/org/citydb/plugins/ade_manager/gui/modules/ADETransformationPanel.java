/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2021
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.lrg.tum.de/gis/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * Virtual City Systems, Berlin <https://vc.systems/>
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
import org.citydb.event.Event;
import org.citydb.gui.components.common.TitledPanel;
import org.citydb.gui.factory.PopupMenuDecorator;
import org.citydb.gui.util.GuiUtil;
import org.citydb.plugins.ade_manager.ADEManagerPlugin;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citydb.plugins.ade_manager.gui.popup.StatusDialog;
import org.citydb.plugins.ade_manager.gui.table.ADESchemaNamespaceRow;
import org.citydb.plugins.ade_manager.gui.table.TableCellRenderer;
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
	private TitledPanel component;
	private final JLabel browseXMLSchemaLabel = new JLabel();
	private final JTextField browseXMLSchemaText = new JTextField();
	private final JButton browseXMLSchemaButton = new JButton();
	private final JButton readXMLSchemaButton = new JButton();
	private JTable schemaTable;
	private final JLabel nameLabel = new JLabel();
	private final JTextField nameInputField = new JTextField();
	private final JLabel descriptionLabel = new JLabel();
	private final JTextField descriptionInputField = new JTextField();
	private final JLabel versionLabel = new JLabel();
	private final JTextField versionInputField = new JTextField();
	private final JLabel dbPrefixLabel = new JLabel();
	private final JTextField dbPrefixInputField = new JTextField();
	private final JLabel initObjectClassIdLabel = new JLabel();
	private final JFormattedTextField initObjectClassIdInputField = new JFormattedTextField(new DecimalFormat("##########"));
	private final JLabel browseOutputLabel = new JLabel();
	private final JTextField browseOutputText = new JTextField();
	private final JButton browserOutputButton = new JButton();
	private final JButton transformAndExportButton = new JButton();
	private final TableModel<ADESchemaNamespaceRow> schemaTableModel = new TableModel<>(ADESchemaNamespaceRow.getColumnNames());

	private final TransformationController adeTransformer;

	public ADETransformationPanel(ADEManagerPanel parentPanel, ADEManagerPlugin plugin) {
		super(parentPanel, plugin);
		this.adeTransformer = new TransformationController(plugin);
		initGui();
	}

	private void initGui() {
		// Input panel
		JPanel browseXMLSchemaPanel = new JPanel();
		browseXMLSchemaPanel.setLayout(new GridBagLayout());
		browseXMLSchemaPanel.add(browseXMLSchemaLabel, GuiUtil.setConstraints(0, 0, 0, 0, GridBagConstraints.NONE, 0, 0, 0, BORDER_THICKNESS));
		browseXMLSchemaPanel.add(browseXMLSchemaText, GuiUtil.setConstraints(1, 0, 1, 1, GridBagConstraints.BOTH, 0, BORDER_THICKNESS, 0, BORDER_THICKNESS));
		browseXMLSchemaPanel.add(browseXMLSchemaButton, GuiUtil.setConstraints(2, 0, 0, 0, GridBagConstraints.NONE, 0, BORDER_THICKNESS, 0, 0));
		browseXMLSchemaPanel.add(readXMLSchemaButton, GuiUtil.setConstraints(0, 1, 3, 1, 1, 0, GridBagConstraints.NONE, BORDER_THICKNESS * 3, 0, 0, 0));

		// Schema table panel
		schemaTable = new JTable(schemaTableModel);
		schemaTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		schemaTable.getTableHeader().setDefaultRenderer(new TableCellRenderer(schemaTable.getTableHeader().getDefaultRenderer()));
		for (int i = 0; i < schemaTable.getColumnModel().getColumnCount(); i++) {
			schemaTable.getColumnModel().getColumn(i).setCellRenderer(
					new TableCellRenderer(schemaTable.getDefaultRenderer(schemaTableModel.getColumnClass(i))));
		}

		JScrollPane schemaPanel = new JScrollPane(schemaTable);
		schemaPanel.setPreferredSize(new Dimension(browseXMLSchemaText.getPreferredSize().width, 200));

		JPanel metadataInputPanel = new JPanel();
		metadataInputPanel.setLayout(new GridBagLayout());
		{
			nameInputField.setColumns(10);
			descriptionInputField.setColumns(10);
			versionInputField.setColumns(10);
			dbPrefixInputField.setColumns(10);
			initObjectClassIdInputField.setColumns(10);

			metadataInputPanel.add(nameLabel, GuiUtil.setConstraints(0, 0, 1, 0, GridBagConstraints.HORIZONTAL, 0, 0, 0, 0));
			metadataInputPanel.add(nameInputField, GuiUtil.setConstraints(0, 1, 1, 0, GridBagConstraints.HORIZONTAL, 3, 0, BORDER_THICKNESS, 0));
			metadataInputPanel.add(descriptionLabel, GuiUtil.setConstraints(0, 2, 1, 0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, 0, 0, 0));
			metadataInputPanel.add(descriptionInputField, GuiUtil.setConstraints(0, 3, 1, 0, GridBagConstraints.HORIZONTAL, 3, 0, BORDER_THICKNESS, 0));
			metadataInputPanel.add(versionLabel, GuiUtil.setConstraints(0, 4, 1, 0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, 0, 0, 0));
			metadataInputPanel.add(versionInputField, GuiUtil.setConstraints(0, 5, 1, 0, GridBagConstraints.HORIZONTAL, 3, 0, BORDER_THICKNESS, 0));
			metadataInputPanel.add(dbPrefixLabel, GuiUtil.setConstraints(0, 6, 1, 0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, 0, 0, 0));
			metadataInputPanel.add(dbPrefixInputField, GuiUtil.setConstraints(0, 7, 1, 0, GridBagConstraints.HORIZONTAL, 3, 0, BORDER_THICKNESS, 0));
			metadataInputPanel.add(initObjectClassIdLabel, GuiUtil.setConstraints(0, 8, 1, 0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, 0, 0, 0));
			metadataInputPanel.add(initObjectClassIdInputField, GuiUtil.setConstraints(0, 9, 1, 0, GridBagConstraints.HORIZONTAL, 3, 0, 0, 0));
		}

		JPanel schemaAndMetadataPanel = new JPanel();
		schemaAndMetadataPanel.setLayout(new GridBagLayout());
		schemaAndMetadataPanel.add(schemaPanel, GuiUtil.setConstraints(0, 0, 0.8, 1, GridBagConstraints.BOTH, 0, 0, 0, BORDER_THICKNESS));
		schemaAndMetadataPanel.add(metadataInputPanel, GuiUtil.setConstraints(1, 0, 0.2, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, 0, BORDER_THICKNESS, 0, 0));

		// Export panel
		JPanel transformationOutputPanel = new JPanel();
		transformationOutputPanel.setLayout(new GridBagLayout());
		transformationOutputPanel.add(browseOutputLabel, GuiUtil.setConstraints(0, 0, 0, 0, GridBagConstraints.NONE, 0, 0, 0, BORDER_THICKNESS));
		transformationOutputPanel.add(browseOutputText, GuiUtil.setConstraints(1, 0, 1, 1, GridBagConstraints.BOTH, 0, BORDER_THICKNESS, 0, BORDER_THICKNESS));
		transformationOutputPanel.add(browserOutputButton, GuiUtil.setConstraints(2, 0, 0, 0, GridBagConstraints.NONE, 0, BORDER_THICKNESS, 0, 0));
		transformationOutputPanel.add(transformAndExportButton, GuiUtil.setConstraints(0, 1, 3, 1, 1, 0, GridBagConstraints.NONE, BORDER_THICKNESS * 3, 0, 0, 0));

		JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
		content.add(browseXMLSchemaPanel, GuiUtil.setConstraints(0, 0, 1, 0, GridBagConstraints.BOTH, 0, 0, 0, 0));
		content.add(schemaAndMetadataPanel, GuiUtil.setConstraints(0, 1, 1, 0, GridBagConstraints.BOTH, BORDER_THICKNESS * 5, 0, 0, 0));
		content.add(transformationOutputPanel, GuiUtil.setConstraints(0, 2, 1, 0, GridBagConstraints.BOTH, BORDER_THICKNESS * 5, 0, 0, 0));

		component = new TitledPanel().build(content);

		PopupMenuDecorator.getInstance().decorate(browseXMLSchemaText, browseOutputText, nameInputField,
				descriptionInputField, versionInputField, dbPrefixInputField, initObjectClassIdInputField);

		browseXMLSchemaButton.addActionListener(e -> browserXMLSchemaFile());
		
		readXMLSchemaButton.addActionListener(e -> new SwingWorker<Void, Void>() {
			protected Void doInBackground() {
				parseADESchema();
				return null;
			}
		}.execute());
		
		browserOutputButton.addActionListener(e -> browseTransformationOutputDirectory());
				
		transformAndExportButton.addActionListener(e -> new SwingWorker<Void, Void>() {
			protected Void doInBackground() {
				transformADESchema();
				return null;
			}
		}.execute());
		
		setEnabledMetadataSettings(false);
	}

	// localized Labels and Strings
	public void doTranslation() {
		schemaTable.getColumnModel().getColumn(0).setHeaderValue(Translator.I18N.getString("ade_manager.operationsPanel.xsdTable.namespace"));

		nameLabel.setText(MessageFormat.format(Translator.I18N.getString("ade_manager.transformationPanel.namePanel.border"),
				String.valueOf(GlobalConstants.MAX_ADE_NAME_LENGTH)));
		descriptionLabel.setText(MessageFormat.format(Translator.I18N.getString("ade_manager.transformationPanel.descriptionPanel.border"),
				String.valueOf(GlobalConstants.MAX_ADE_DESCRIPTION_LENGTH)));
		versionLabel.setText(MessageFormat.format(Translator.I18N.getString("ade_manager.transformationPanel.versionPanel.border"),
				String.valueOf(GlobalConstants.MAX_ADE_VERSION_LENGTH)));
		dbPrefixLabel.setText(MessageFormat.format(Translator.I18N.getString("ade_manager.transformationPanel.dbPrefixPanel.border"),
				String.valueOf(GlobalConstants.MAX_DB_PREFIX_LENGTH)));
		initObjectClassIdLabel.setText(MessageFormat.format(Translator.I18N.getString("ade_manager.transformationPanel.initObjectClassIdPanel.border"),
				String.valueOf(GlobalConstants.MIN_ADE_OBJECTCLASSID)));

		browseXMLSchemaLabel.setText(Translator.I18N.getString("ade_manager.transformationPanel.browseXMLSchemaPanel.label"));
		browseXMLSchemaButton.setText(Language.I18N.getString("common.button.browse"));
		readXMLSchemaButton.setText(Translator.I18N.getString("ade_manager.transformationPanel.button.readXMLSchema"));
		browseOutputLabel.setText(Translator.I18N.getString("ade_manager.transformationPanel.transformationOutputPanel.label"));
		browserOutputButton.setText(Language.I18N.getString("common.button.browse"));
		transformAndExportButton.setText(Translator.I18N.getString("ade_manager.transformationPanel.button.transformAndExport"));

		component.setTitle(Translator.I18N.getString("ade_manager.transformationPanel.title"));
	}

	public void loadSettings() {
		browseXMLSchemaText.setText(plugin.getConfig().getXMLschemaInputPath());
		browseOutputText.setText(plugin.getConfig().getTransformationOutputPath());
		nameInputField.setText(plugin.getConfig().getAdeName());
		descriptionInputField.setText(plugin.getConfig().getAdeDescription());
		versionInputField.setText(plugin.getConfig().getAdeVersion());
		dbPrefixInputField.setText(plugin.getConfig().getAdeDbPrefix());
		initObjectClassIdInputField.setValue(plugin.getConfig().getInitialObjectclassId());
	}

	public void setSettings() {
		plugin.getConfig().setXMLschemaInputPath(browseXMLSchemaText.getText());
		plugin.getConfig().setTransformationOutputPath(browseOutputText.getText());
		plugin.getConfig().setAdeName(nameInputField.getText());
		plugin.getConfig().setAdeDescription(descriptionInputField.getText());
		plugin.getConfig().setAdeVersion(versionInputField.getText());
		plugin.getConfig().setAdeDbPrefix(dbPrefixInputField.getText());
		plugin.getConfig().setInitialObjectclassId(((Number)initObjectClassIdInputField.getValue()).intValue());
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
		nameLabel.setEnabled(enable);
		nameInputField.setEnabled(enable);
		descriptionLabel.setEnabled(enable);
		descriptionInputField.setEnabled(enable);
		versionLabel.setEnabled(enable);
		versionInputField.setEnabled(enable);
		dbPrefixLabel.setEnabled(enable);
		dbPrefixInputField.setEnabled(enable);
		initObjectClassIdLabel.setEnabled(enable);
		initObjectClassIdInputField.setEnabled(enable);
		browseOutputLabel.setEnabled(enable);
		browseOutputText.setEnabled(enable);
		browserOutputButton.setEnabled(enable);
		transformAndExportButton.setEnabled(enable);
		schemaTable.getTableHeader().setEnabled(enable);
		schemaTable.setEnabled(enable);
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

		if (browseXMLSchemaText.getText().trim().isEmpty()) {
			viewController.errorMessage(Translator.I18N.getString("ade_manager.error.incomplete.title"),
					Translator.I18N.getString("ade_manager.error.incomplete.schema"));
			setEnabledMetadataSettings(false);
			return;
		}

		final StatusDialog statusDialog = new StatusDialog(viewController.getTopFrame(),
				Translator.I18N.getString("ade_manager.dialog.transformation.title"),
				Translator.I18N.getString("ade_manager.dialog.parse.message"));
		
		SwingUtilities.invokeLater(() -> {
			statusDialog.setLocationRelativeTo(viewController.getTopFrame());
			statusDialog.setVisible(true);
		});
		
		log.info("Start parsing ADE XML schema...");
		String xmlSchemaPath = browseXMLSchemaText.getText();
		try {			
			List<String> adeNamespaces = adeTransformer.getADENamespacesFromXMLSchema(xmlSchemaPath);		
			for (String schemaNamespace : adeNamespaces) {
				ADESchemaNamespaceRow schemaColumn = new ADESchemaNamespaceRow(schemaNamespace);
				schemaTableModel.addNewRow(schemaColumn);
			}			
			log.info("Parsing ADE XML schema completed.");
			
			setEnabledMetadataSettings(true);
		} catch (TransformationException e) {
			printErrorMessage("Failed to parse ADE schema.", e);
			setEnabledMetadataSettings(false);
		} finally {
			SwingUtilities.invokeLater(statusDialog::dispose);
		}
	}

	private void transformADESchema() {	
		setSettings();
	
		String adeName = plugin.getConfig().getAdeName();
		if (adeName.trim().length() == 0) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incomplete.title"),
					Translator.I18N.getString("ade_manager.error.incomplete.name"));
			return;
		}
		
		if (adeName.trim().length() > GlobalConstants.MAX_ADE_NAME_LENGTH) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incorrect.title"),
					MessageFormat.format(Translator.I18N.getString("ade_manager.error.incorrect.name"),
							String.valueOf(GlobalConstants.MAX_ADE_NAME_LENGTH)));
			return;
		}
		
		String adeDescription = plugin.getConfig().getAdeDescription();
		if (adeDescription.trim().length() == 0) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incomplete.title"),
					Translator.I18N.getString("ade_manager.error.incomplete.description"));
			return;
		}
		
		if (adeDescription.trim().length() > GlobalConstants.MAX_ADE_NAME_LENGTH) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incorrect.title"),
					MessageFormat.format(Translator.I18N.getString("ade_manager.error.incorrect.description"),
							String.valueOf(GlobalConstants.MAX_ADE_DESCRIPTION_LENGTH)));
			return;
		}
		
		String adeVersion = plugin.getConfig().getAdeVersion();
		if (adeVersion.trim().length() == 0) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incomplete.title"),
					Translator.I18N.getString("ade_manager.error.incomplete.version"));
			return;
		}
		
		if (adeVersion.trim().length() > GlobalConstants.MAX_ADE_VERSION_LENGTH) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incorrect.title"),
					MessageFormat.format(Translator.I18N.getString("ade_manager.error.incorrect.version"),
							String.valueOf(GlobalConstants.MAX_ADE_VERSION_LENGTH)));
			return;
		}
		
		String dbPrefix = plugin.getConfig().getAdeDbPrefix();
		if (dbPrefix.trim().length() == 0) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incomplete.title"),
					Translator.I18N.getString("ade_manager.error.incomplete.prefix"));
			return;
		}
		
		if (Character.isDigit(dbPrefix.trim().charAt(0))) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incorrect.title"),
					Translator.I18N.getString("ade_manager.error.incorrect.prefix.digit"));
			return;
		}
		
		if (!Pattern.compile("[a-zA-Z0-9]*").matcher(dbPrefix.trim()).matches()) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incorrect.title"),
					Translator.I18N.getString("ade_manager.error.incorrect.prefix.special"));
			return;
		}
		
		if (dbPrefix.trim().length() > 4) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incorrect.title"),
					Translator.I18N.getString("ade_manager.error.incorrect.prefix.length"));
			return;
		}		
		
		int initialObjectClassId = plugin.getConfig().getInitialObjectclassId();
		if (initialObjectClassId < GlobalConstants.MIN_ADE_OBJECTCLASSID) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incorrect.title"),
					Translator.I18N.getString("ade_manager.error.incorrect.classId"));
			return;
		}	

		int[] selectedRowNums = schemaTable.getSelectedRows();
		if (selectedRowNums.length == 0) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incomplete.title"),
					Translator.I18N.getString("ade_manager.error.incomplete.namespace"));
			return;
		}
		
		List<String> namespaces = new ArrayList<>();
		for (int rowNum : selectedRowNums) {
			namespaces.add(schemaTableModel.getColumn(rowNum).getValue(0));
		}
		
		File outputFile = new File(plugin.getConfig().getTransformationOutputPath().trim());
		if (!(outputFile.isDirectory() && outputFile.exists())) {
			viewController.errorMessage(
					Translator.I18N.getString("ade_manager.error.incomplete.title"),
					Translator.I18N.getString("ade_manager.error.incomplete.output"));
			return;
		}	
	
		final StatusDialog statusDialog = new StatusDialog(viewController.getTopFrame(),
				Translator.I18N.getString("ade_manager.dialog.transformation.title"),
				Translator.I18N.getString("ade_manager.dialog.transform.message"));
		
		SwingUtilities.invokeLater(() -> {
			statusDialog.setLocationRelativeTo(viewController.getTopFrame());
			statusDialog.setVisible(true);
		});
		
		try {
			adeTransformer.doProcess(namespaces);
			log.info("Transformation finished.");
		} catch (TransformationException e) {
			printErrorMessage("Failed to transform XML schema.", e);
		} finally {
			SwingUtilities.invokeLater(statusDialog::dispose);
		}
	}

	public void handleEvent(Event event) throws Exception {
		// nothing to do
	}

	@Override
	public Component getViewComponent() {
		return component;
	}
}
