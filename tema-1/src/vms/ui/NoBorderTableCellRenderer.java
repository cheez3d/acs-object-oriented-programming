package vms.ui;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

public class NoBorderTableCellRenderer implements TableCellRenderer {

	private static final Border DEFAULT_BORDER = new EmptyBorder(1, 1, 1, 1);

	private TableCellRenderer renderer;

	public NoBorderTableCellRenderer(TableCellRenderer renderer) {
		this.renderer = renderer;
	}

	@Override
	public Component getTableCellRendererComponent(
		JTable table, Object value, boolean isSelected, boolean hasFocus,
		int row, int column
	) {
		Component component = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		if (component instanceof JComponent) {
			((JComponent)component).setBorder(DEFAULT_BORDER);
		}

		return component;
	}

}