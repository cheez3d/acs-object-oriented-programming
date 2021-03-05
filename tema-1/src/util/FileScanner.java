package util;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.Scanner;

public final class FileScanner {

	private final File file;
	private final Scanner scanner;

	public File getFile() {
		return file;
	}

	public Scanner getScanner() {
		return scanner;
	}

	public boolean isClosed() {
		try {
			scanner.hasNext();
		} catch (IllegalStateException e) {
			return true;
		}

		return false;
	}

	public FileScanner(File file) throws FileNotFoundException, NullPointerException {
		this.file = file;
		this.scanner = new Scanner(file);
	}

}