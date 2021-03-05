package vms.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.time.LocalDateTime;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.github.lgooddatepicker.tableeditors.DateTimeTableEditor;

import vms.VMS;

import vms.campaign.Campaign;
import vms.campaign.CampaignStatusType;

import vms.notification.Notification;
import vms.notification.NotificationType;

import vms.user.User;

import vms.voucher.GiftVoucher;
import vms.voucher.LoyaltyVoucher;
import vms.voucher.Voucher;
import vms.voucher.VoucherStatusType;

public final class GuestPane {

	private Window window;

	private JTabbedPane pane;

	public GuestPane(Window window) {
		this.window = window;

		pane = new JTabbedPane();

		createCampaignsPanel();

		createNotificationsPanel();

		createAccountPanel();
	}

	@SuppressWarnings("serial")
	private static final class CampaignTableModel extends AbstractTableModel {

		private static enum Column {

			ID(0, Integer.class, "Id", 50),
			NAME(1, String.class, "Name", 100),
			DESCRIPTION(2, String.class, "Description", 100),
			START_DATE_TIME(3, LocalDateTime.class, "Start", 200),
			END_DATE_TIME(4, LocalDateTime.class, "End", 200),
			STATUS(5, CampaignStatusType.class, "Status", 70);

			private final int index;
			private final Class<?> cls;
			private final String string;
			private final int width;

			private Column(int index, Class<?> cls, String string, int width) {
				this.index = index;
				this.cls = cls;
				this.string = string;
				this.width = width;
			}

			private static Column fromIndex(int index) {
				return Arrays.stream(values())
					.filter(c -> c.index == index)
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException(String.format("No enum with index '%d'", index)));
			}

		}

		private static final String[] COLUMN_NAMES = {
			Column.ID.string,
			Column.NAME.string,
			Column.DESCRIPTION.string,
			Column.START_DATE_TIME.string,
			Column.END_DATE_TIME.string,
			Column.STATUS.string,
		};

