package vms.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;

import java.time.LocalDateTime;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultFormatter;

import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.tableeditors.DateTimeTableEditor;

import util.Util;

import vms.VMS;
import vms.VMS.AccessController.Event;

import vms.campaign.Campaign;
import vms.campaign.CampaignStatusType;
import vms.campaign.CampaignStrategyType;

import vms.exception.VMSArgumentException;
import vms.exception.VMSException;

import vms.user.User;
import vms.user.UserType;

import vms.voucher.GiftVoucher;
import vms.voucher.LoyaltyVoucher;
import vms.voucher.Voucher;
import vms.voucher.VoucherStatusType;

public final class AdminPane {

	private Window window;

	private JTabbedPane pane;

	public AdminPane(Window window) {
		this.window = window;

		pane = new JTabbedPane();

		createCampaignsPanel();

		createAccountPanel();
	}

	@SuppressWarnings("serial")
	private static final class CampaignTableModel extends AbstractTableModel {

		private static enum Column {

			ID(0, Integer.class, "Id", 50, new CampaignStatusType[] {}),
			NAME(1, String.class, "Name", 100, new CampaignStatusType[] {CampaignStatusType.NEW}),
			DESCRIPTION(2, String.class, "Description", 100, new CampaignStatusType[] {CampaignStatusType.NEW}),
			START_DATE_TIME(3, LocalDateTime.class, "Start", 200, new CampaignStatusType[] {CampaignStatusType.NEW}),
			END_DATE_TIME(4, LocalDateTime.class, "End", 200, new CampaignStatusType[] {CampaignStatusType.NEW, CampaignStatusType.STARTED}),
			TOTAL_VOUCHER_COUNT(5, Integer.class, "Budget", 50, new CampaignStatusType[] {CampaignStatusType.NEW, CampaignStatusType.STARTED}),
			AVAILABLE_VOUCHER_COUNT(6, Integer.class, "Remaining", 50, new CampaignStatusType[] {}),
			STATUS(7, CampaignStatusType.class, "Status", 70, new CampaignStatusType[] {}),
			STRATEGY(8, CampaignStrategyType.class, "Strategy", 50, new CampaignStatusType[] {CampaignStatusType.NEW});

			private final int index;
			private final Class<?> cls;
			private final String string;
			private final int width;
			private final CampaignStatusType[] isEditable;

			private Column(int index, Class<?> cls, String string, int width, CampaignStatusType[] isEditable) {
				this.index = index;
				this.cls = cls;
				this.string = string;
				this.width = width;
				this.isEditable = isEditable;
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
			Column.TOTAL_VOUCHER_COUNT.string,
			Column.AVAILABLE_VOUCHER_COUNT.string,
			Column.STATUS.string,
			Column.STRATEGY.string,
		};

