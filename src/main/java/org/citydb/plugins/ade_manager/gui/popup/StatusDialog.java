/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * Copyright 2013 - 2017
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
/**
 * copy from matching plugin writen by Claus.
 */
package org.citydb.plugins.ade_manager.gui.popup;

import org.citydb.config.i18n.Language;
import org.citydb.event.Event;
import org.citydb.event.EventDispatcher;
import org.citydb.event.EventHandler;
import org.citydb.event.global.EventType;
import org.citydb.event.global.ObjectCounterEvent;
import org.citydb.event.global.ProgressBarEventType;
import org.citydb.event.global.StatusDialogMessage;
import org.citydb.event.global.StatusDialogProgressBar;
import org.citydb.event.global.StatusDialogTitle;
import org.citydb.gui.util.GuiUtil;
import org.citydb.registry.ObjectRegistry;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

@SuppressWarnings("serial")
public class StatusDialog extends JDialog implements EventHandler {
	final EventDispatcher eventDispatcher;
	
	private JLabel titleLabel;
	private JLabel messageLabel;
	private JProgressBar progressBar;
	private JLabel detailsLabel;
	private JPanel main;
	private JPanel row;
	private JLabel featureLabel;
	private JLabel remainingFeatureLabel;
	private JLabel featureCounterLabel;
	private JLabel remainingFeatureCounterLabel;
	
	private JButton button;
	private volatile boolean acceptStatusUpdate = true;
	private long featureCounter;
	private int progressBarCounter;
	private int maxFeatureNumber;
	
	public StatusDialog(JFrame frame, 
			String windowTitle, 
			String statusTitle,
			String statusMessage,
			boolean setButton) {
		super(frame, windowTitle, true);
		
		eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();
		eventDispatcher.addEventHandler(EventType.OBJECT_COUNTER, this);
		eventDispatcher.addEventHandler(EventType.STATUS_DIALOG_MESSAGE, this);
		eventDispatcher.addEventHandler(EventType.INTERRUPT, this);
		eventDispatcher.addEventHandler(EventType.STATUS_DIALOG_PROGRESS_BAR, this);
		
		initGUI(windowTitle, statusTitle, statusMessage, setButton);
	}

