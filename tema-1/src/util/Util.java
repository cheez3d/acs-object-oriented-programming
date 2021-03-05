package util;

import java.awt.Component;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.IntStream;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public final class Util {

	public static String capitalizeString(String string) {
		return Optional.ofNullable(string)
			.map(s -> IntStream.concat(s.codePoints().limit(1).map(Character::toUpperCase), s.codePoints().skip(1)))
			.map(s -> s.toArray())
			.map(a -> new String(a, 0, a.length))
			.orElse(null);
	}

	public static String joinExceptionCauseChain(Exception e) {
		StringJoiner joiner = new StringJoiner("\n");

		joiner.add(e.getMessage());

		Throwable cause = e.getCause();

		while (cause != null) {
			String message = cause.getMessage();

			if (message != null) {
				joiner.add(cause.getMessage());
			}

			cause = cause.getCause();
		}

		return joiner.toString();
	}

	public static void printExceptionCauseChain(Exception e) {
		System.err.println(e.getMessage());

		Throwable cause = e.getCause();

		while (cause != null) {
			String message = cause.getMessage();

			if (message != null) {
				System.err.println("\t" + cause.getMessage());
			}

			cause = cause.getCause();
		}
	}

	public static Throwable getExceptionRootCause(Exception e) {
		Throwable cause = e;

		while (true) {
			if (cause.getCause() == null) {
				break;
			}

			cause = cause.getCause();
		}

		return cause;
	}

	public static JOptionPane getComponentOptionPane(JComponent component) {
		JOptionPane pane = null;

		if (!(component instanceof JOptionPane)) {
			pane = getComponentOptionPane((JComponent)component.getParent());
		} else {
			pane = (JOptionPane)component;
		}

		return pane;
	}

	public static void resizeTableColumnWidth(JTable table) {
		TableColumnModel columnModel = table.getColumnModel();

		for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
			int width = 4;

			for (int rowIndex = 0; rowIndex < table.getRowCount(); ++rowIndex) {
				TableCellRenderer renderer = table.getCellRenderer(rowIndex, columnIndex);

				Component component = table.prepareRenderer(renderer, rowIndex, columnIndex);

				width = Math.max(component.getPreferredSize().width+1, width);
			}

			if (width > 256) {
				width = 256;
			}

			columnModel.getColumn(columnIndex).setPreferredWidth(width);
		}
	}

	private Util() {
		throw new UnsupportedOperationException();
	}

}