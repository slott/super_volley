package dk.slott.super_volley.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import android.util.Log;

public class FileUtils {
	private static final String TAG = "FileUtils";

	public static String readTextFile(final String filename) {
		return readTextFile(new File(filename));
	}

	public static String readTextFile(final File filename) {
		Log.d(TAG, "filename: " + filename);
		final StringBuilder text = new StringBuilder((int)filename.length());
		Log.d(TAG, "initial size: " + filename.length());
		FileInputStream fIn = null;
		BufferedReader br = null;
		try {
			fIn = new FileInputStream(filename);
			br = new BufferedReader(new InputStreamReader(fIn));
			String aDataRow = "";
			while ((aDataRow = br.readLine()) != null) {
				text.append(aDataRow);
				text.append("\n");
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Exception while reading text file: " + e);
		}
		// MSH: Catch memory error.
		catch (Error e) {
			Log.e(TAG, "Error: " + e);
			return "";
		}
		finally {
			try {
				if(fIn != null)
					fIn.close();
			} catch (IOException e) {
				Log.e(TAG, "IOException: " + e);
			}
			if(br != null)
				try {
					br.close();
				} catch (IOException e) {
					Log.e(TAG, "Exception while closing reader: " + e);
				}
		}
		Log.d(TAG, "final size: " + text.length());
		return text.toString();
	}
	
	
	/**
	 * Write data to file and close when done.
	 * @param text
	 * @param file
	 */
	public static void writeTextFile(final String text, final File file) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(file.getAbsolutePath(), false), 512);
			out.write(text);
		} catch (Exception e) {
			Log.e(TAG, "Exceptioin writing file:" + e);
		}
		finally {
			try {
				if(out != null)
					out.flush();
			} catch (IOException e) {
				Log.e(TAG, "Exception while flushing out: " + e);
			}
			try {
				if(out != null)
					out.close();
			} catch (IOException e) {
				Log.e(TAG, "Exception while closing out: " + e);
			}
		}
	}
	
}
