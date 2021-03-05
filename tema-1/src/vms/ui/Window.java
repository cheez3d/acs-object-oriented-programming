package vms.ui;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import vms.VMS;

import vms.exception.VMSArgumentException;

import vms.user.User;
import vms.user.UserType;

public final class Window {

	public static final String TITLE = "Voucher Management Service";

	public static final String CONFIRMATION_DIALOG_TITLE = "Confirmation";
	public static final String INFORMATION_DIALOG_TITLE = "Information";
	public static final String EXCEPTION_DIALOG_TITLE = "Exception";

	public static final String NEW_DIALOG_TITLE = "New";

	public static boolean INHERIT_STATE_FROM_LAST_TEST;

	private static Window instance;

	public static Window getInstance() {
		if (instance == null) {
			if (VMS.getInstance().getCampaignCount() > 0 || VMS.getInstance().getUserCount() > 0) {
				int option = JOptionPane.showConfirmDialog(
					null,
					"Would you like to inherit the state of the last run test?",
					CONFIRMATION_DIALOG_TITLE,
					JOptionPane.YES_NO_OPTION
				);

				INHERIT_STATE_FROM_LAST_TEST = option == JOptionPane.YES_OPTION;
			} else {
				INHERIT_STATE_FROM_LAST_TEST = false;
			}

			instance = new Window();

			instance.updateTask = new TimerTask() {

				private boolean firstRun = true;

				@Override
				public void run() {
					LocalDateTime now = LocalDateTime.now();

					if (!firstRun) {
						now = now.plusSeconds(30);
					} else {
						firstRun = false;
					}

					now = now.withSecond(0).withNano(0);

					instance.get().setTitle(String.format(
						"%s | %s",
						TITLE,
						DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT).format(now))
					);

					VMS.getInstance().setDateTime(now);

					synchronized (this) {
						JTable table = instance.currentEditableCampaignTable;

						if (table != null && table.isEditing()) {
							try {
								wait();
							} catch (InterruptedException e) {}
						}

						instance.updateAllCampaignTables();
					}
				}

			};

			instance.updateTask.run();

			Timer timer = new Timer(true);
			LocalDateTime now = LocalDateTime.now();
			long firstRunDelay = now.until(now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES), ChronoUnit.MILLIS);
			timer.scheduleAtFixedRate(instance.updateTask, firstRunDelay, Duration.of(1, ChronoUnit.MINUTES).toMillis());
		}

		return instance;
	}

	private JFrame frame;

	private LoginPane loginPane;

	private JTable currentEditableCampaignTable;

	private boolean isException;

	private TimerTask updateTask;

	private Window() {
		frame = new JFrame(TITLE);

		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		frame.setMinimumSize(new Dimension(600, 300));
		frame.setSize(new Dimension(1000, 500));

		{
			GraphicsDevice firstGraphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];

			Rectangle bounds = firstGraphicsDevice.getDefaultConfiguration().getBounds();

			frame.setLocation(
				bounds.x+(bounds.width-frame.getWidth())/2,
				bounds.y+(bounds.height-frame.getHeight())/2
			);
		}

		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));

		loginPane = new LoginPane(this);

		frame.add(loginPane.get());

		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				VMS.getInstance().stop();

				System.exit(0);
			}

		});

		frame.setVisible(true);
	}

	public JFrame get() {
		return frame;
	}

	public LoginPane getLoginPane() {
		return loginPane;
	}

	private Map<User, AdminPane> adminPaneMap = new HashMap<>();

	public Map<User, AdminPane> getAdminPaneMap() {
		return Collections.unmodifiableMap(adminPaneMap);
	}

	public AdminPane getAdminPane(User user) throws VMSArgumentException {
		if (user.getType() != UserType.ADMIN) {
			throw new VMSArgumentException("Invalid user type");
		}

		AdminPane pane = adminPaneMap.get(user);

		if (pane == null) {
			pane = new AdminPane(this);

			adminPaneMap.put(user, pane);
		} else {
			pane.get().setSelectedIndex(0);
		}

		currentEditableCampaignTable = pane.getCampaignTable();

		frame.add(pane.get());

		return pane;
	}

	private Map<User, GuestPane> guestPaneMap = new HashMap<>();

	public Map<User, GuestPane> getGuestPaneMap() {
		return Collections.unmodifiableMap(guestPaneMap);
	}

	public GuestPane getGuestPane(User user) {
		GuestPane pane = guestPaneMap.get(user);

		if (pane == null) {
			pane = new GuestPane(this);

			guestPaneMap.put(user, pane);
		} else {
			pane.get().setSelectedIndex(0);
		}

		currentEditableCampaignTable = null;

		frame.add(pane.get());

		return pane;
	}

	public void updateAllCampaignTables() {
		adminPaneMap.values().stream()
			.map(AdminPane::getCampaignTableModel)
			.forEach(AbstractTableModel::fireTableDataChanged);

		guestPaneMap.values().stream()
			.map(GuestPane::getCampaignTableModel)
			.forEach(AbstractTableModel::fireTableDataChanged);
	}

	public void updateAllVoucherTables() {
		adminPaneMap.values().stream()
		.flatMap(p -> p.getVoucherPanelMap().values().stream())
		.map(Map.Entry::getValue)
		.forEach(AbstractTableModel::fireTableDataChanged);

		guestPaneMap.values().stream()
			.flatMap(p -> p.getVoucherPanelMap().values().stream())
			.map(Map.Entry::getValue)
			.forEach(AbstractTableModel::fireTableDataChanged);
	}

	public void updateAllObserverTables() {
		adminPaneMap.values().stream()
			.flatMap(p -> p.getObserverPanelMap().values().stream())
			.map(Map.Entry::getValue)
			.forEach(AbstractTableModel::fireTableDataChanged);
	}

	public void updateAllNotificationTables() {
		guestPaneMap.values().stream()
			.map(GuestPane::getNotificationTableModel)
			.forEach(AbstractTableModel::fireTableDataChanged);
	}

	public void clearPaneMaps() {
		adminPaneMap.clear();
		guestPaneMap.clear();
	}

	public int confirmation(String message) {
		return JOptionPane.showConfirmDialog(frame, message, Window.CONFIRMATION_DIALOG_TITLE, JOptionPane.YES_NO_OPTION);
	}

	public boolean isException() {
		return isException;
	}

	public TimerTask getUpdateTask() {
		return updateTask;
	}

	public void exception(String message) {
		isException = true;

		JOptionPane.showMessageDialog(frame, message, EXCEPTION_DIALOG_TITLE, JOptionPane.ERROR_MESSAGE);

		isException = false;
	}

	public static void main(String[] args) {
		if (LaunchDialogs.showLastTestConfirmDialog()) {
			LaunchDialogs.showLastTestDialog();
		}

		while (LaunchDialogs.showTestConfirmDialog()) {
			LaunchDialogs.showTestDialog();
		}

		getInstance(); // creeaza fereastra
	}

}