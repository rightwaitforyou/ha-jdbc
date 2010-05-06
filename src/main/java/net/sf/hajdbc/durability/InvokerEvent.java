/*
 * HA-JDBC: High-Availability JDBC
 * Copyright 2004-2009 Paul Ferraro
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.hajdbc.durability;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import net.sf.hajdbc.Database;

/**
 * @author paul
 *
 */
public class InvokerEvent extends InvocationEvent
{
	private static final long serialVersionUID = -2796504299012960009L;

	private final String databaseId;
	private boolean completed = false;
	private byte[] result;
	private byte[] exception;
	
	/**
	 * @param source
	 */
	public InvokerEvent(TransactionIdentifier transactionId, Durability.Phase phase, Database<?> database)
	{
		super(transactionId, phase);
		
		this.databaseId = database.getId();
	}

	public InvokerEvent(byte[] transactionId, int phase, String databaseId, boolean completed, byte[] result, byte[] exception)
	{
		super(transactionId, phase);
		
		this.databaseId = databaseId;
		this.completed = completed;
		this.result = result;
		this.exception = exception;
	}
	
	public String getDatabaseId()
	{
		return this.databaseId;
	}
	
	public void setResult(Object result)
	{
		this.result = toBytes(result);
	}

	public void setException(Throwable exception)
	{
		this.exception = toBytes(exception);
	}

	public byte[] getException()
	{
		return this.exception;
	}
	
	public byte[] getResult()
	{
		return this.result;
	}

	public boolean isCompleted()
	{
		return this.completed;
	}

	public void setCompleted(boolean completed)
	{
		this.completed = completed;
	}
	
	private static byte[] toBytes(Object object)
	{
		if (object == null) return null;
		
		if (object instanceof Serializable)
		{
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			
			try
			{
				ObjectOutput output = new ObjectOutputStream(bytes);
				
				try
				{
					output.writeObject(object);
				
					output.flush();
				}
				finally
				{
					try
					{
						output.close();
					}
					catch (IOException e)
					{
						// Ignore
					}
				}
				
				return bytes.toByteArray();
			}
			catch (IOException e)
			{
				// log
			}
		}
		
		return object.getClass().getName().getBytes();
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.durability.InvocationEvent#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object object)
	{
		if ((object == null) || !(object instanceof InvokerEvent)) return false;
		
		InvokerEvent event = (InvokerEvent) object;
		
		return super.equals(object) && this.databaseId.equals(event.databaseId);
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.durability.InvocationEvent#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return super.hashCode() + this.databaseId.hashCode();
	}
}