		private static final Class<?>[] COLUMN_CLASSES = {
			Column.ID.cls,
			Column.NAME.cls,
			Column.DESCRIPTION.cls,
			Column.START_DATE_TIME.cls,
			Column.END_DATE_TIME.cls,
			Column.STATUS.cls,
		};

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex < 0 || columnIndex >= COLUMN_NAMES.length) {
				return "";
			}

			return COLUMN_NAMES[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex < 0 || columnIndex >= COLUMN_NAMES.length) {
				return null;
			}

			return COLUMN_CLASSES[columnIndex];
		}

		@Override
		public int getRowCount() {
			return VMS.getInstance().getAuthenticatedUser().getCampaignCount();
		}

		@Override
		public int getColumnCount() {
			return COLUMN_CLASSES.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (rowIndex < 0 || rowIndex > getRowCount() || columnIndex < 0 || columnIndex > getColumnCount()) {
				return null;
			}

			Campaign campaign = VMS.getInstance().getAuthenticatedUser().getCampaigns().get(rowIndex);

			switch (Column.fromIndex(columnIndex)) {
			case ID:
				return campaign.getId();

			case NAME:
				return campaign.getName();

			case DESCRIPTION:
				return campaign.getDescription();

			case START_DATE_TIME:
				return campaign.getStartDateTime();

			case END_DATE_TIME:
				return campaign.getEndDateTime();

			case STATUS:
				return campaign.getStatus();

			default:
				return null;
			}
		}

	}

	private AbstractTableModel campaignTableModel;

	public AbstractTableModel getCampaignTableModel() {
		return campaignTableModel;
	}

	@SuppressWarnings("serial")
	private final class CampaignTableIdRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column
		) {
			JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			row = table.convertRowIndexToModel(row);

			label.setBackground(row == selectedCampaignId ? table.getSelectionBackground() : table.getBackground());

			return label;
		}

	}

	private int selectedCampaignId = -1;

	public JPanel createCampaignsPanel() {
		JPanel panel = new JPanel();

		pane.addTab("Campaigns", panel);

		pane.addTab("Vouchers", null);
		pane.setEnabledAt(1, false);

		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		JTable table = new JTable();

		table.setCellSelectionEnabled(false);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);

		AbstractTableModel tableModel = campaignTableModel = new CampaignTableModel();
		table.setModel(tableModel);

		table.getRowSorter().toggleSortOrder(CampaignTableModel.Column.ID.index);

		table.getColumnModel().getColumn(CampaignTableModel.Column.ID.index).setCellRenderer(new CampaignTableIdRenderer());

		table.setDefaultEditor(LocalDateTime.class, new DateTimeTableEditor());
		table.setDefaultRenderer(LocalDateTime.class, new DateTimeTableEditor());

		Arrays.stream(CampaignTableModel.Column.values())
			.map(c -> c.cls)
			.distinct()
			.forEach(cls -> table.setDefaultRenderer(cls, new NoBorderTableCellRenderer(table.getDefaultRenderer(cls))));

		Arrays.stream(CampaignTableModel.Column.values())
			.map(c -> table.getColumnModel().getColumn(c.index))
			.forEach(c -> {
				TableCellRenderer renderer = c.getCellRenderer();

				if (renderer != null) {
					c.setCellRenderer(new NoBorderTableCellRenderer(renderer));
				}
			});

		Arrays.stream(CampaignTableModel.Column.values())
			.forEach(c -> table.getColumnModel().getColumn(c.index).setPreferredWidth(c.width));

		table.addMouseListener(new MouseAdapter() {

			private int oldRow = -1;

			@Override
			public void mouseReleased(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				// int column = table.columnAtPoint(e.getPoint());

				if (row == oldRow) {
					return;
				}

				oldRow = row;

				if (row == -1) {
					pane.setEnabledAt(1, false);
					pane.setComponentAt(1, null);
				} else {
					row = table.convertRowIndexToModel(row);
				}

				selectedCampaignId = row;
				table.repaint();

				if (row == -1) {
					return;
				}

				Campaign campaign = VMS.getInstance().getAuthenticatedUser().getCampaigns().get(row);

				pane.setComponentAt(1, getVouchersPanel(campaign));
				pane.setEnabledAt(1, true);
			}

		});

		JScrollPane tablePane = new JScrollPane(table);

		panel.add(tablePane);

		return panel;
	}

	@SuppressWarnings("serial")
	private static final class VoucherTableModel extends AbstractTableModel {

		private static enum Column {

			CODE(0, String.class, "Code", 200),
			STATUS(1, VoucherStatusType.class, "Status", 70),
			REDEMPTION_DATE_TIME(2, LocalDateTime.class, "Redemption", 200),
			TYPE(3, String.class, "Type", 100),
			VALUE(4, Float.class, "Value", 50);

			private final int index;
			private final Class<?> cls;
			private final String string;
			private final int width;

			private Column(int index, Class<?> cls, String string, int width) {
				this.index = index;
				this.cls = cls;
				this.string = string;
				this.width = width;
			}

			private static Column fromIndex(int index) {
				return Arrays.stream(values())
					.filter(c -> c.index == index)
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException(String.format("No enum with index '%d'", index)));
			}

		}

		private static final String[] COLUMN_NAMES = {
			Column.CODE.string,
			Column.STATUS.string,
			Column.REDEMPTION_DATE_TIME.string,
			Column.TYPE.string,
			Column.VALUE.string,
		};

		private static final Class<?>[] COLUMN_CLASSES = {
			Column.CODE.cls,
			Column.STATUS.cls,
			Column.REDEMPTION_DATE_TIME.cls,
			Column.TYPE.cls,
			Column.VALUE.cls
		};

		private Campaign campaign;

		public VoucherTableModel(Campaign campaign) {
			this.campaign = campaign;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex < 0 || columnIndex >= COLUMN_NAMES.length) {
				return "";
			}

			return COLUMN_NAMES[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex < 0 || columnIndex >= COLUMN_NAMES.length) {
				return null;
			}

			return COLUMN_CLASSES[columnIndex];
		}

		@Override
		public int getRowCount() {
			return VMS.getInstance().getAuthenticatedUser().getCampaignVoucherCount(campaign.getId());
		}

		@Override
		public int getColumnCount() {
			return COLUMN_CLASSES.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (rowIndex < 0 || rowIndex > getRowCount() || columnIndex < 0 || columnIndex > getColumnCount()) {
				return null;
			}

			Voucher voucher = VMS.getInstance().getAuthenticatedUser().getCampaignVouchers(campaign.getId()).get(rowIndex);

			switch (Column.fromIndex(columnIndex)) {
			case CODE:
				return voucher.getCode();

			case STATUS:
				return voucher.getStatus();

			case REDEMPTION_DATE_TIME:
				return voucher.getRedemptionDateTime();

			case TYPE:
				if (voucher instanceof GiftVoucher) {
					return "GiftVoucher";
				} else if (voucher instanceof LoyaltyVoucher) {
					return "LoyaltyVoucher";
				}

			case VALUE:
				if (voucher instanceof GiftVoucher) {
					return ((GiftVoucher)voucher).getAmount();
				} else if (voucher instanceof LoyaltyVoucher) {
					return ((LoyaltyVoucher)voucher).getDiscount();
				}

			default:
				return null;
			}
		}

	}

	private Map<Campaign, Map.Entry<JPanel, AbstractTableModel>> voucherPanelMap = new HashMap<>();

	public Map<Campaign, Map.Entry<JPanel, AbstractTableModel>> getVoucherPanelMap() {
		return Collections.unmodifiableMap(voucherPanelMap);
	}

	public JPanel getVouchersPanel(Campaign campaign) {
		Map.Entry<JPanel, AbstractTableModel> panelEntry = voucherPanelMap.get(campaign);

		JPanel panel;

		if (panelEntry == null) {
			panel = new JPanel();

			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

			JTable table = new JTable();

			table.setCellSelectionEnabled(false);
			table.setAutoCreateRowSorter(true);
			table.setFillsViewportHeight(true);

			AbstractTableModel tableModel = new VoucherTableModel(campaign);
			table.setModel(tableModel);

			table.getRowSorter().toggleSortOrder(VoucherTableModel.Column.TYPE.index);

			table.setDefaultEditor(LocalDateTime.class, new DateTimeTableEditor());
			table.setDefaultRenderer(LocalDateTime.class, new DateTimeTableEditor());

			Arrays.stream(VoucherTableModel.Column.values())
				.map(c -> c.cls)
				.distinct()
				.forEach(cls -> table.setDefaultRenderer(cls, new NoBorderTableCellRenderer(table.getDefaultRenderer(cls))));

			Arrays.stream(VoucherTableModel.Column.values())
				.map(c -> table.getColumnModel().getColumn(c.index))
				.forEach(c -> {
					TableCellRenderer renderer = c.getCellRenderer();

					if (renderer != null) {
						c.setCellRenderer(new NoBorderTableCellRenderer(renderer));
					}
				});

			Arrays.stream(VoucherTableModel.Column.values())
				.forEach(c -> table.getColumnModel().getColumn(c.index).setPreferredWidth(c.width));

			JScrollPane tablePane = new JScrollPane(table);

			panel.add(tablePane);

			voucherPanelMap.put(campaign, new AbstractMap.SimpleImmutableEntry<>(panel, tableModel));
		} else {
			panel = panelEntry.getKey();
		}

		return panel;
	}

	@SuppressWarnings("serial")
	private static final class NotificationTableModel extends AbstractTableModel {

		private static enum Column {

			TYPE(0, NotificationType.class, "Type", 100),
			DATE_TIME(1, LocalDateTime.class, "Date", 200),
			CAMPAIGN_ID(2, Integer.class, "Campaign id", 50);


			private final int index;
			private final Class<?> cls;
			private final String string;
			private final int width;

			private Column(int index, Class<?> cls, String string, int width) {
				this.index = index;
				this.cls = cls;
				this.string = string;
				this.width = width;
			}

			private static Column fromIndex(int index) {
				return Arrays.stream(values())
					.filter(c -> c.index == index)
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException(String.format("No enum with index '%d'", index)));
			}

		}

		private static final String[] COLUMN_NAMES = {
			Column.TYPE.string,
			Column.DATE_TIME.string,
			Column.CAMPAIGN_ID.string,
		};

		private static final Class<?>[] COLUMN_CLASSES = {
			Column.TYPE.cls,
			Column.DATE_TIME.cls,
			Column.CAMPAIGN_ID.cls,
		};

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex < 0 || columnIndex >= COLUMN_NAMES.length) {
				return "";
			}

			return COLUMN_NAMES[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex < 0 || columnIndex >= COLUMN_NAMES.length) {
				return null;
			}

			return COLUMN_CLASSES[columnIndex];
		}

		@Override
		public int getRowCount() {
			return VMS.getInstance().getAuthenticatedUser().getNotificationCount();
		}

		@Override
		public int getColumnCount() {
			return COLUMN_CLASSES.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (rowIndex < 0 || rowIndex > getRowCount() || columnIndex < 0 || columnIndex > getColumnCount()) {
				return null;
			}

			Notification notification = VMS.getInstance().getAuthenticatedUser().getNotifications().get(rowIndex);

			switch (Column.fromIndex(columnIndex)) {
			case TYPE:
				return notification.getType();

			case DATE_TIME:
				return notification.getDateTime();

			case CAMPAIGN_ID:
				return notification.getCampaignId();

			default:
				return null;
			}
		}

	}

	private AbstractTableModel notificationTableModel;

	public AbstractTableModel getNotificationTableModel() {
		return notificationTableModel;
	}

	public JPanel createNotificationsPanel() {
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		JTable table = new JTable();

		table.setCellSelectionEnabled(false);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);

		AbstractTableModel tableModel = notificationTableModel = new NotificationTableModel();
		table.setModel(tableModel);

		table.getRowSorter().toggleSortOrder(NotificationTableModel.Column.DATE_TIME.index);

		table.setDefaultEditor(LocalDateTime.class, new DateTimeTableEditor());
		table.setDefaultRenderer(LocalDateTime.class, new DateTimeTableEditor());

		Arrays.stream(NotificationTableModel.Column.values())
			.map(c -> c.cls)
			.distinct()
			.forEach(cls -> table.setDefaultRenderer(cls, new NoBorderTableCellRenderer(table.getDefaultRenderer(cls))));

		Arrays.stream(NotificationTableModel.Column.values())
			.map(c -> table.getColumnModel().getColumn(c.index))
			.forEach(c -> {
				TableCellRenderer renderer = c.getCellRenderer();

				if (renderer != null) {
					c.setCellRenderer(new NoBorderTableCellRenderer(renderer));
				}
			});

		Arrays.stream(NotificationTableModel.Column.values())
			.forEach(c -> table.getColumnModel().getColumn(c.index).setPreferredWidth(c.width));

		JScrollPane tablePane = new JScrollPane(table);

		panel.add(tablePane);

		pane.addTab("Notifications", panel);

		return panel;
	}

	public JPanel createAccountPanel() {
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		User authenticatedUser = VMS.getInstance().getAuthenticatedUser();

		JLabel nameLabel = new JLabel("Name");
		panel.add(nameLabel);

		JTextField nameField = new JTextField(authenticatedUser.getName());
		nameField.setEditable(false);
		panel.add(nameField);

		JLabel emailLabel = new JLabel("Email");
		panel.add(emailLabel);

		JTextField emailField = new JTextField(authenticatedUser.getEmail());
		emailField.setEditable(false);
		panel.add(emailField);

		JLabel typeLabel = new JLabel("Type");
		panel.add(typeLabel);

		JTextField typeField = new JTextField(authenticatedUser.getType().toString());
		typeField.setEditable(false);
		panel.add(typeField);

		JButton logoutButton = new JButton("Log out");

		logoutButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				hide();

				window.getLoginPane().show();
			}

		});

		panel.add(logoutButton);

		pane.add(panel, "Account");

		return panel;
	}

	public JTabbedPane get() {
		return pane;
	}

	public void hide() {
		pane.setVisible(false);
	}

	public void show() {
		pane.setVisible(true);
	}

}