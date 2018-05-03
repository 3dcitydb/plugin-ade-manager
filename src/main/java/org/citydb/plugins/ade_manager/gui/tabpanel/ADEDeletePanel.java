package org.citydb.plugins.ade_manager.gui.tabpanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.citydb.config.project.database.DBOperationType;
import org.citydb.config.project.exporter.SimpleQuery;
import org.citydb.config.project.query.filter.selection.SimpleSelectionFilterMode;
import org.citydb.config.project.query.filter.type.FeatureTypeFilter;
import org.citydb.event.Event;
import org.citydb.event.EventHandler;
import org.citydb.event.global.PropertyChangeEvent;
import org.citydb.gui.components.checkboxtree.DefaultCheckboxTreeCellRenderer;
import org.citydb.gui.components.feature.FeatureTypeTree;
import org.citydb.gui.factory.PopupMenuDecorator;
import org.citydb.gui.util.GuiUtil;
import org.citydb.modules.database.gui.operations.DatabaseOperationView;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citygml4j.model.module.citygml.CityGMLVersion;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.prompt.PromptSupport.FocusBehavior;

public class ADEDeletePanel extends DatabaseOperationView implements EventHandler {
	private JPanel featureClassfilterPanel;
	private FeatureTypeTree typeTree;	
	private JLabel objectNumberLabel = new JLabel();
	private JXTextField objectNumberInputField = new JXTextField();
	private JButton deleteButton = new JButton();
	private JPanel component;
	
	private final ConfigImpl config;
	
	public ADEDeletePanel(ConfigImpl config) {
		this.config = config;
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

	@Override
	public void handleEvent(Event event) throws Exception {
		PropertyChangeEvent e = (PropertyChangeEvent)event;
		if (e.getPropertyName().equals("citygml.version"))
			typeTree.updateCityGMLVersion((CityGMLVersion)e.getNewValue(), true);
	}
	
}
