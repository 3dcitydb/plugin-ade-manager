
package org.citydb.plugins.ade_manager.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.citydb.config.i18n.Language;
import org.citydb.event.Event;
import org.citydb.event.EventHandler;
import org.citydb.event.global.DatabaseConnectionStateEvent;
import org.citydb.gui.util.GuiUtil;
import org.citydb.log.Logger;
import org.citydb.modules.database.gui.operations.DatabaseOperationView;
import org.citydb.plugin.extension.view.ViewController;
import org.citydb.plugins.ade_manager.ADEManagerPlugin;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.gui.table.ADESchemaNamespaceRow;
import org.citydb.plugins.ade_manager.gui.table.TableModel;
import org.citydb.plugins.ade_manager.gui.tabpanel.ADEDeletePanel;
import org.citydb.plugins.ade_manager.gui.tabpanel.ADERegistryPanel;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.transformation.TransformationException;
import org.citydb.plugins.ade_manager.transformation.TransformationController;

@SuppressWarnings("serial")
public class ADEManagerPanel extends JPanel implements EventHandler {	
	public static final int BORDER_THICKNESS = 4;
	public static final int MAX_TEXTFIELD_HEIGHT = 20;
	public static final int MAX_LABEL_WIDTH = 60;
	public static final int BUTTON_WIDTH = 180;
		
	private JPanel browseXMLSchemaPanel;
	private JTextField browseXMLSchemaText = new JTextField();
	private JButton browseXMLSchemaButton = new JButton();
	private JButton readXMLSchemaButton = new JButton();	
	private JTable schemaTable;
	private JScrollPane schemaPanel;	
	private JPanel namePanel;
	private JTextField nameInputField = new JTextField();	
	private JPanel descriptionPanel;
	private JTextField descriptionInputField = new JTextField();
	private JPanel versionPanel;
	private JTextField versionInputField = new JTextField();
	private JPanel dbPrefixPanel;
	private JTextField dbPrefixInputField = new JTextField();
	private JPanel initObjectClassIdPanel;
	private JTextField initObjectClassIdInputField = new JTextField();	
	private JPanel transformationOutputPanel;
	private JTextField browseOutputText = new JTextField();
	private JButton browserOutputButton = new JButton();
	private JButton transformAndExportButton = new JButton();
	private int standardButtonHeight = (new JButton("D")).getPreferredSize().height;
	
	private JTabbedPane databaseOperationTab;
	private DatabaseOperationView[] databaseOperationPanels;
	
	private TableModel<ADESchemaNamespaceRow> schemaTableModel = new TableModel<ADESchemaNamespaceRow>(ADESchemaNamespaceRow.getColumnNames());	
	private final Logger LOG = Logger.getInstance();
	
	private ViewController viewController;	
	private TransformationController adeTransformer;
	
	private ConfigImpl config;
	
	public ADEManagerPanel(ViewController viewController, ADEManagerPlugin plugin) {	
		this.config = plugin.getConfig();		
		this.viewController = viewController;
		
		this.adeTransformer = new TransformationController(config);
		
		initGui();
		addListeners();	
		setEnabledMetadataSettings(false);
	}

	public ViewController getViewController() {
		return viewController;
	}