	private void initGUI(String windowTitle, 
			String statusTitle, 
			String statusMessage, 
			boolean setButton) {
		
		if (statusTitle == null)
			statusTitle = "";
		
		if (statusMessage == null)
			statusMessage = "";
		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		titleLabel = new JLabel(statusTitle);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		messageLabel = new JLabel(statusMessage);
		featureLabel = new JLabel("Deleted objects: ");
		remainingFeatureLabel = new JLabel("Remaining objects:");

		featureCounterLabel = new JLabel("0", SwingConstants.TRAILING);
		remainingFeatureCounterLabel = new JLabel("0", SwingConstants.TRAILING);
		featureCounterLabel.setPreferredSize(new Dimension(100, featureLabel.getPreferredSize().height));
		remainingFeatureCounterLabel.setPreferredSize(new Dimension(100, featureLabel.getPreferredSize().height));
		button = new JButton(Language.I18N.getString("common.button.cancel"));		
		progressBar = new JProgressBar();

		setLayout(new GridBagLayout()); {
			main = new JPanel();
			add(main, GuiUtil.setConstraints(0,0,1.0,0.0,GridBagConstraints.BOTH,5,5,5,5));
			main.setLayout(new GridBagLayout());
			{
				main.add(titleLabel, GuiUtil.setConstraints(0,0,0.0,0.5,GridBagConstraints.HORIZONTAL,5,5,5,5));
				main.add(messageLabel, GuiUtil.setConstraints(0,1,0.0,0.5,GridBagConstraints.HORIZONTAL,5,5,0,5));
				main.add(progressBar, GuiUtil.setConstraints(0,2,1.0,0.0,GridBagConstraints.HORIZONTAL,0,5,5,5));

				detailsLabel = new JLabel("Details");
				main.add(detailsLabel, GuiUtil.setConstraints(0,3,1.0,0.0,GridBagConstraints.HORIZONTAL,5,5,0,5));

				row = new JPanel();
				row.setBackground(new Color(255, 255, 255));
				row.setBorder(BorderFactory.createEtchedBorder());
				main.add(row, GuiUtil.setConstraints(0,4,1.0,0.0,GridBagConstraints.BOTH,0,5,5,5));
				row.setLayout(new GridBagLayout());
				{				
					row.add(featureLabel, GuiUtil.setConstraints(0,0,0.0,0.0,GridBagConstraints.HORIZONTAL,5,5,1,5));
					row.add(featureCounterLabel, GuiUtil.setConstraints(1,0,1.0,0.0,GridBagConstraints.HORIZONTAL,5,5,1,5));
					row.add(remainingFeatureLabel, GuiUtil.setConstraints(0,1,0.0,0.0,GridBagConstraints.HORIZONTAL,1,5,5,5));
					row.add(remainingFeatureCounterLabel, GuiUtil.setConstraints(1,1,1.0,0.0,GridBagConstraints.HORIZONTAL,1,5,5,5));
				}
			
			}

			if (setButton)
				add(button, GuiUtil.setConstraints(0,1,0.0,0.0,GridBagConstraints.NONE,5,5,5,5));

			pack();
			progressBar.setIndeterminate(true);
			
			addWindowListener(new WindowListener() {
				public void windowClosed(WindowEvent e) {
					eventDispatcher.removeEventHandler(StatusDialog.this);
				}
				public void windowActivated(WindowEvent e) {}
				public void windowClosing(WindowEvent e) {}
				public void windowDeactivated(WindowEvent e) {}
				public void windowDeiconified(WindowEvent e) {}
				public void windowIconified(WindowEvent e) {}
				public void windowOpened(WindowEvent e) {}
			});
		}
	}

	public JLabel getStatusTitleLabel() {
		return titleLabel;
	}

	public JLabel getStatusMessageLabel() {
		return messageLabel;
	}
	
	public JButton getCancelButton() {
		return button;
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	@Override
	public void handleEvent(Event e) throws Exception {
		if (e.getEventType() == EventType.OBJECT_COUNTER) {
			ObjectCounterEvent counter = (ObjectCounterEvent)e;
			featureCounter += counter.getCounter().size();
			featureCounterLabel.setText(String.valueOf(featureCounter));
		}
		else if (e.getEventType() == EventType.STATUS_DIALOG_PROGRESS_BAR) {
			StatusDialogProgressBar progressBarEvent = (StatusDialogProgressBar)e;
			
			if (progressBarEvent.getType() == ProgressBarEventType.INIT) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {		
						progressBar.setIndeterminate(progressBarEvent.isSetIntermediate());
					}
				});
				
				if (!progressBarEvent.isSetIntermediate()) {
					maxFeatureNumber = progressBarEvent.getValue();
					progressBar.setMaximum(maxFeatureNumber);
					progressBar.setValue(0);
					progressBarCounter = 0;
					remainingFeatureCounterLabel.setText(String.valueOf(maxFeatureNumber));
				}
			} else {
				progressBarCounter += progressBarEvent.getValue();
				progressBar.setValue(progressBarCounter);
				remainingFeatureCounterLabel.setText(String.valueOf(maxFeatureNumber - progressBarCounter));
			}
		}
		else if (e.getEventType() == EventType.INTERRUPT) {
			acceptStatusUpdate = false;
			messageLabel.setText(Language.I18N.getString("common.dialog.msg.abort"));
			progressBar.setIndeterminate(true);
		}
		else if (e.getEventType() == EventType.STATUS_DIALOG_MESSAGE && acceptStatusUpdate) {
			messageLabel.setText(((StatusDialogMessage)e).getMessage());
		}

		else if (e.getEventType() == EventType.STATUS_DIALOG_TITLE && acceptStatusUpdate) {
			titleLabel.setText(((StatusDialogTitle)e).getTitle());
		}

	}
	
}
