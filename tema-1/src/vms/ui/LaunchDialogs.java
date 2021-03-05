package vms.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import util.Util;

import vms.VMS;

import vms.campaign.Campaign;

import vms.exception.VMSException;

import vms.user.User;

public final class LaunchDialogs {

	public static boolean showLastTestConfirmDialog() {
		int option = JOptionPane.showConfirmDialog(
			null,
			"Would you like to run the last known run test?",
			Window.CONFIRMATION_DIALOG_TITLE,
			JOptionPane.YES_NO_OPTION
		);

		return option == JOptionPane.YES_OPTION;
	}

	public static void showLastTestDialog() {
		String campaignsFilePath = VMS.getInstance().getConfig().get(TestSelectedFile.CAMPAIGNS_FILE.key);
		String usersFilePath = VMS.getInstance().getConfig().get(TestSelectedFile.USERS_FILE.key);
		String eventsFilePath = VMS.getInstance().getConfig().get(TestSelectedFile.EVENTS_FILE.key);

		if (campaignsFilePath == null || usersFilePath == null || eventsFilePath == null) {
			return;
		}

		JTextField campaignsFileField = new JTextField(campaignsFilePath);
		campaignsFileField.setEditable(false);

		JTextField usersFileField = new JTextField(usersFilePath);
		usersFileField.setEditable(false);

		JTextField eventsFileField = new JTextField(eventsFilePath);
		eventsFileField.setEditable(false);

		int option = JOptionPane.showOptionDialog(
			null,

			new Object[] {
				"Campaigns file", campaignsFileField,
				"Users file", usersFileField,
				"Events file", eventsFileField,
			},

			"Run test",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,

			new Object[] {"Run", "Cancel"},
			"Run"
		);

		if (option == JOptionPane.YES_OPTION) {
			try {
				VMS.Test test = new VMS.Test(
					new File(campaignsFilePath),
					new File(usersFilePath),
					new File(eventsFilePath)
				);

				test.run();
			} catch (VMSException e) {
				JOptionPane.showMessageDialog(
					null,
					Util.joinExceptionCauseChain(e),
					Window.EXCEPTION_DIALOG_TITLE,
					JOptionPane.ERROR_MESSAGE
				);
			}
		}
	}

	public static boolean showTestConfirmDialog() {
		int option = JOptionPane.showConfirmDialog(
			null,
			"Would you like to run a new test?",
			Window.CONFIRMATION_DIALOG_TITLE,
			JOptionPane.YES_NO_OPTION
		);

		return option == JOptionPane.YES_OPTION;
	}

	private static enum TestSelectedFile {

		CAMPAIGNS_FILE(0, "testCampaignsFile", "campaigns"),
		USERS_FILE(1, "testUsersFile", "users"),
		EVENTS_FILE(2, "testEventsFile", "events");

		private int index;
		private String key;
		private String string;

		private TestSelectedFile(int index, String key, String string) {
			this.index = index;
			this.key = key;
			this.string = string;
		}

		@Override
		public String toString() {
			return string;
		}

	}

	public static void showTestDialog() {
		JButton runButton = new JButton("Run");
		runButton.setEnabled(false);

		JButton cancelButton = new JButton("Cancel");

		JButton[] options = new JButton[] {runButton, cancelButton};

		for (JButton option : options) {
			option.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane pane = Util.getComponentOptionPane((JComponent)e.getSource());
					pane.setValue(option);
				}

			});
		}

		List<Object> message = new ArrayList<>();

		Collection<?>[] testData = new Collection<?>[TestSelectedFile.values().length];
		testData[TestSelectedFile.CAMPAIGNS_FILE.index] = null;
		testData[TestSelectedFile.USERS_FILE.index] = null;
		testData[TestSelectedFile.EVENTS_FILE.index] = null;

		Arrays.stream(TestSelectedFile.values()).forEach(selectedFile -> {
			message.add(String.format("%s file", Util.capitalizeString(selectedFile.toString())));

			JPanel panel = new JPanel(new BorderLayout());

			JTextField fileField = new JTextField("No file selected", 32);
			fileField.setEditable(false);
			panel.add(fileField, BorderLayout.CENTER);

			JButton fileButton = new JButton("Choose");

			fileButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser fileChooser = new JFileChooser(".");
					fileChooser.setDialogTitle(String.format("Choose %s file",  selectedFile.toString()));
					fileChooser.setAcceptAllFileFilterUsed(false);
					fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "text"));

					int option = fileChooser.showOpenDialog(null);

					if (option != JFileChooser.APPROVE_OPTION) {
						return;
					}

					File file = fileChooser.getSelectedFile();

					VMS.Reader reader = VMS.getInstance().getReader();

					try {
						switch (selectedFile) {
						case CAMPAIGNS_FILE:
							reader.setCampaignsFile(file);
							testData[selectedFile.index] = reader.readCampaigns();
							break;

						case USERS_FILE:
							reader.setUsersFile(file);
							testData[selectedFile.index] = reader.readUsers();
							break;

						case EVENTS_FILE:
							reader.setEventsFile(file);
							testData[selectedFile.index] = reader.readEvents();
							break;
						}

						VMS.getInstance().getConfig().set(selectedFile.key, file.getAbsolutePath());

						fileField.setText(file.getAbsolutePath());

						if (!Arrays.asList(testData).contains(null)) {
							runButton.setEnabled(true);
						}
					} catch (VMSException ex) {
						testData[selectedFile.index] = null;

						runButton.setEnabled(false);

						fileField.setText(Util.getExceptionRootCause(ex).getMessage());

						JOptionPane.showMessageDialog(
							null,
							Util.joinExceptionCauseChain(ex),
							Window.EXCEPTION_DIALOG_TITLE,
							JOptionPane.ERROR_MESSAGE
						);
					}
				}

			});

			panel.add(fileButton, BorderLayout.LINE_END);

			message.add(panel);
		});

		int option = JOptionPane.showOptionDialog(
			null,
			message.toArray(),
			"Run test",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,

			options, cancelButton
		);

		if (option == JOptionPane.OK_OPTION) {
			@SuppressWarnings("unchecked")
			List<Campaign> campaignList = (List<Campaign>)testData[TestSelectedFile.CAMPAIGNS_FILE.index];

			@SuppressWarnings("unchecked")
			List<User> userList = (List<User>)testData[TestSelectedFile.USERS_FILE.index];

			@SuppressWarnings("unchecked")
			Queue<VMS.Test.Event> eventQueue = (Queue<VMS.Test.Event>)testData[TestSelectedFile.EVENTS_FILE.index];

			VMS.Test test = new VMS.Test(campaignList, userList, eventQueue);
			test.run();
		}
	}

	private LaunchDialogs() {
		throw new UnsupportedOperationException();
	}

}