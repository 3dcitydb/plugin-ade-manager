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
