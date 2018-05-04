package org.citydb.plugins.ade_manager.gui.tabpanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DBOperationType;
import org.citydb.config.project.exporter.SimpleQuery;
import org.citydb.config.project.query.filter.selection.SimpleSelectionFilterMode;
import org.citydb.config.project.query.filter.type.FeatureTypeFilter;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.event.Event;
import org.citydb.event.EventHandler;
import org.citydb.event.global.PropertyChangeEvent;
import org.citydb.gui.components.checkboxtree.DefaultCheckboxTreeCellRenderer;
import org.citydb.gui.components.feature.FeatureTypeTree;
import org.citydb.gui.factory.PopupMenuDecorator;
import org.citydb.gui.util.GuiUtil;
import org.citydb.log.Logger;
import org.citydb.modules.database.gui.operations.DatabaseOperationView;
import org.citydb.plugin.extension.view.ViewController;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.deletion.DBDeleteController;
import org.citydb.plugins.ade_manager.deletion.DBDeleteException;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citydb.query.Query;
import org.citydb.query.builder.QueryBuildException;
import org.citydb.query.builder.config.ConfigQueryBuilder;
import org.citydb.registry.ObjectRegistry;
import org.citygml4j.model.module.citygml.CityGMLVersion;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.prompt.PromptSupport.FocusBehavior;

public class ADEDeletePanel extends DatabaseOperationView implements EventHandler {
	private final Logger LOG = Logger.getInstance();
	private final ADEManagerPanel parentPanel;
	private final ViewController viewContoller;
	
	private JPanel featureClassfilterPanel;
	private FeatureTypeTree typeTree;	
	private JLabel objectNumberLabel = new JLabel();
	private JXTextField objectNumberInputField = new JXTextField();
	private JButton deleteButton = new JButton();
	private JPanel component;
	
	private final ConfigImpl config;
	
	public ADEDeletePanel(ADEManagerPanel parentPanel, ConfigImpl config) {
		this.parentPanel = parentPanel;
		this.config = config;
		this.viewContoller = this.parentPanel.getViewController();
		
		initGui();
	}
	
	private void initGui() {	
		int BORDER_THICKNESS = ADEManagerPanel.BORDER_THICKNESS;

		component = new JPanel();
		component.setLayout(new GridBagLayout());
		
		typeTree = new FeatureTypeTree(CityGMLVersion.v2_0_0);
		typeTree.setRowHeight((int)(new JCheckBox().getPreferredSize().getHeight()) - 4);		
		typeTree.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), 
				BorderFactory.createEmptyBorder(0, 0, 4, 4)));
		PopupMenuDecorator.getInstance().decorate(typeTree);
		
		// get rid of standard icons
		DefaultCheckboxTreeCellRenderer renderer = (DefaultCheckboxTreeCellRenderer)typeTree.getCellRenderer();
		renderer.setLeafIcon(null);
		renderer.setOpenIcon(null);
		renderer.setClosedIcon(null);
				
		featureClassfilterPanel = new JPanel();		
		featureClassfilterPanel.setLayout(new GridBagLayout());
		featureClassfilterPanel.setBorder(BorderFactory.createTitledBorder(""));
		featureClassfilterPanel.add(typeTree, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));	
	
		JPanel deletePanel = new JPanel();
		deletePanel.setLayout(new GridBagLayout());
		deletePanel.add(objectNumberLabel, GuiUtil.setConstraints(0,0,0.0,0.0,GridBagConstraints.BOTH,0,0,0,5));
		deletePanel.add(objectNumberInputField, GuiUtil.setConstraints(1,0,1.0,0.0,GridBagConstraints.HORIZONTAL,0,0,0,5));
		deletePanel.add(deleteButton, GuiUtil.setConstraints(2,0,0.0,0.0,GridBagConstraints.NONE,0,5,0,0));	
		objectNumberInputField.setPromptForeground(Color.LIGHT_GRAY);
		objectNumberInputField.setFocusBehavior(FocusBehavior.SHOW_PROMPT);
		
		int index = 0;
		component.add(featureClassfilterPanel, GuiUtil.setConstraints(0,index++,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		component.add(deletePanel, GuiUtil.setConstraints(0,index++,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
	
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						doDelete();						
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
	}
	
	@Override
	public String getLocalizedTitle() {
		return "Deletion";
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
		objectNumberLabel.setText("Maximum number of Toplevel-Features to be deleted:");
		objectNumberInputField.setPrompt(("defulat is unlimited"));
		deleteButton.setText("Delete");
		((TitledBorder) featureClassfilterPanel.getBorder()).setTitle("Feature Classes");
	}

	@Override
	public void setEnabled(boolean enable) {
		// 
	}

	@Override
	public void loadSettings() {
		SimpleQuery query = config.getDeleteQuery();;
		FeatureTypeFilter featureTypeFilter = query.getFeatureTypeFilter();
		typeTree.getCheckingModel().clearChecking();
		typeTree.setSelected(featureTypeFilter.getTypeNames());
		typeTree.repaint();
	}

	@Override
	public void setSettings() {
		SimpleQuery query = config.getDeleteQuery();
		query.setMode(SimpleSelectionFilterMode.COMPLEX);
		query.setUseTypeNames(true);
		FeatureTypeFilter featureTypeFilter = query.getFeatureTypeFilter();
		featureTypeFilter.reset();
		featureTypeFilter.setTypeNames(typeTree.getSelectedTypeNames());		
	}
	
	private void doDelete() {
		viewContoller.clearConsole();
		setSettings();

		SimpleQuery simpleQueryConfig = config.getDeleteQuery();
		if (simpleQueryConfig.getFeatureTypeFilter().getTypeNames().isEmpty()) {
			viewContoller.errorMessage(Language.I18N.getString("export.dialog.error.incorrectData"),
					Language.I18N.getString("common.dialog.error.incorrectData.featureClass"));
			return;
		}
		
		final AbstractDatabaseAdapter databaseAdapter = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter();
		final SchemaMapping schemaMapping = ObjectRegistry.getInstance().getSchemaMapping();

		// build query from filter settings
		Query query = null;
		try {
			ConfigQueryBuilder queryBuilder = new ConfigQueryBuilder(schemaMapping, databaseAdapter);
			query = queryBuilder.buildQuery(config.getDeleteQuery(), config.getNamespaceFilter());
		} catch (QueryBuildException e) {
			LOG.error("Failed to build the export query expression.");
			return;
		}

		viewContoller.setStatusText("Delete");
		LOG.info("Initializing database delete...");

		DBDeleteController deleter = new DBDeleteController(query);
		boolean success = false;
		try {
			success = deleter.doProcess();
		} catch (DBDeleteException e) {
			LOG.error(e.getMessage());

			Throwable cause = e.getCause();
			while (cause != null) {
				LOG.error("Cause: " + cause.getMessage());
				cause = cause.getCause();
			}
		}

		// cleanup
		deleter.cleanup();

		if (success) {
			LOG.info("Database delete successfully finished.");
		} else {
			LOG.warn("Database delete aborted.");
		}

		viewContoller.setStatusText(Language.I18N.getString("main.status.ready.label"));
	}

	@Override
	public void handleEvent(Event event) throws Exception {
		PropertyChangeEvent e = (PropertyChangeEvent)event;
		if (e.getPropertyName().equals("citygml.version"))
			typeTree.updateCityGMLVersion((CityGMLVersion)e.getNewValue(), true);
	}
	
}
