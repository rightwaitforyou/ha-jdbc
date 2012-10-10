/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2012  Paul Ferraro
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
package net.sf.hajdbc.sql.xa;

import java.lang.reflect.InvocationHandler;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import net.sf.hajdbc.invocation.Invoker;
import net.sf.hajdbc.sql.InvocationHandlerFactory;
import net.sf.hajdbc.sql.SQLProxy;

/**
 * @author Paul Ferraro
 */
public class XAConnectionInvocationHandlerFactory implements InvocationHandlerFactory<XADataSource, XADataSourceDatabase, XADataSource, XAConnection, SQLException>
{
	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.sql.InvocationHandlerFactory#getTargetClass()
	 */
	@Override
	public Class<XAConnection> getTargetClass()
	{
		return XAConnection.class;
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.sql.InvocationHandlerFactory#createInvocationHandler(java.lang.Object, net.sf.hajdbc.sql.SQLProxy, net.sf.hajdbc.invocation.Invoker, java.util.Map)
	 */
	@Override
	public InvocationHandler createInvocationHandler(XADataSource dataSource, SQLProxy<XADataSource, XADataSourceDatabase, XADataSource, SQLException> proxy, Invoker<XADataSource, XADataSourceDatabase, XADataSource, XAConnection, SQLException> invoker, Map<XADataSourceDatabase, XAConnection> connections) throws SQLException
	{
		return new XAConnectionInvocationHandler(dataSource, proxy, invoker, connections);
	}
}
