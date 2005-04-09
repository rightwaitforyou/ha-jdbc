/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2004 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.sql;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.sf.hajdbc.SQLException;

/**
 * Provides temp file support for serializing data for large objects.
 * Data is GZIPed for disk optimization and simple obfuscation.
 * Any files created by this object are deleted when {@link #close()} is called.
 * 
 * @author  Paul Ferraro
 * @version $Revision$
 * @since   1.0
 */
public class FileSupport
{
	private static final String TEMP_FILE_PREFIX = "ha-jdbc";
	private static final String TEMP_FILE_SUFFIX = "lob";
	private static final int BUFFER_SIZE = 8192;
	
	private List fileList = new LinkedList();
	
	/**
	 * Create a file from the specified binary input stream.
	 * @param inputStream a binary stream of data
	 * @return a temporary file
	 * @throws java.sql.SQLException if an IO error occurs
	 */
	public File createFile(InputStream inputStream) throws java.sql.SQLException
	{
		File file = this.createTempFile();;
		
		try
		{
			OutputStream outputStream = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file), BUFFER_SIZE));
			byte[] chunk = new byte[BUFFER_SIZE];
			int byteCount = inputStream.read(chunk);
			
			while (byteCount >= 0)
			{
				outputStream.write(chunk, 0, byteCount);
				byteCount = inputStream.read(chunk);
			}
			
			outputStream.flush();
			outputStream.close();
			
			return file;
		}
		catch (IOException e)
		{
			throw new SQLException(e);
		}
	}
	
	/**
	 * Create a file from the specified character input stream
	 * @param reader a character stream of data
	 * @return a temporary file
	 * @throws java.sql.SQLException if an IO error occurs
	 */
	public File createFile(Reader reader) throws java.sql.SQLException
	{
		File file = this.createTempFile();;
		
		try
		{
			Writer writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file))), BUFFER_SIZE);
			char[] chunk = new char[BUFFER_SIZE];
			int byteCount = reader.read(chunk);
			
			while (byteCount >= 0)
			{
				writer.write(chunk, 0, byteCount);
				byteCount = reader.read(chunk);
			}
			
			writer.flush();
			writer.close();
			
			return file;
		}
		catch (IOException e)
		{
			throw new SQLException(e);
		}
	}
	
	/**
	 * Returns a reader for the specified file.
	 * @param file a temp file
	 * @return a reader
	 * @throws java.sql.SQLException if IO error occurs
	 */
	public Reader getReader(File file) throws java.sql.SQLException
	{
		return new InputStreamReader(this.getInputStream(file));
	}
	
	/**
	 * Returns an input stream for the specified file.
	 * @param file a temp file
	 * @return an input stream
	 * @throws java.sql.SQLException if IO error occurs
	 */
	public InputStream getInputStream(File file) throws java.sql.SQLException
	{
		try
		{
			return new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)), BUFFER_SIZE);
		}
		catch (IOException e)
		{
			throw new SQLException(e);
		}
	}
	
	/**
	 * Creates a temp file and stores a reference to it so that it can be deleted later.
	 * @return a temp file
	 * @throws SQLException if an IO error occurs
	 */
	private File createTempFile() throws SQLException
	{
		try
		{
			File file = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
			
			this.fileList.add(file);
			
			return file;
		}
		catch (IOException e)
		{
			throw new SQLException(e);
		}
	}
	
	/**
	 * Deletes any files created by this object.
	 */
	public void close()
	{
		while (!this.fileList.isEmpty())
		{
			File file = (File) this.fileList.remove(0);
			
			file.delete();
		}
	}
	
	/**
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable
	{
		this.close();
		
		super.finalize();
	}
}