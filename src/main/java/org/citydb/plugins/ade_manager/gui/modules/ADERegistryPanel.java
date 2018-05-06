package org.citydb.plugins.ade_manager.gui.modules;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DBOperationType;
import org.citydb.config.project.database.DatabaseConfigurationException;
import org.citydb.database.version.DatabaseVersionException;
import org.citydb.event.Event;
import org.citydb.gui.util.GuiUtil;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.event.ScriptCreationEvent;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citydb.plugins.ade_manager.gui.popup.ScriptDialog;
import org.citydb.plugins.ade_manager.gui.table.ADEMetadataRow;
import org.citydb.plugins.ade_manager.gui.table.TableModel;
import org.citydb.plugins.ade_manager.registry.ADERegistrationController;
import org.citydb.plugins.ade_manager.registry.ADERegistrationException;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataInfo;

public class ADERegistryPanel extends OperationModuleView {
	private JPanel component;
	private JPanel browseRegistryPanel;
	private JTextField browseRegistryText = new JTextField();
	private JButton browseRegistryButton = new JButton();		
	private JPanel adeButtonsPanel;
	private JButton registerADEButton = new JButton();	
	private JButton fetchADEsButton = new JButton();	
	private JButton removeADEButton = new JButton();
	private JButton generateDeleteScriptsButton = new JButton();
	private JScrollPane adeTableScrollPanel;
	private JTable adeTable;
	private TableModel<ADEMetadataRow> adeTableModel = new TableModel<ADEMetadataRow>(ADEMetadataRow.getColumnNames());
	private int standardButtonHeight = (new JButton("D")).getPreferredSize().height;

	private ADERegistrationController adeRegistor;	
	
	public ADERegistryPanel(ADEManagerPanel parentPanel, ConfigImpl config) {
		super(parentPanel, config);
		this.adeRegistor = new ADERegistrationController(config);
		eventDispatcher.addEventHandler(org.citydb.plugins.ade_manager.event.EventType.SCRIPT_CREATION_EVENT, this);		
		initGui();
	}
	