		private static final Class<?>[] COLUMN_CLASSES = {
			Column.ID.cls,
			Column.NAME.cls,
			Column.DESCRIPTION.cls,
			Column.START_DATE_TIME.cls,
			Column.END_DATE_TIME.cls,
			Column.TOTAL_VOUCHER_COUNT.cls,
			Column.AVAILABLE_VOUCHER_COUNT.cls,
			Column.STATUS.cls,
			Column.STRATEGY.cls,
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
			return VMS.getInstance().getCampaignCount();
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

			Campaign campaign = VMS.getInstance().getCampaigns().get(rowIndex);

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

			case TOTAL_VOUCHER_COUNT:
				return campaign.getTotalVoucherCount();

			case AVAILABLE_VOUCHER_COUNT:
				return campaign.getAvailableVoucherCount();

			case STATUS:
				return campaign.getStatus();

			case STRATEGY:
				return campaign.getStrategy();

			default:
				return null;
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (rowIndex < 0 || rowIndex > getRowCount() || columnIndex < 0 || columnIndex > getColumnCount()) {
				return false;
			}

			CampaignStatusType status = VMS.getInstance().getCampaigns().get(rowIndex).getStatus();

			return Arrays.asList(Column.fromIndex(columnIndex).isEditable).contains(status);
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			Campaign campaign = VMS.getInstance().getCampaigns().get(rowIndex);

			Campaign ref = new Campaign(campaign);

			try {
				switch (Column.fromIndex(columnIndex)) {
				case NAME:
					String name = (String)value;

					if (name.compareTo(campaign.getName()) == 0) {
						return;
					}

					ref.setName(name);

					break;

				case DESCRIPTION:
					String description = (String)value;

					if (description.compareTo(campaign.getDescription()) == 0) {
						return;
					}

					ref.setDescription(description);

					break;

				case START_DATE_TIME:
					LocalDateTime startDateTime = (LocalDateTime)value;

					if (startDateTime.compareTo(campaign.getStartDateTime()) == 0) {
						return;
					}

					ref.setStartDateTime(startDateTime);

					break;

				case END_DATE_TIME:
					LocalDateTime endDateTime = (LocalDateTime)value;

					if (endDateTime.compareTo(campaign.getEndDateTime()) == 0) {
						return;
					}

					ref.setEndDateTime(endDateTime);

					break;

				case TOTAL_VOUCHER_COUNT:
					int totalVoucherCount = (int)value;

					if (totalVoucherCount == campaign.getTotalVoucherCount()) {
						return;
					}

					ref.setTotalVoucherCount(totalVoucherCount);

					break;

				case STRATEGY:
					CampaignStrategyType strategy = (CampaignStrategyType)value;

					if (strategy == campaign.getStrategy()) {
						return;
					}

					ref.setStrategy(strategy);

					break;

				default:
				}
			} catch (VMSException e) {
				if (!Window.getInstance().isException()) {
					Window.getInstance().exception(Util.getExceptionRootCause(e).getMessage());
				}
			}

			try {
				VMS.getInstance().getAccessController().handle(new Event(
					Event.Type.EDIT_CAMPAIGN,
					VMS.getInstance().getAuthenticatedUser(),
					Arrays.asList(ref).iterator()
				));

				Window.getInstance().updateAllCampaignTables();

				Window.getInstance().updateAllNotificationTables();
			} catch (VMSException e) {
				Window.getInstance().exception(Util.getExceptionRootCause(e).getMessage());
			}
		}

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

	@SuppressWarnings("serial")
	private static final class CampaignTableTotalVoucherCountRenderer extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

		private SpinnerModel model;
		private JSpinner spinner;

		public CampaignTableTotalVoucherCountRenderer() {
			model = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1);
			spinner = new JSpinner(model);

			JFormattedTextField field = (JFormattedTextField)spinner.getEditor().getComponent(0);
			DefaultFormatter formatter = (DefaultFormatter)field.getFormatter();
			formatter.setCommitsOnValidEdit(true);

			spinner.remove(spinner.getComponent(1)); // javax.swing.plaf.basic.BasicArrowButton[Spinner.previousButton]
		}

		@Override
		public Object getCellEditorValue() {
			return model.getValue();
		}