	private void initGui() {	
		readXMLSchemaButton.setPreferredSize(new Dimension(BUTTON_WIDTH, getStandardButtonHeight()));
		transformAndExportButton.setPreferredSize(new Dimension(BUTTON_WIDTH, getStandardButtonHeight()));
	
		// Input panel
		browseXMLSchemaPanel = new JPanel();
		browseXMLSchemaPanel.setLayout(new GridBagLayout());
		browseXMLSchemaPanel.add(browseXMLSchemaText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		browseXMLSchemaPanel.add(browseXMLSchemaButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));	
				
		// Schema table panel
		schemaTable = new JTable(schemaTableModel);
		schemaTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		schemaTable.setCellSelectionEnabled(false);
		schemaTable.setColumnSelectionAllowed(false);
		schemaTable.setRowSelectionAllowed(true);
		schemaTable.setRowHeight(25);		
		schemaPanel = new JScrollPane(schemaTable);
		schemaPanel.setPreferredSize(new Dimension(browseXMLSchemaText.getPreferredSize().width, 200));
		schemaPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 0, 0, 0)));
		
		// metadata parameters
		namePanel = new JPanel();
		namePanel.setLayout(new GridBagLayout());
		namePanel.add(nameInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS));
		nameInputField.setColumns(10);

		descriptionPanel = new JPanel();
		descriptionPanel.setLayout(new GridBagLayout());
		descriptionPanel.add(descriptionInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS));
		descriptionInputField.setColumns(10);
		
		versionPanel = new JPanel();
		versionPanel.setLayout(new GridBagLayout());
		versionPanel.add(versionInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS));
		versionInputField.setColumns(10);
		
		dbPrefixPanel = new JPanel();
		dbPrefixPanel.setLayout(new GridBagLayout());
		dbPrefixPanel.add(dbPrefixInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS));
		dbPrefixInputField.setColumns(10);
		
		initObjectClassIdPanel = new JPanel();
		initObjectClassIdPanel.setLayout(new GridBagLayout());
		initObjectClassIdPanel.add(initObjectClassIdInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS));
		initObjectClassIdInputField.setColumns(10);
		
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
		schemaAndMetadataPanel.add(schemaPanel, GuiUtil.setConstraints(0,0,0.7,0,GridBagConstraints.BOTH,0,0,0,0));
		schemaAndMetadataPanel.add(Box.createRigidArea(new Dimension(BORDER_THICKNESS, 0)), GuiUtil.setConstraints(1,0,0,0,GridBagConstraints.NONE,0,0,0,0));
		schemaAndMetadataPanel.add(metadataInputPanel, GuiUtil.setConstraints(2,0,0.3,0,GridBagConstraints.BOTH,0,0,0,0));
	
		// Export panel
		transformationOutputPanel = new JPanel();
		transformationOutputPanel.setLayout(new GridBagLayout());
		transformationOutputPanel.setBorder(BorderFactory.createTitledBorder(""));
		transformationOutputPanel.add(browseOutputText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		transformationOutputPanel.add(browserOutputButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));

		// TabPanels for database performing database transactions
		databaseOperationTab = new JTabbedPane();
		GridBagConstraints c = GuiUtil.setConstraints(0,2,1.0,1.0,GridBagConstraints.BOTH,5,0,0,0);
		c.gridwidth = 4;
		add(databaseOperationTab, c);

		databaseOperationPanels = new DatabaseOperationView[2];
		databaseOperationPanels[0] = new ADERegistryPanel(this, config);
		databaseOperationPanels[1] = new ADEDeletePanel(this, config);

		for (int i = 0; i < databaseOperationPanels.length; ++i)
			databaseOperationTab.insertTab(null, databaseOperationPanels[i].getIcon(), null, databaseOperationPanels[i].getToolTip(), i);

		databaseOperationTab.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {			
				int index = databaseOperationTab.getSelectedIndex();
				for (int i = 0; i < databaseOperationTab.getTabCount(); ++i)
					databaseOperationTab.setComponentAt(i, index == i ? databaseOperationPanels[index].getViewComponent() : null);
			}
		});
		
		// Assemble all panels
		JPanel mainScrollView = new JPanel();
		mainScrollView.setLayout(new GridBagLayout());
		
		int index = 0;		
		mainScrollView.add(browseXMLSchemaPanel, GuiUtil.setConstraints(0,index++,0.0,0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		mainScrollView.add(readXMLSchemaButton, GuiUtil.setConstraints(0,index++,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS*2,BORDER_THICKNESS));
		mainScrollView.add(schemaAndMetadataPanel, GuiUtil.setConstraints(0,index++,0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		mainScrollView.add(transformationOutputPanel, GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		mainScrollView.add(transformAndExportButton, GuiUtil.setConstraints(0,index++,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,0,BORDER_THICKNESS));
		mainScrollView.add(databaseOperationTab, GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.BOTH,0,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		mainScrollView.add(Box.createVerticalGlue(), GuiUtil.setConstraints(0,index++,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));

		JScrollPane mainScrollPanel = new JScrollPane(mainScrollView);
		mainScrollPanel.setBorder(BorderFactory.createEmptyBorder());
		mainScrollPanel.setViewportBorder(BorderFactory.createEmptyBorder());
		this.setLayout(new GridBagLayout());	
		this.add(mainScrollPanel, GuiUtil.setConstraints(0,1,1.0,1.0,GridBagConstraints.BOTH,5,0,0,0));
	}

	// localized Labels and Strings
	public void doTranslation() {	
		browseXMLSchemaPanel.setBorder(BorderFactory.createTitledBorder("XML Schema (XSD)"));

		namePanel.setBorder(BorderFactory.createTitledBorder("Name"));
		descriptionPanel.setBorder(BorderFactory.createTitledBorder("Description"));
		versionPanel.setBorder(BorderFactory.createTitledBorder("Version"));
		dbPrefixPanel.setBorder(BorderFactory.createTitledBorder("DB_Prefix"));
		initObjectClassIdPanel.setBorder(BorderFactory.createTitledBorder("InitialObjectclassId"));

		browseXMLSchemaButton.setText(Language.I18N.getString("common.button.browse"));
		readXMLSchemaButton.setText("Read XML Schema");
		((TitledBorder) transformationOutputPanel.getBorder()).setTitle("Output");
		browserOutputButton.setText(Language.I18N.getString("common.button.browse"));
		transformAndExportButton.setText("Transform");
		
		for (int i = 0; i < databaseOperationPanels.length; ++i) {
			databaseOperationTab.setTitleAt(i, databaseOperationPanels[i].getLocalizedTitle());
			databaseOperationPanels[i].doTranslation();
		}
	}

	public void loadSettings() {
		browseXMLSchemaText.setText(config.getXMLschemaInputPath());
		browseOutputText.setText(config.getTransformationOutputPath());
		nameInputField.setText(config.getAdeName());
		descriptionInputField.setText(config.getAdeDescription());
		versionInputField.setText(config.getAdeVersion());
		dbPrefixInputField.setText(config.getAdeDbPrefix());
		initObjectClassIdInputField.setText(String.valueOf(config.getInitialObjectclassId()));		
		
		int index = 0;
		for (int i = 0; i < databaseOperationPanels.length; ++i) {
			databaseOperationPanels[i].loadSettings();			
		}

		databaseOperationTab.setSelectedIndex(-1);
		databaseOperationTab.setSelectedIndex(index);
	}

	public void setSettings() {
		config.setXMLschemaInputPath(browseXMLSchemaText.getText());
		config.setTransformationOutputPath(browseOutputText.getText());
		config.setAdeName(nameInputField.getText());
		config.setAdeDescription(descriptionInputField.getText());
		config.setAdeVersion(versionInputField.getText());
		config.setAdeDbPrefix(dbPrefixInputField.getText());
		config.setInitialObjectclassId(Integer.valueOf(initObjectClassIdInputField.getText()));
		
		for (int i = 0; i < databaseOperationPanels.length; ++i)
			databaseOperationPanels[i].setSettings();
	}
	
	public int getStandardButtonHeight() {
		return standardButtonHeight;
	}
	
	public void printErrorMessage(Exception e) {
		printErrorMessage("Unexpected Error", e);
	}
	
	public void printErrorMessage(String info, Exception e) {
		LOG.error(info + ". Cause: " + e.getMessage());			
		Throwable cause = e.getCause();
		while (cause != null) {
			LOG.error("Cause: " + cause.getMessage());
			cause = cause.getCause();
		}
	}

	private void addListeners() {
		browseXMLSchemaButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browserXMLschemaFile();
			}
		});
		
		readXMLSchemaButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						parseADESchema();						
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
	
		schemaTable.addMouseListener(new MouseAdapter()
		{
		    public void mouseClicked(MouseEvent e)
		    {
		        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {	
		        	Thread thread = new Thread() {
						public void run() { 							
				        //	dummyFunc();
						}
					};
					thread.setDaemon(true);
					thread.start();
		        }
		    }
		});
		
		browserOutputButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browseTransformationOutputDirectory();
			}
		});
				
		transformAndExportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						transformADESchema();						
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
	}
	
	private void browserXMLschemaFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	
		FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Schema file (*.xsd)", "xsd");
		chooser.addChoosableFileFilter(filter);
		chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
		chooser.setFileFilter(filter);
	
		if (!browseXMLSchemaText.getText().trim().isEmpty())
			chooser.setCurrentDirectory(new File(browseXMLSchemaText.getText()));
	
		int result = chooser.showOpenDialog(getTopLevelAncestor());
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
		chooser.setDialogTitle("Ouput Folder");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setCurrentDirectory(new File(browseOutputText.getText()));
	
		int result = chooser.showSaveDialog(getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		String browseString = chooser.getSelectedFile().toString();
		if (!browseString.isEmpty())
			browseOutputText.setText(browseString);
	}

	private void parseADESchema() {
		viewController.clearConsole();
		schemaTableModel.reset();
		
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
		}
	}

	private void transformADESchema() {	
		setSettings();
	
		String adeName = config.getAdeName();
		if (adeName.trim().equals("")) {
			viewController.errorMessage("Incomplete Information", "Please enter a name for the ADE");
			return;
		}
		
		String dbPrefix = config.getAdeDbPrefix();
		if (dbPrefix.trim().equals("")) {
			viewController.errorMessage("Incomplete Information", "Please enter a name for the ADE");
			return;
		}
		
		int initialObjectclassId = config.getInitialObjectclassId();
		if (initialObjectclassId < ADEMetadataManager.MIN_ADE_OBJECTCLASSID) {
			viewController.errorMessage("Incorrect Information", "Then initial objectclass ID must be greater than or equal to 10000");
			return;
		}	
		
		int selectedRowNum = schemaTable.getSelectedRow();
		if (selectedRowNum == -1) {
			viewController.errorMessage("Incomplete Information", "Please select a schema namespace");
			return;
		}
		String selectedSchemaNamespace = schemaTableModel.getColumn(selectedRowNum).getValue(0);
	
		try {
			adeTransformer.doProcess(selectedSchemaNamespace);
		} catch (TransformationException e) {
			printErrorMessage(e);
			return;
		}
	
		LOG.info("Transformation finished");
	}

	public void handleEvent(Event event) throws Exception {
		DatabaseConnectionStateEvent state = (DatabaseConnectionStateEvent)event;
		for (int i = 0; i < databaseOperationPanels.length; ++i)
			databaseOperationPanels[i].handleDatabaseConnectionStateEvent(state);
	}

}
