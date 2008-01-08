/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2007 Paul Ferraro
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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.hajdbc.Database;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("nls")
public class ConnectionInvocationHandler<D> extends AbstractChildInvocationHandler<D, D, Connection>
{
	private static final Set<String> DRIVER_READ_METHOD_SET = new HashSet<String>(Arrays.asList("createArrayOf", "createBlob", "createClob", "createNClob", "createSQLXML", "createStruct", "getAutoCommit", "getCatalog", "getClientInfo", "getHoldability", "getTypeMap", "getWarnings", "isClosed", "isReadOnly", "nativeSQL"));
	private static final Set<String> DATABASE_READ_METHOD_SET = new HashSet<String>(Arrays.asList("getMetaData", "getTransactionIsolation", "isValid"));
	private static final Set<String> DRIVER_WRITE_METHOD_SET = new HashSet<String>(Arrays.asList("clearWarnings", "setAutoCommit", "setClientInfo", "setHoldability", "setTypeMap"));
	private static final Set<String> DATABASE_WRITE_METHOD_SET = new HashSet<String>(Arrays.asList("commit", "releaseSavepoint", "rollback"));
	
	private FileSupport fileSupport;
	
	public ConnectionInvocationHandler(D proxy, SQLProxy<D, D> handler, Invoker<D, D, Connection> invoker, Map<Database<D>, Connection> connectionMap, FileSupport fileSupport) throws Exception
	{
		super(proxy, handler, invoker, Connection.class, connectionMap);
		
		this.fileSupport = fileSupport;
	}
	
	/**
	 * @see net.sf.hajdbc.sql.AbstractChildInvocationHandler#getInvocationStrategy(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	@Override
	protected InvocationStrategy<D, Connection, ?> getInvocationStrategy(Connection connection, Method method, Object[] parameters) throws Exception
	{
		String methodName = method.getName();
		
		if (DRIVER_READ_METHOD_SET.contains(methodName))
		{
			return new DriverReadInvocationStrategy<D, Connection, Object>();
		}
		
		if (DATABASE_READ_METHOD_SET.contains(methodName))
		{
			return new DatabaseReadInvocationStrategy<D, Connection, Object>();
		}
		
		if (DRIVER_WRITE_METHOD_SET.contains(methodName))
		{
			return new DriverWriteInvocationStrategy<D, Connection, Object>();
		}
		
		if (DATABASE_WRITE_METHOD_SET.contains(methodName))
		{
			return new TransactionalDatabaseWriteInvocationStrategy<D, Connection, Object>();
		}
		
		if (methodName.startsWith("prepare") || methodName.endsWith("Statement"))
		{
			boolean readOnly = connection.isReadOnly();
			
			if (methodName.equals("createStatement"))
			{
				if (readOnly)
				{
					return new DriverReadInvocationStrategy<D, Connection, Object>();
				}

				return new StatementInvocationStrategy<D>(connection, this.fileSupport);
			}

			if (readOnly)
			{
				return new DatabaseReadInvocationStrategy<D, Connection, Object>();
			}
			
			if (methodName.equals("prepareStatement"))
			{
				return new PreparedStatementInvocationStrategy<D>(connection, this.fileSupport, (String) parameters[0]);
			}
			else if (methodName.equals("prepareCall"))
			{
				return new CallableStatementInvocationStrategy<D>(connection, this.fileSupport);
			}
		}
		
		if (methodName.equals("setSavepoint"))
		{
			return new SavepointInvocationStrategy<D>(connection);
		}
/*		
		if (methodName.equals("createBlob"))
		{
			return new BlobInvocationStrategy<D, Connection>(connection);
		}
		
		if (methodName.equals("createClob") || methodName.equals("createNClob"))
		{
			return new ClobInvocationStrategy<D, Connection>(connection, method.getReturnType().asSubclass(Clob.class));
		}
*/		
		return super.getInvocationStrategy(connection, method, parameters);
	}

	/**
	 * @see net.sf.hajdbc.sql.AbstractChildInvocationHandler#getInvoker(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Invoker<D, Connection, ?> getInvoker(Connection connection, Method method, Object[] parameters) throws Exception
	{
		if (method.equals(Connection.class.getMethod("releaseSavepoint", Savepoint.class)))
		{
			final SQLProxy<D, Savepoint> proxy = (SQLProxy) Proxy.getInvocationHandler(parameters[0]);
			
			return new Invoker<D, Connection, Void>()
			{
				public Void invoke(Database<D> database, Connection connection) throws SQLException
				{
					connection.releaseSavepoint(proxy.getObject(database));
					
					return null;
				}					
			};
		}
		
		if (method.equals(Connection.class.getMethod("rollback", Savepoint.class)))
		{
			final SQLProxy<D, Savepoint> proxy = (SQLProxy) Proxy.getInvocationHandler(parameters[0]);
			
			return new Invoker<D, Connection, Void>()
			{
				public Void invoke(Database<D> database, Connection connection) throws SQLException
				{
					connection.rollback(proxy.getObject(database));
					
					return null;
				}					
			};
		}
		
		return super.getInvoker(connection, method, parameters);
	}

	/**
	 * @see net.sf.hajdbc.sql.AbstractChildInvocationHandler#isSQLMethod(java.lang.reflect.Method)
	 */
	@Override
	protected boolean isSQLMethod(Method method)
	{
		return method.getName().equals("prepareStatement");
	}

	/**
	 * @see net.sf.hajdbc.sql.AbstractChildInvocationHandler#postInvoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void postInvoke(Connection object, Method method, Object[] parameters) throws Exception
	{
		if (method.equals(Connection.class.getMethod("close")))
		{
			this.fileSupport.close();
			
			this.getParentProxy().removeChild(this);
		}
		else if (method.equals(Connection.class.getMethod("releaseSavepoint", Savepoint.class)))
		{
			SQLProxy<D, Savepoint> proxy = (SQLProxy) Proxy.getInvocationHandler(parameters[0]);
			
			this.removeChild(proxy);
		}
	}

	/**
	 * @see net.sf.hajdbc.sql.AbstractChildInvocationHandler#close(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void close(D parent, Connection connection) throws SQLException
	{
		connection.close();
	}
}
