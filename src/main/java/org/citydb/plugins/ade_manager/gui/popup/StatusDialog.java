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
package org.citydb.plugins.ade_manager.gui.popup;

import org.citydb.gui.util.GuiUtil;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

@SuppressWarnings("serial")
public class StatusDialog extends JDialog {
	private JLabel messageLabel;
	private JProgressBar progressBar;
	private JPanel main;

	public StatusDialog(JFrame frame, String windowTitle, String statusMessage) {
		super(frame, windowTitle, true);	
		initGUI(windowTitle, statusMessage);
	}

	private void initGUI(String windowTitle, String statusMessage) {		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		
		messageLabel = new JLabel(statusMessage);
		messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD));
		progressBar = new JProgressBar();

		setLayout(new GridBagLayout()); {
			main = new JPanel();
			add(main, GuiUtil.setConstraints(0,0,1.0,0.0,GridBagConstraints.BOTH,5,5,5,5));
			main.setLayout(new GridBagLayout());
			{
				main.add(messageLabel, GuiUtil.setConstraints(0,0,0.0,0.5,GridBagConstraints.HORIZONTAL,5,5,5,5));
				main.add(progressBar, GuiUtil.setConstraints(0,1,1.0,0.0,GridBagConstraints.HORIZONTAL,5,5,5,5));		
			}

			pack();
			progressBar.setIndeterminate(true);
		}
	}

}
