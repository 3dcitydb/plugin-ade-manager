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

import org.citydb.ade.ADEExtensionManager;
import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DatabaseOperationType;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.database.schema.mapping.SchemaMappingException;
import org.citydb.database.schema.mapping.SchemaMappingValidationException;
import org.citydb.event.Event;
import org.citydb.gui.components.common.TitledPanel;
import org.citydb.gui.factory.PopupMenuDecorator;
import org.citydb.gui.modules.database.util.ADEInfoDialog;
import org.citydb.gui.modules.database.util.ADEInfoRow;
import org.citydb.gui.util.GuiUtil;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.event.ScriptCreationEvent;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citydb.plugins.ade_manager.gui.popup.ScriptDialog;
import org.citydb.plugins.ade_manager.gui.popup.StatusDialog;
import org.citydb.plugins.ade_manager.gui.table.ADEMetadataRow;
import org.citydb.plugins.ade_manager.gui.table.TableModel;
import org.citydb.plugins.ade_manager.registry.ADERegistrationController;
import org.citydb.plugins.ade_manager.registry.ADERegistrationException;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataInfo;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.util.Translator;
import org.citydb.registry.ObjectRegistry;

import javax.swing.*;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class ADERegistryPanel extends OperationModuleView {
	private JPanel component;
	private TitledPanel adeOperationsPanel;
	private TitledPanel browseRegistryPanel;
	private final JLabel browseRegistryLabel = new JLabel();
	private final JTextField browseRegistryText = new JTextField();
	private final JButton browseRegistryButton = new JButton();
	private final JButton registerADEButton = new JButton();
	private final JButton fetchADEsButton = new JButton();
	private final JButton removeADEButton = new JButton();
	private final JButton generateDeleteScriptsButton = new JButton();
	private final JButton generateEnvelopeScriptsButton = new JButton();
	private JTable adeTable;
	private final TableModel<ADEMetadataRow> adeTableModel = new TableModel<>(ADEMetadataRow.getColumnNames());

	private final ADERegistrationController adeRegistrationController;
	
	public ADERegistryPanel(ADEManagerPanel parentPanel, ConfigImpl config) {
		super(parentPanel, config);
		this.adeRegistrationController = new ADERegistrationController(config);
		eventDispatcher.addEventHandler(org.citydb.plugins.ade_manager.event.EventType.SCRIPT_CREATION_EVENT, this);		
		initGui();
	}
	
	protected void initGui() {
		component = new JPanel();
		component.setLayout(new GridBagLayout());

		// ADE table panel
		adeTable = new JTable(adeTableModel);
		adeTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		adeTable.setShowVerticalLines(true);
		adeTable.setShowHorizontalLines(true);
		adeTable.setCellSelectionEnabled(false);
		adeTable.setColumnSelectionAllowed(false);
		adeTable.setRowSelectionAllowed(true);
		adeTable.setRowHeight(20);
		JScrollPane adeTableScrollPanel = new JScrollPane(adeTable);
		adeTableScrollPanel.setPreferredSize(new Dimension(adeTable.getPreferredSize().width, 120));

		JPanel adeButtonsPanel = new JPanel();
		adeButtonsPanel.setLayout(new GridBagLayout());
		adeButtonsPanel.add(fetchADEsButton, GuiUtil.setConstraints(0, 0, 0, 0, GridBagConstraints.NONE, 0, 0, 0, BORDER_THICKNESS));
		adeButtonsPanel.add(removeADEButton, GuiUtil.setConstraints(1, 0, 0, 0, GridBagConstraints.NONE, 0, BORDER_THICKNESS, 0, BORDER_THICKNESS));
		adeButtonsPanel.add(generateDeleteScriptsButton, GuiUtil.setConstraints(2, 0, 0, 0, GridBagConstraints.NONE, 0, BORDER_THICKNESS, 0, BORDER_THICKNESS));
		adeButtonsPanel.add(generateEnvelopeScriptsButton, GuiUtil.setConstraints(3, 0, 0, 0, GridBagConstraints.NONE, 0, BORDER_THICKNESS, 0, 0));

		JPanel adeOperationsContentPanel = new JPanel();
		adeOperationsContentPanel.setLayout(new GridBagLayout());
		adeOperationsContentPanel.add(adeTableScrollPanel, GuiUtil.setConstraints(0, 0, 1, 0, GridBagConstraints.BOTH, 0, 0, BORDER_THICKNESS, 0));
		adeOperationsContentPanel.add(adeButtonsPanel, GuiUtil.setConstraints(0, 1, 1, 0, GridBagConstraints.NONE, BORDER_THICKNESS * 2, 0, 0, 0));
		adeOperationsPanel = new TitledPanel().build(adeOperationsContentPanel);

		JPanel browseRegistryContentPanel = new JPanel();
		browseRegistryContentPanel.setLayout(new GridBagLayout());
		browseRegistryContentPanel.add(browseRegistryLabel, GuiUtil.setConstraints(0, 0, 0, 0, GridBagConstraints.NONE, 0, 0, 0, BORDER_THICKNESS));
		browseRegistryContentPanel.add(browseRegistryText, GuiUtil.setConstraints(1, 0, 1, 1, GridBagConstraints.BOTH, 0, BORDER_THICKNESS, 0, BORDER_THICKNESS));
		browseRegistryContentPanel.add(browseRegistryButton, GuiUtil.setConstraints(2, 0, 0, 0, GridBagConstraints.NONE, 0, BORDER_THICKNESS, 0, 0));
		browseRegistryContentPanel.add(registerADEButton, GuiUtil.setConstraints(0, 1, 3, 1, 0, 0, GridBagConstraints.NONE, BORDER_THICKNESS * 3, 0, 0, 0));
		browseRegistryPanel = new TitledPanel().build(browseRegistryContentPanel);

		component.add(adeOperationsPanel, GuiUtil.setConstraints(0, 0, 1, 0, GridBagConstraints.BOTH, 0, 0, 0, 0));
		component.add(browseRegistryPanel, GuiUtil.setConstraints(0, 1, 1, 0, GridBagConstraints.BOTH, 0, 0, 0, 0));

		PopupMenuDecorator.getInstance().decorate(browseRegistryText);

		adeTable.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					Thread thread = new Thread(() -> showADEInfoDialog());
					thread.setDaemon(true);
					thread.start();
				}
			}
		});

		fetchADEsButton.addActionListener(e -> {
			Thread thread = new Thread(this::showRegisteredADEs);
			thread.setDaemon(true);
			thread.start();
		});
		
		registerADEButton.addActionListener(e -> {
			Thread thread = new Thread(this::registerADE);
			thread.setDaemon(true);
			thread.start();
		});
		
		removeADEButton.addActionListener(e -> {
			Thread thread = new Thread(this::deregisterADE);
			thread.setDaemon(true);
			thread.start();
		});
		
		generateDeleteScriptsButton.addActionListener(e -> {
			Thread thread = new Thread(this::generateDeleteScripts);
			thread.setDaemon(true);
			thread.start();
		});
		
		generateEnvelopeScriptsButton.addActionListener(e -> {
			Thread thread = new Thread(this::generateEnvelopeScripts);
			thread.setDaemon(true);
			thread.start();
		});
		
		browseRegistryButton.addActionListener(e -> browserRegistryInputDirectory());
	}
	
	@Override
	public String getLocalizedTitle() {
		return Translator.I18N.getString("ade_manager.registryPanel.title");
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

	@Override
	public DatabaseOperationType getType() {
		return null;
	}

	@Override
	public void doTranslation() {
		adeOperationsPanel.setTitle(Translator.I18N.getString("ade_manager.operationsPanel.border"));
		adeTable.getColumnModel().getColumn(0).setHeaderValue(Translator.I18N.getString("ade_manager.operationsPanel.adeTable.id"));
		adeTable.getColumnModel().getColumn(1).setHeaderValue(Translator.I18N.getString("ade_manager.operationsPanel.adeTable.name"));
		adeTable.getColumnModel().getColumn(2).setHeaderValue(Translator.I18N.getString("ade_manager.operationsPanel.adeTable.description"));
		adeTable.getColumnModel().getColumn(3).setHeaderValue(Translator.I18N.getString("ade_manager.operationsPanel.adeTable.version"));
		adeTable.getColumnModel().getColumn(4).setHeaderValue(Translator.I18N.getString("ade_manager.operationsPanel.adeTable.dbPrefix"));
		adeTable.getColumnModel().getColumn(5).setHeaderValue(Translator.I18N.getString("ade_manager.operationsPanel.adeTable.creationDate"));
		browseRegistryLabel.setText(Translator.I18N.getString("ade_manager.registryPanel.label"));
		browseRegistryPanel.setTitle(Translator.I18N.getString("ade_manager.registryPanel.border"));
		browseRegistryButton.setText(Language.I18N.getString("common.button.browse"));
		registerADEButton.setText(Translator.I18N.getString("ade_manager.registryPanel.button.register"));
		fetchADEsButton.setText(Translator.I18N.getString("ade_manager.operationsPanel.button.fetch"));
		removeADEButton.setText(Translator.I18N.getString("ade_manager.operationsPanel.button.remove"));
		generateDeleteScriptsButton.setText(Translator.I18N.getString("ade_manager.operationsPanel.button.gen_delete_script"));
		generateEnvelopeScriptsButton.setText(Translator.I18N.getString("ade_manager.operationsPanel.button.gen_envelope_script"));
	}

	@Override
	public void setEnabled(boolean enable) {
		// nothing to do
	}

	@Override
	public void loadSettings() {
		browseRegistryText.setText(config.getAdeRegistryInputPath());		
	}

	@Override
	public void setSettings() {
		config.setAdeRegistryInputPath(browseRegistryText.getText());
	}
	
	private void browserRegistryInputDirectory() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(Translator.I18N.getString("ade_manager.registryPanel.inputFileChooser.title"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setCurrentDirectory(new File(browseRegistryText.getText()).getParentFile());
	
		int result = chooser.showOpenDialog(parentPanel.getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		String browseString = chooser.getSelectedFile().toString();
		if (!browseString.isEmpty())
			browseRegistryText.setText(browseString);
	}

	private void showADEInfoDialog() {
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			printErrorMessage("Failed to connect to database", e);
			return;
		}
		String adeId = adeTableModel.getColumn(adeTable.getSelectedRow()).getValue(0);
		String adeName = adeTableModel.getColumn(adeTable.getSelectedRow()).getValue(1);
		String adeVersion = adeTableModel.getColumn(adeTable.getSelectedRow()).getValue(3);
		SchemaMapping rootSchema = ObjectRegistry.getInstance().getSchemaMapping();
		SchemaMapping adeSchema;
		try {
			adeSchema = databaseController.getActiveDatabaseAdapter().getUtil().getADESchemaMapping(adeId, rootSchema);
		} catch (SQLException | JAXBException | SchemaMappingException | SchemaMappingValidationException e) {
			printErrorMessage("Failed to retrieve ADE information for '" + adeName + "'.", e);
			return;
		}

		if (adeSchema != null) {
			boolean hasImpExpSupport = ADEExtensionManager.getInstance().getExtensionById(adeId) != null;
			ADEInfoRow adeInfoRow = new ADEInfoRow(adeId, adeName, adeVersion, true, hasImpExpSupport);
			ADEInfoDialog dialog = new ADEInfoDialog(adeInfoRow, adeSchema, rootSchema, viewController.getTopFrame());
			SwingUtilities.invokeLater(() -> {
				dialog.setLocationRelativeTo(viewController.getTopFrame());
				dialog.setVisible(true);
			});
		}
	}
	
	private void registerADE() {
		viewController.clearConsole();
		setSettings();

		if (browseRegistryText.getText().trim().isEmpty()) {
			viewController.errorMessage(Translator.I18N.getString("ade_manager.error.incomplete.title"),
					Translator.I18N.getString("ade_manager.error.incomplete.adeExtension"));
			return;
		}
		
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			printErrorMessage("Failed to connect to database", e);
			return;
		}
		
		final StatusDialog statusDialog = new StatusDialog(viewController.getTopFrame(),
				Translator.I18N.getString("ade_manager.dialog.register.title"),
				Translator.I18N.getString("ade_manager.dialog.register.message"));
		
		SwingUtilities.invokeLater(() -> {
			statusDialog.setLocationRelativeTo(viewController.getTopFrame());
			statusDialog.setVisible(true);
		});
		
		boolean isComplete = false;
		try {	
			adeRegistrationController.initDBConneciton();
			isComplete = adeRegistrationController.registerADE();
			adeRegistrationController.commitTransactions();
		} catch (ADERegistrationException e) {
			adeRegistrationController.rollbackTransactions();
			printErrorMessage("Failed to register ADE.", e);
		} finally {
			adeRegistrationController.closeDBConnection();
			SwingUtilities.invokeLater(statusDialog::dispose);
		}
		
		if (isComplete) {
			// database re-connection is required for completing the ADE registration process
			log.info("ADE registration is completed and will take effect after reconnecting to the database.");
			if (dbPool.isConnected()) {
				dbPool.disconnect();
				databaseController.connect(true);
			}
			// update the ADE list table by querying the ADE again
			showRegisteredADEs();
		}
	}
	
	private void showRegisteredADEs() {
		setSettings();	
		
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			printErrorMessage("Failed to connect to database.", e);
			return;
		}
		
		try {	
			adeRegistrationController.initDBConneciton();
			List<ADEMetadataInfo> adeList = adeRegistrationController.queryRegisteredADEs();
			if (adeList.size() == 0)
				log.info("Status: No ADEs are registered in the connected database.");
			
			adeTableModel.reset();			
			for (ADEMetadataInfo adeEntity: adeList) {
				if (adeEntity == null) 
					continue; 				
				adeTableModel.addNewRow(new ADEMetadataRow(adeEntity));
			}
		} catch (ADERegistrationException e) {
			printErrorMessage("Failed to fetch ADEs.", e);
		} finally {
			adeRegistrationController.closeDBConnection();
		}
	}
	
	private void deregisterADE(){
		viewController.clearConsole();
		setSettings();	
		
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			printErrorMessage("Failed to connect to database.", e);
			return;
		}
		
		int selectedRowNum = adeTable.getSelectedRow();
		if (selectedRowNum == -1) {
			viewController.errorMessage("ADE Deregistration", "Please select one of the listed ADEs.");
			return;
		}
		
		final StatusDialog statusDialog = new StatusDialog(viewController.getTopFrame(),
				Translator.I18N.getString("ade_manager.dialog.remove.title"),
				Translator.I18N.getString("ade_manager.dialog.remove.message"));
		
		SwingUtilities.invokeLater(() -> {
			statusDialog.setLocationRelativeTo(viewController.getTopFrame());
			statusDialog.setVisible(true);
		});
		
		boolean isComplete = false;
		String adeId = adeTableModel.getColumn(selectedRowNum).getValue(0);	
		try {
			adeRegistrationController.initDBConneciton();
			isComplete = adeRegistrationController.deregisterADE(adeId);
			adeRegistrationController.commitTransactions();
		} catch (ADERegistrationException e) {
			adeRegistrationController.rollbackTransactions();
			printErrorMessage("Failed to deregister ADE.", e);
		} finally {
			adeRegistrationController.closeDBConnection();
			SwingUtilities.invokeLater(statusDialog::dispose);
		}	
		
		if (isComplete) {
			// database re-connection is required for completing the ADE de-registration process
			log.info("ADE deregistration is completed and will take effect after reconnecting to the database.");
			if (dbPool.isConnected()) {
				dbPool.disconnect();
				databaseController.connect(true);
			}
			// update the ADE list table by querying the ADE again
			showRegisteredADEs();
		}		
	}
	
	private void generateDeleteScripts() {
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			printErrorMessage("Failed to connect to database.", e);
			return;
		}
		
		final StatusDialog statusDialog = new StatusDialog(viewController.getTopFrame(),
				Translator.I18N.getString("ade_manager.dialog.script.title"),
				Translator.I18N.getString("ade_manager.dialog.deleteScript.message"));

		SwingUtilities.invokeLater(() -> {
			statusDialog.setLocationRelativeTo(viewController.getTopFrame());
			statusDialog.setVisible(true);
		});
		
		DBSQLScript deleteScript = null;
		try {	
			adeRegistrationController.initDBConneciton();
			deleteScript = adeRegistrationController.createDeleteScripts();
		} catch (ADERegistrationException e) {
			printErrorMessage("Failed to create delete script.", e);
		} finally {
			adeRegistrationController.closeDBConnection();
			SwingUtilities.invokeLater(statusDialog::dispose);
		}	
		
		if (deleteScript != null)
			eventDispatcher.triggerEvent(new ScriptCreationEvent(deleteScript, false, this));
	}
	
	private void generateEnvelopeScripts() {
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			printErrorMessage("Failed to connect to database.", e);
			return;
		}
		
		final StatusDialog statusDialog = new StatusDialog(viewController.getTopFrame(),
				Translator.I18N.getString("ade_manager.dialog.script.title"),
				Translator.I18N.getString("ade_manager.dialog.envelopeScript.message"));
		
		SwingUtilities.invokeLater(() -> {
			statusDialog.setLocationRelativeTo(viewController.getTopFrame());
			statusDialog.setVisible(true);
		});
		
		DBSQLScript envelopeScript = null;		
		try {			
			adeRegistrationController.initDBConneciton();
			envelopeScript = adeRegistrationController.createEnvelopeScripts();
		} catch (ADERegistrationException e) {
			printErrorMessage("Failed to create envelope script.", e);
		} finally {
			adeRegistrationController.closeDBConnection();
			SwingUtilities.invokeLater(statusDialog::dispose);
		}	
		
		if (envelopeScript != null)
			eventDispatcher.triggerEvent(new ScriptCreationEvent(envelopeScript, false, this));
	}

	@Override
	public void handleEvent(Event event) throws Exception {
		if (event.getEventType() == org.citydb.plugins.ade_manager.event.EventType.SCRIPT_CREATION_EVENT) {
            ScriptCreationEvent scriptCreationEvent = (ScriptCreationEvent) event;
            DBSQLScript script = scriptCreationEvent.getScript();
            boolean autoInstall = scriptCreationEvent.isAutoInstall();
            if (autoInstall)
            	return;
            
            final ScriptDialog scriptDialog = new ScriptDialog(viewController.getTopFrame(), script, autoInstall);			
    		scriptDialog.getButton().addActionListener(e -> SwingUtilities.invokeLater(() -> {
			    try {
				    adeRegistrationController.initDBConneciton();
				    adeRegistrationController.installDBScript(scriptDialog.getScript());
				    adeRegistrationController.commitTransactions();
				    log.info("Script is successfully installed into the connected database.");
			    } catch (ADERegistrationException e1) {
				    adeRegistrationController.rollbackTransactions();
				    printErrorMessage(e1);
			    } finally {
				    adeRegistrationController.closeDBConnection();
				    scriptDialog.dispose();
			    }
		    }));

    		SwingUtilities.invokeLater(() -> {
			    scriptDialog.setLocationRelativeTo(parentPanel.getTopLevelAncestor());
			    scriptDialog.setVisible(true);
		    });
        }
	}

}
