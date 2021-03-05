package vms.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.Arrays;
import java.util.function.BiConsumer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import util.Util;

import vms.VMS;

import vms.exception.VMSArgumentException;
import vms.exception.VMSException;

import vms.user.User;

public final class LoginPane {

	private Window window;

	private JTabbedPane pane;

	private JButton defaultButton;

	public LoginPane(Window window) {
		this.window = window;

		pane = new JTabbedPane();

		createLoginPanel();

		pane.setEnabledAt(0, false);

		createFilesPanel();

		if (!pane.isEnabledAt(1)) {
			pane.setSelectedIndex(0);
		}

		show();
	}

	private static enum SelectedFile {

		CAMPAIGNS_FILE(0, "campaignsFile", "campaigns"),
		USERS_FILE(1, "usersFile", "users");

		private int index;
		// private String key;
		private String string;

		private SelectedFile(int index, String key, String string) {
			this.index = index;
			// this.key = key;
			this.string = string;
		}

	}

	private boolean hasLoggedInAfterFileLoad = false;

	private JPanel createFilesPanel() {
		JPanel panel = new JPanel();

		pane.insertTab("Files", null, panel, null, 0);

		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		JLabel campaignsFileLabel = new JLabel("Campaigns file");
		panel.add(campaignsFileLabel);

		JTextField campaignsFileField = new JTextField(Window.INHERIT_STATE_FROM_LAST_TEST ? "Inherited from last test" : "No file selected");
		campaignsFileField.setEditable(false);
		panel.add(campaignsFileField);

		JButton campaignsFileButton = new JButton("Choose");
		panel.add(campaignsFileButton);

		JLabel usersFileLabel = new JLabel("Users file");
		panel.add(usersFileLabel);

		JTextField usersFileField = new JTextField(Window.INHERIT_STATE_FROM_LAST_TEST ? "Inherited from last test" : "No file selected");
		usersFileField.setEditable(false);
		panel.add(usersFileField);

		JButton usersFileButton = new JButton("Choose");
		panel.add(usersFileButton);

		Boolean[] isFileSelected = new Boolean[SelectedFile.values().length];
		isFileSelected[SelectedFile.CAMPAIGNS_FILE.index] = false;
		isFileSelected[SelectedFile.USERS_FILE.index] = false;

		BiConsumer<SelectedFile, File> loadFile = (selectedFile, file) -> {
			JTextField fileField = null;

			try {
				switch (selectedFile) {
				case CAMPAIGNS_FILE:
					fileField = campaignsFileField;

					VMS.getInstance().clearCampaigns();
					Window.getInstance().clearPaneMaps();

					if (
						Window.INHERIT_STATE_FROM_LAST_TEST ||
						hasLoggedInAfterFileLoad && isFileSelected[SelectedFile.USERS_FILE.index]
					) {
						Window.INHERIT_STATE_FROM_LAST_TEST = false;

						isFileSelected[SelectedFile.USERS_FILE.index] = false;

						VMS.getInstance().clearUsers();
						usersFileField.setText("No file selected");

						hasLoggedInAfterFileLoad = false;
					}

					VMS.getInstance().addCampaigns(file);

					break;

				case USERS_FILE:
					fileField = usersFileField;

					if (
						Window.INHERIT_STATE_FROM_LAST_TEST ||
						hasLoggedInAfterFileLoad && isFileSelected[SelectedFile.CAMPAIGNS_FILE.index]
					) {
						Window.INHERIT_STATE_FROM_LAST_TEST = false;

						isFileSelected[SelectedFile.CAMPAIGNS_FILE.index] = false;

						VMS.getInstance().clearCampaigns();

						campaignsFileField.setText("No file selected");

						hasLoggedInAfterFileLoad = false;
					}

					VMS.getInstance().clearUsers();
					Window.getInstance().clearPaneMaps();

					VMS.getInstance().addUsers(file);

					break;

				default:
				}
			} catch (VMSException ex) {
				isFileSelected[selectedFile.index] = false;

				pane.setEnabledAt(1, false);

				fileField.setText(Util.getExceptionRootCause(ex).getMessage());

				Window.getInstance().clearPaneMaps();

				window.exception(Util.joinExceptionCauseChain(ex));

				return;
			}

			fileField.setText(file.getAbsolutePath());

			isFileSelected[selectedFile.index] = true;

			if (!Arrays.asList(isFileSelected).contains(false)) {
				pane.setEnabledAt(1, true);
			} else {
				pane.setEnabledAt(1, false);
			}
		};

		/* if (!Window.INHERIT_STATE_FROM_LAST_TEST) {
			try {
				loadFile.accept(SelectedFile.CAMPAIGNS_FILE, new File(VMS.getInstance().getConfig().get("campaignsFile")));
			} catch (NullPointerException e) {}

			try {
				loadFile.accept(SelectedFile.USERS_FILE, new File(VMS.getInstance().getConfig().get("usersFile")));
			} catch (NullPointerException e) {}
		} */

		ActionListener buttonClickListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SelectedFile selectedFile = e.getSource() == campaignsFileButton ? SelectedFile.CAMPAIGNS_FILE : SelectedFile.USERS_FILE;

				JFileChooser fileChooser = new JFileChooser(".");
				fileChooser.setDialogTitle(String.format("Choose %s file", selectedFile.string));
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "text"));

				int option = fileChooser.showOpenDialog(window.get());

				if (option != JFileChooser.APPROVE_OPTION) {
					return;
				}

				File file = fileChooser.getSelectedFile();

				loadFile.accept(selectedFile, file);
			}

		};

		campaignsFileButton.addActionListener(buttonClickListener);
		usersFileButton.addActionListener(buttonClickListener);

		if (Window.INHERIT_STATE_FROM_LAST_TEST) {
			pane.setEnabledAt(1, true);
		}

		return panel;
	}

	private JPanel createLoginPanel() {
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		JLabel emailLabel = new JLabel("Email");
		panel.add(emailLabel);

		JTextField emailField = new JTextField();
		panel.add(emailField);

		JLabel passwordLabel = new JLabel("Password");
		panel.add(passwordLabel);

		JTextField passwordField = new JPasswordField();
		panel.add(passwordField);

		JCheckBox rememberCheckBox = new JCheckBox("Remember me");

		rememberCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (rememberCheckBox.isSelected()) {
					VMS.getInstance().getConfig().set("memorizeCredentials", "");
				} else {
					VMS.getInstance().getConfig().remove("memorizeCredentials");
				}
			}

		});

		panel.add(rememberCheckBox);

		if (VMS.getInstance().getConfig().get("memorizeCredentials") != null) {
			rememberCheckBox.setSelected(true);
		}

		emailField.setText(VMS.getInstance().getConfig().get("memorizedEmail"));

		passwordField.setText(VMS.getInstance().getConfig().get("memorizedPassword"));

		JButton loginButton = defaultButton = new JButton("Log in");

		loginButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				User user;

				Runnable showCredentialsExceptionDialog = () -> window.exception("Invalid credentials provided");

				try {
					user = VMS.getInstance().getUserFromEmail(emailField.getText());
				} catch (VMSArgumentException ex) {
					showCredentialsExceptionDialog.run();
					return;
				}

				if (user.getPassword().compareTo(passwordField.getText()) != 0) {
					showCredentialsExceptionDialog.run();
					return;
				}

				if (rememberCheckBox.isSelected()) {
					VMS.getInstance().getConfig().set("memorizedEmail", emailField.getText());
					VMS.getInstance().getConfig().set("memorizedPassword", passwordField.getText());
				}

				VMS.getInstance().setAuthenticatedUser(user);

				hasLoggedInAfterFileLoad = true;

				switch (user.getType()) {
				case ADMIN: {
					int option = window.confirmation("Would you like to access the administrative pane?");

					hide();

					if (option == JOptionPane.YES_OPTION) {
						window.getAdminPane(user).show();
					} else {
						window.getGuestPane(user).show();
					}

					break;
				}

				case GUEST:
					hide();

					window.getGuestPane(user).show();

					break;

				default:
				}
			}

		});

		panel.add(loginButton);

		pane.addTab("Login", panel);

		return panel;
	}

	public JTabbedPane get() {
		return pane;
	}

	public void hide() {
		window.get().getRootPane().setDefaultButton(null);

		pane.setVisible(false);
	}

	public void show() {
		pane.setVisible(true);

		window.get().getRootPane().setDefaultButton(defaultButton);
	}

}