	protected void initGui() {		
		component = new JPanel();
		component.setLayout(new GridBagLayout());

		fetchADEsButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		removeADEButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		registerADEButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		generateDeleteScriptsButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		
		// ADE table panel
		adeTable = new JTable(adeTableModel);
		adeTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		adeTable.setCellSelectionEnabled(false);
		adeTable.setColumnSelectionAllowed(false);
		adeTable.setRowSelectionAllowed(true);
		adeTable.setRowHeight(20);		
		adeTableScrollPanel = new JScrollPane(adeTable);
		adeTableScrollPanel.setPreferredSize(new Dimension(adeTable.getPreferredSize().width, 150));
		adeTableScrollPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 0, 4, 4)));

		browseRegistryPanel = new JPanel();
		browseRegistryPanel.setLayout(new GridBagLayout());
		browseRegistryPanel.setBorder(BorderFactory.createTitledBorder(""));
		browseRegistryPanel.add(browseRegistryText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		browseRegistryPanel.add(browseRegistryButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));	
		
		int index = 0;
		adeButtonsPanel = new JPanel();
		adeButtonsPanel.setLayout(new GridBagLayout());
		adeButtonsPanel.add(fetchADEsButton, GuiUtil.setConstraints(index++,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		adeButtonsPanel.add(registerADEButton, GuiUtil.setConstraints(index++,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		adeButtonsPanel.add(removeADEButton, GuiUtil.setConstraints(index++,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		adeButtonsPanel.add(generateDeleteScriptsButton, GuiUtil.setConstraints(index++,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		
		index = 0;
		component.add(adeTableScrollPanel, GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		component.add(browseRegistryPanel, GuiUtil.setConstraints(0,index++,1.0,0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		component.add(adeButtonsPanel, GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
			
		fetchADEsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						showRegisteredADEs();						
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
		
		registerADEButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						registerADE();
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
		
		removeADEButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						deregisterADE();
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
		
		generateDeleteScriptsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						generateDeleteScripts();
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
		
		browseRegistryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browserRegistryInputDirectory();
			}
		});
	}
	
	@Override
	public String getLocalizedTitle() {
		return "Registry";
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
	public DBOperationType getType() {
		return null;
	}

	@Override
	public void doTranslation() {
		((TitledBorder) browseRegistryPanel.getBorder()).setTitle("Input for ADE Registry");
		browseRegistryButton.setText(Language.I18N.getString("common.button.browse"));
		
		registerADEButton.setText("Register ADE into DB");
		fetchADEsButton.setText("Fetch ADEs from DB");
		removeADEButton.setText("Remove seleted ADE from DB");
		generateDeleteScriptsButton.setText("Generate Delete Scripts");
	}

	@Override
	public void setEnabled(boolean enable) {
		// 
		
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
		chooser.setDialogTitle("Input Folder");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setCurrentDirectory(new File(browseRegistryText.getText()).getParentFile());
	
		int result = chooser.showOpenDialog(parentPanel.getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		String browseString = chooser.getSelectedFile().toString();
		if (!browseString.isEmpty())
			browseRegistryText.setText(browseString);
	}
	
	private void registerADE() {
		setSettings();
		
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			printErrorMessage("ADE registration aborted", e);
			return;
		}
		
		boolean isComplete = false;
		try {	
			adeRegistor.initDBConneciton();
			isComplete = adeRegistor.registerADE();
			adeRegistor.commitTransactions();
		} catch (ADERegistrationException e) {
			adeRegistor.rollbackTransactions();
			printErrorMessage("ADE registration aborted", e);
		} finally {
			adeRegistor.closeDBConnection();
		}
		
		if (isComplete) {
			// database re-connection is required for completing the ADE registration process
			LOG.info("ADE registration is completed and will take effect after reconnecting to the database.");	
			if (dbPool.isConnected()) {
				dbPool.disconnect();
				try {
					databaseController.connect(true);
				} catch (DatabaseConfigurationException | DatabaseVersionException | SQLException e) {
					printErrorMessage("Failed to reconnect to the database", e);
				}
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
			printErrorMessage("Querying ADEs aborted", e);
			return;
		}
		
		try {	
			adeRegistor.initDBConneciton();
			List<ADEMetadataInfo> adeList = adeRegistor.queryRegisteredADEs();
			if (adeList.size() == 0) 
				LOG.info("Status: No ADEs are registered in the connected database");
			
			adeTableModel.reset();			
			for (ADEMetadataInfo adeEntity: adeList) {
				if (adeEntity == null) 
					continue; 				
				adeTableModel.addNewRow(new ADEMetadataRow(adeEntity));
			}
		} catch (ADERegistrationException e) {
			printErrorMessage(e);
		} finally {
			adeRegistor.closeDBConnection();
		}
	}
	
	private void deregisterADE(){
		setSettings();	
		
		dbPool.purge();
		
		int selectedRowNum = adeTable.getSelectedRow();
		if (selectedRowNum == -1) {
			viewContoller.errorMessage("ADE Deregistration aborted", "Please select one of the listed ADEs");
			return;
		}
		
		boolean isComplete = false;
		String adeId = adeTableModel.getColumn(selectedRowNum).getValue(0);	
		try {
			adeRegistor.initDBConneciton();
			isComplete = adeRegistor.deregisterADE(adeId);
			adeRegistor.commitTransactions();
		} catch (ADERegistrationException e) {
			adeRegistor.rollbackTransactions();
			printErrorMessage("ADE Deregistration aborted", e);
		} finally {
			adeRegistor.closeDBConnection();
		}	
		
		if (isComplete) {
			// database re-connection is required for completing the ADE de-registration process
			LOG.info("ADE Deregistration is completed and will take effect after reconnecting to the database.");
			if (dbPool.isConnected()) {
				dbPool.disconnect();
				try {
					databaseController.connect(true);
				} catch (DatabaseConfigurationException | DatabaseVersionException | SQLException e) {
					printErrorMessage("Failed to reconnect to the database", e);
				}
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
			printErrorMessage("Delete-script creation aborted", e);
			return;
		}
		
		try {			
			adeRegistor.initDBConneciton();
			boolean autoInstall = false;
			adeRegistor.createDeleteScripts(autoInstall);
		} catch (ADERegistrationException e) {
			printErrorMessage("Delete-script creation aborted", e);
		} finally {
			adeRegistor.closeDBConnection();			
		}	
	}
	

	@Override
	public void handleEvent(Event event) throws Exception {
		if (event.getEventType() == org.citydb.plugins.ade_manager.event.EventType.SCRIPT_CREATION_EVENT) {
            ScriptCreationEvent scriptCreationEvent = (ScriptCreationEvent) event;
            String script = scriptCreationEvent.getScript();
            boolean autoInstall = scriptCreationEvent.isAutoInstall();
            
            final ScriptDialog scriptDialog = new ScriptDialog(viewContoller.getTopFrame(), script, autoInstall);			
    		scriptDialog.getButton().addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				SwingUtilities.invokeLater(new Runnable() {
    					public void run() {
    						try {
    							adeRegistor.initDBConneciton();
    							adeRegistor.installDeleteScript(scriptDialog.getScript());
    							adeRegistor.commitTransactions();
    						} catch (ADERegistrationException e) {
    							adeRegistor.rollbackTransactions();
    							printErrorMessage(e);
    						} finally {
    							adeRegistor.closeDBConnection();
    							scriptDialog.dispose();
    						}  						
    					}
    				});
    			}
    		});

    		SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    				scriptDialog.setLocationRelativeTo(parentPanel.getTopLevelAncestor());
    				scriptDialog.setVisible(true);
    			}
    		});
        }		
	
	}

}
