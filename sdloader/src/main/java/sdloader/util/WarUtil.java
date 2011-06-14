/*
 * Copyright 2005-2010 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sdloader.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;

/**
 * WarUtil
 * 
 * @author c9katayama
 */
public class WarUtil {

	private static SDLoaderLog log = SDLoaderLogFactory.getLog(WarUtil.class);

	private static final String LAST_MODIFIED_FILE = ".warlastmodified";

	/**
	 * Warの名前を返します。
	 * 
	 * @param warFileName
	 * @return
	 */
	public static String getArchiveName(final String warFileName) {
		return warFileName.substring(0, warFileName.length() - ".war".length());
	}

	private static long readLastModifiled(File file) {
		if (file.exists()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				return Long.valueOf(reader.readLine());
			} catch (Exception e) {
				log.debug(e.getMessage(), e);
			} finally {
				IOUtil.closeNoException(reader);
			}
		}
		return 0;
	}

	private static void writeLastModifiled(File file, long time) {
		BufferedWriter writer = null;
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(Long.toString(time));
			writer.flush();
		} catch (Exception e) {
			log.debug(e.getMessage(), e);
		} finally {
			IOUtil.closeNoException(writer);
		}
	}

	/**
	 * WARファイルを解凍します。
	 * 
	 * @param warFile
	 * @param directory
	 * @throws IOException
	 */
	public static void extractWar(File warFile, File directory)
			throws IOException {
		try {
			long timestamp = warFile.lastModified();
			File warModifiedTimeFile = new File(directory, LAST_MODIFIED_FILE);
			long lastModified = readLastModifiled(warModifiedTimeFile);

			if (timestamp == lastModified) {
				log.info("war file " + warFile.getName() + " not modified.");
				return;
			}
			if (directory.exists()) {
				IOUtil.forceRemoveDirectory(directory);
				directory.mkdir();
			}

			log.info("war extract start. warfile=" + warFile.getName());

			JarInputStream jin = new JarInputStream(new BufferedInputStream(
					new FileInputStream(warFile)));
			JarEntry entry = null;

			while ((entry = jin.getNextJarEntry()) != null) {
				File file = new File(directory, entry.getName());

				if (entry.isDirectory()) {
					if (!file.exists()) {
						file.mkdirs();
					}
				} else {
					File dir = new File(file.getParent());
					if (!dir.exists()) {
						dir.mkdirs();
					}

					FileOutputStream fout = null;
					try {
						fout = new FileOutputStream(file);
						ResourceUtil.copyStream(jin, fout);
					} finally {
						fout.flush();
						fout.close();
						fout = null;
					}

					if (entry.getTime() >= 0) {
						file.setLastModified(entry.getTime());
					}
				}
			}

			writeLastModifiled(warModifiedTimeFile, timestamp);

			log.info("war extract success. lastmodified=" + timestamp);
		} catch (IOException ioe) {
			log.info("war extract fail.");
			throw ioe;
		}
	}
}