		@Override
		public Component getTableCellEditorComponent(
			JTable table, Object value, boolean isSelected,
			int row, int column
		) {
			model.setValue(value);

			return spinner;
		}

		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column
		) {
			model.setValue(value);

			return spinner;
		}

	}

	@SuppressWarnings("serial")
	private static final class CampaignTableStrategyRenderer extends JComboBox<CampaignStrategyType> implements TableCellRenderer {

		public CampaignTableStrategyRenderer(CampaignStrategyType[] strategies) {
			super(strategies);
		}

		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column
		) {
			if (isSelected) {
				setBackground(table.getSelectionBackground());
			} else {
				setBackground(table.getBackground());
			}

			setSelectedItem(value);

			return this;
		}

	}

	@SuppressWarnings("serial")
	private static final class CampaignTableStrategyEditor extends DefaultCellEditor {

		public CampaignTableStrategyEditor(CampaignStrategyType[] strategies) {
			super(new JComboBox<>(strategies));
		}

	}

	private JTable campaignTable;

	public JTable getCampaignTable() {
		return campaignTable;
	}

	private AbstractTableModel campaignTableModel;

	public AbstractTableModel getCampaignTableModel() {
		return campaignTableModel;
	}

	private int selectedCampaignId = -1;

	public JPanel createCampaignsPanel() {
		JPanel panel = new JPanel();

		pane.addTab("Campaigns", panel);

		pane.addTab("Vouchers", null);
		pane.setEnabledAt(1, false);

		pane.addTab("Observers", null);
		pane.setEnabledAt(2, false);

		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		JTable table = campaignTable = new JTable();

		table.setCellSelectionEnabled(false);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);

		AbstractTableModel tableModel = campaignTableModel = new CampaignTableModel();
		table.setModel(tableModel);

		table.getRowSorter().toggleSortOrder(CampaignTableModel.Column.ID.index);

		table.getColumnModel().getColumn(CampaignTableModel.Column.ID.index).setCellRenderer(new CampaignTableIdRenderer());

		table.getColumnModel().getColumn(CampaignTableModel.Column.TOTAL_VOUCHER_COUNT.index).setCellEditor(new CampaignTableTotalVoucherCountRenderer());
		table.getColumnModel().getColumn(CampaignTableModel.Column.TOTAL_VOUCHER_COUNT.index).setCellRenderer(new CampaignTableTotalVoucherCountRenderer());

		table.setDefaultEditor(LocalDateTime.class, new DateTimeTableEditor());
		table.setDefaultRenderer(LocalDateTime.class, new DateTimeTableEditor());

		table.setDefaultEditor(CampaignStrategyType.class, new CampaignTableStrategyEditor(CampaignStrategyType.values()));
		table.setDefaultRenderer(CampaignStrategyType.class, new CampaignTableStrategyRenderer(CampaignStrategyType.values()));

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

		table.addPropertyChangeListener("tableCellEditor", new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if (!table.isEditing()) {
					synchronized (window.getUpdateTask()) {
						window.getUpdateTask().notify();
					}
				}
			}

		});

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

					pane.setEnabledAt(2, false);
					pane.setComponentAt(2, null);
				} else {
					row = table.convertRowIndexToModel(row);
				}

				selectedCampaignId = row;
				table.repaint();

				if (row == -1) {
					return;
				}

				Campaign campaign = VMS.getInstance().getCampaigns().get(row);

				pane.setComponentAt(1, getVouchersPanel(campaign));
				pane.setEnabledAt(1, true);

				pane.setComponentAt(2, getObserversPanel(campaign));
				pane.setEnabledAt(2, true);
			}

		});

		table.addMouseListener(new MouseAdapter() {

			private void popUp(MouseEvent e) {
				JPopupMenu menu = new JPopupMenu();

				int row = table.rowAtPoint(e.getPoint());

				if (row != -1) {
					row = table.convertRowIndexToModel(row);

					Campaign campaign = VMS.getInstance().getCampaigns().get(row);

					switch (campaign.getStatus()) {
					case NEW:
					case STARTED:
						JMenuItem cancelButton = new JMenuItem("Cancel");

						cancelButton.addActionListener(new ActionListener() {

							@Override
							public void actionPerformed(ActionEvent e) {
								try {
									VMS.getInstance().getAccessController().handle(new Event(
										Event.Type.CANCEL_CAMPAIGN,
										VMS.getInstance().getAuthenticatedUser(),
										Arrays.asList(campaign.getId()).iterator()
									));

									window.updateAllCampaignTables();

									window.updateAllNotificationTables();
								} catch (VMSException ex) {
									window.exception(Util.getExceptionRootCause(ex).getMessage());
								}
							}
						});

						menu.add(cancelButton);

					default:
					}
				}

				menu.addSeparator();

				JMenuItem newButton = new JMenuItem("New");

				newButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							JTextField nameField = new JTextField("New Campaign");

							JTextField descriptionField = new JTextField();

							DateTimePicker startDateTimePicker = new DateTimePicker();

							startDateTimePicker.setDateTimeStrict(VMS.getInstance().getDateTime());

							DateTimePicker endDateTimePicker = new DateTimePicker();

							endDateTimePicker.setDateTimeStrict(VMS.getInstance().getDateTime().plusDays(1));

							JSpinner totalVoucherCountSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));

							JComboBox<CampaignStrategyType> strategyComboBox = new JComboBox<>(CampaignStrategyType.values());

							int option = JOptionPane.showConfirmDialog(
								window.get(),

								new Object[] {
									"Name", nameField,
									"Description", descriptionField,
									"Start", startDateTimePicker,
									"End", endDateTimePicker,
									"Budget", totalVoucherCountSpinner,
									"Strategy", strategyComboBox,
								},

								Window.NEW_DIALOG_TITLE,
								JOptionPane.OK_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE
							);

							if (option == JOptionPane.OK_OPTION) {
								if (nameField.getText().trim().length() == 0) {
									throw new VMSArgumentException("Campaign name cannot be empty");
								}

								VMS.getInstance().getAccessController().handle(new Event(
									Event.Type.ADD_CAMPAIGN,

									VMS.getInstance().getAuthenticatedUser(),

									Arrays.asList(new Campaign(
										VMS.getInstance().getCampaignCount()+1,
										nameField.getText(),
										descriptionField.getText(),
										startDateTimePicker.getDateTimeStrict(),
										endDateTimePicker.getDateTimeStrict(),
										(int)totalVoucherCountSpinner.getValue(),
										(CampaignStrategyType)strategyComboBox.getSelectedItem()
									)).iterator()
								));

								window.updateAllCampaignTables();
							}
						} catch (VMSException ex) {
							window.exception(Util.getExceptionRootCause(ex).getMessage());
						}
					}

				});

				menu.add(newButton);

				JMenuItem generateVouchersButton = new JMenuItem("Generate vouchers");

				generateVouchersButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						JFileChooser fileChooser = new JFileChooser(".");
						fileChooser.setDialogTitle("Choose emails file");
						fileChooser.setAcceptAllFileFilterUsed(false);
						fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "text"));

						int option = fileChooser.showOpenDialog(window.get());

						if (option != JFileChooser.APPROVE_OPTION) {
							return;
						}

						File file = fileChooser.getSelectedFile();

						try {
							VMS.getInstance().getReader().setEmailsFile(file);
						} catch (VMSException ex) {
							window.exception(Util.joinExceptionCauseChain(ex));
							return;
						}

						List<List<Object>> voucherArgList;

						try {
							voucherArgList = VMS.getInstance().getReader().readEmails();
						} catch (VMSException ex) {
							window.exception(Util.joinExceptionCauseChain(ex));
							return;
						}

						Object[][] reportTableData = new Object[voucherArgList.size()][];

						int i = 0;

						for (List<Object> argList : voucherArgList) {
							Object[] reportTableRow = new Object[2];

							reportTableRow[0] = argList;

							try {
								VMS.getInstance().getAccessController().handle(new Event(
									Event.Type.GENERATE_VOUCHER,
									VMS.getInstance().getAuthenticatedUser(),
									argList.iterator()
								));

								reportTableRow[1] = "Generated successfully";
							} catch (VMSException ex) {
								reportTableRow[1] = Util.getExceptionRootCause(ex).getMessage();
							}

							reportTableData[i++] = reportTableRow;
						}

						window.updateAllCampaignTables();

						window.updateAllVoucherTables();

						window.updateAllObserverTables();

						JTable reportTable = new JTable(reportTableData, new Object[] {"Arguments", "Result"});
						reportTable.setEnabled(false);
						Util.resizeTableColumnWidth(reportTable);

						JOptionPane.showMessageDialog(
							window.get(),

							new Object[] {
								"Finished processing arguments",
								new JScrollPane(reportTable),
							},

							Window.INFORMATION_DIALOG_TITLE,
							JOptionPane.INFORMATION_MESSAGE
						);
					}
				});

				menu.add(generateVouchersButton);

				menu.show(e.getComponent(), e.getX(), e.getY());
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popUp(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popUp(e);
				}
			}

		});

		JScrollPane tablePane = new JScrollPane(table);

		panel.add(tablePane);

		return panel;
	}

	@SuppressWarnings("serial")
	private static final class VoucherTableModel extends AbstractTableModel {

		private static enum Column {

			ID(0, Integer.class, "Id", 50),
			CODE(1, String.class, "Code", 200),
			STATUS(2, VoucherStatusType.class, "Status", 70),
			REDEMPTION_DATE_TIME(3, LocalDateTime.class, "Redemption", 200),
			EMAIL(4, String.class, "Email", 100),
			TYPE(5, String.class, "Type", 100),
			VALUE(6, Float.class, "Value", 50);

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
			Column.CODE.string,
			Column.STATUS.string,
			Column.REDEMPTION_DATE_TIME.string,
			Column.EMAIL.string,
			Column.TYPE.string,
			Column.VALUE.string,
		};

		private static final Class<?>[] COLUMN_CLASSES = {
			Column.ID.cls,
			Column.CODE.cls,
			Column.STATUS.cls,
			Column.REDEMPTION_DATE_TIME.cls,
			Column.EMAIL.cls,
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
			return campaign.getTotalVoucherCount()-campaign.getAvailableVoucherCount();
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

			Voucher voucher = campaign.getVouchers().get(rowIndex);

			switch (Column.fromIndex(columnIndex)) {
			case ID:
				return voucher.getId();

			case CODE:
				return voucher.getCode();

			case STATUS:
				return voucher.getStatus();

			case REDEMPTION_DATE_TIME:
				return voucher.getRedemptionDateTime();

			case EMAIL:
				return voucher.getEmail();

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

			AbstractTableModel tableModel = new VoucherTableModel(campaign);

			TableRowSorter<AbstractTableModel> tableSorter = new TableRowSorter<>(tableModel);
			table.setRowSorter(tableSorter);

			table.setFillsViewportHeight(true);

			table.setModel(tableModel);

			table.getRowSorter().toggleSortOrder(VoucherTableModel.Column.ID.index);

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

			table.addMouseListener(new MouseAdapter() {

				private void popUp(MouseEvent e) {
					JPopupMenu menu = new JPopupMenu();

					int row = table.rowAtPoint(e.getPoint());

					if (row != -1) {
						row = table.convertRowIndexToModel(row);

						Voucher voucher = campaign.getVouchers().get(row);

						if (campaign.getStatus() == CampaignStatusType.STARTED && voucher.getStatus() == VoucherStatusType.UNUSED) {
							JMenuItem markUsedButton = new JMenuItem("Mark used");

							markUsedButton.addActionListener(new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent e) {
									try {
										VMS.getInstance().getAccessController().handle(new Event(
											Event.Type.REDEEM_VOUCHER,

											VMS.getInstance().getAuthenticatedUser(),

											Arrays.asList(
												campaign.getId(),
												voucher.getId(),
												VMS.getInstance().getDateTime()
											).iterator()
										));

										window.updateAllVoucherTables();
									} catch (VMSException ex) {
										window.exception(Util.getExceptionRootCause(ex).getMessage());
									}
								}

							});

							menu.add(markUsedButton);
						}
					}

					menu.addSeparator();

					switch (campaign.getStatus()) {
					case NEW:
					case STARTED:
						if (campaign.getAvailableVoucherCount() <= 0) {
							break;
						}

						JMenuItem newButton = new JMenuItem("New");

						newButton.addActionListener(new ActionListener() {

							@Override
							public void actionPerformed(ActionEvent e) {
								try {
									JComboBox<String> emailComboBox = new JComboBox<>(
										VMS.getInstance().getUsers().stream().map(User::getEmail).toArray(String[]::new)
									);

									JComboBox<String> typeComboBox = new JComboBox<>(new String[] {"GiftVoucher", "LoyaltyVoucher"});

									JSpinner valueSpinner = new JSpinner(new SpinnerNumberModel(0., 0., Float.MAX_VALUE, 1.));

									int option = JOptionPane.showConfirmDialog(
										window.get(),

										new Object[] {
											"Email", emailComboBox,
											"Type", typeComboBox,
											"Value", valueSpinner,
										},

										Window.NEW_DIALOG_TITLE,
										JOptionPane.OK_CANCEL_OPTION,
										JOptionPane.QUESTION_MESSAGE
									);

									if (option == JOptionPane.OK_OPTION) {
										VMS.getInstance().getAccessController().handle(new Event(
											Event.Type.GENERATE_VOUCHER,

											VMS.getInstance().getAuthenticatedUser(),

											Arrays.asList(
												campaign.getId(),
												(String)emailComboBox.getSelectedItem(),
												(String)typeComboBox.getSelectedItem(),
												(float)(double)valueSpinner.getValue()
											).iterator()
										));

										window.updateAllCampaignTables();

										window.updateAllVoucherTables();

										window.updateAllObserverTables();
									}
								} catch (VMSException ex) {
									window.exception(Util.getExceptionRootCause(ex).getMessage());
								}
							}

						});

						menu.add(newButton);

						JMenuItem newFromStrategyButton = new JMenuItem("New from strategy");

						newFromStrategyButton.addActionListener(new ActionListener() {

							@Override
							public void actionPerformed(ActionEvent e) {
								try {
									VMS.getInstance().getAccessController().handle(new Event(
										Event.Type.GET_VOUCHER,
										VMS.getInstance().getAuthenticatedUser(),
										Arrays.asList(campaign.getId()).iterator()
									));

									window.updateAllCampaignTables();

									window.updateAllVoucherTables();
								} catch (VMSException ex) {
									window.exception(Util.getExceptionRootCause(ex).getMessage());
								}
							}
						});

						menu.add(newFromStrategyButton);

						break;

					default:
					}

					menu.show(e.getComponent(), e.getX(), e.getY());
				}

				@Override
				public void mousePressed(MouseEvent e) {
					if (e.isPopupTrigger()) {
						popUp(e);
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger()) {
						popUp(e);
					}
				}

			});

			JScrollPane tablePane = new JScrollPane(table);

			panel.add(tablePane);

			JTextField tableCodeFilterField = new JTextField();

			tableCodeFilterField.getDocument().addDocumentListener(new DocumentListener(){

				private void updateFilter() {
					String text = tableCodeFilterField.getText();

					if (text.trim().length() == 0) {
						tableSorter.setRowFilter(null);
					} else {
						tableSorter.setRowFilter(RowFilter.regexFilter("(?i)"+Pattern.quote(text)));
					}
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					updateFilter();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					updateFilter();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					throw new UnsupportedOperationException();
				}

			});

			panel.add(tableCodeFilterField);

			voucherPanelMap.put(campaign, new AbstractMap.SimpleImmutableEntry<>(panel, tableModel));
		} else {
			panel = panelEntry.getKey();
		}

		return panel;
	}

	@SuppressWarnings("serial")
	private static final class ObserverTableModel extends AbstractTableModel {

		private static enum Column {

			ID(0, Integer.class, "Id", 50),
			NAME(1, String.class, "Name", 100),
			EMAIL(2, String.class, "Email", 100),
			TYPE(3, UserType.class, "Type", 100);

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
			Column.EMAIL.string,
			Column.TYPE.string,
		};

		private static final Class<?>[] COLUMN_CLASSES = {
			Column.ID.cls,
			Column.NAME.cls,
			Column.EMAIL.cls,
			Column.TYPE.cls,
		};

		private Campaign campaign;

		public ObserverTableModel(Campaign campaign) {
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
			return campaign.getObserverCount();
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

			User observer = campaign.getObservers().get(rowIndex);

			switch (Column.fromIndex(columnIndex)) {
			case ID:
				return observer.getId();

			case NAME:
				return observer.getName();

			case EMAIL:
				return observer.getEmail();

			case TYPE:
				return observer.getType();

			default:
				return null;
			}
		}

	}

	private Map<Campaign, Map.Entry<JPanel, AbstractTableModel>> observerPanelMap = new HashMap<>();

	public Map<Campaign, Map.Entry<JPanel, AbstractTableModel>> getObserverPanelMap() {
		return Collections.unmodifiableMap(observerPanelMap);
	}

	public JPanel getObserversPanel(Campaign campaign) {
		Map.Entry<JPanel, AbstractTableModel> panelEntry = observerPanelMap.get(campaign);

		JPanel panel;

		if (panelEntry == null) {
			panel = new JPanel();

			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

			JTable table = new JTable();

			table.setCellSelectionEnabled(false);
			table.setAutoCreateRowSorter(true);
			table.setFillsViewportHeight(true);

			AbstractTableModel tableModel = new ObserverTableModel(campaign);
			table.setModel(tableModel);

			table.getRowSorter().toggleSortOrder(ObserverTableModel.Column.ID.index);

			table.setDefaultEditor(LocalDateTime.class, new DateTimeTableEditor());
			table.setDefaultRenderer(LocalDateTime.class, new DateTimeTableEditor());

			Arrays.stream(ObserverTableModel.Column.values())
				.map(c -> c.cls)
				.distinct()
				.forEach(cls -> table.setDefaultRenderer(cls, new NoBorderTableCellRenderer(table.getDefaultRenderer(cls))));

			Arrays.stream(ObserverTableModel.Column.values())
				.map(c -> table.getColumnModel().getColumn(c.index))
				.forEach(c -> {
					TableCellRenderer renderer = c.getCellRenderer();

					if (renderer != null) {
						c.setCellRenderer(new NoBorderTableCellRenderer(renderer));
					}
				});

			Arrays.stream(ObserverTableModel.Column.values())
				.forEach(c -> table.getColumnModel().getColumn(c.index).setPreferredWidth(c.width));

			JScrollPane tablePane = new JScrollPane(table);

			panel.add(tablePane);

			observerPanelMap.put(campaign, new AbstractMap.SimpleEntry<>(panel, tableModel));
		} else {
			panel = panelEntry.getKey();
		}

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