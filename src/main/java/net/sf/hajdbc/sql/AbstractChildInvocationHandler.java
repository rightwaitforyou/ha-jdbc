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
package net.sf.hajdbc.sql;

import java.util.Map;

import net.sf.hajdbc.Database;
import net.sf.hajdbc.DatabaseCluster;
import net.sf.hajdbc.logging.Level;

/**
 * @author Paul Ferraro
 * @param <D> 
 * @param <P> 
 * @param <T> 
 */
public abstract class AbstractChildInvocationHandler<Z, D extends Database<Z>, P, PE extends Exception, T, E extends Exception> extends AbstractInvocationHandler<Z, D, T, E>
{
	private P parentObject;
	private SQLProxy<Z, D, P, PE> parentProxy;
	private Invoker<Z, D, P, T, PE> parentInvoker;

	protected AbstractChildInvocationHandler(P parent, SQLProxy<Z, D, P, PE> proxy, Invoker<Z, D, P, T, PE> invoker, Class<T> proxyClass, Map<D, T> objectMap)
	{
		super(proxyClass, objectMap);
		
		this.parentObject = parent;
		this.parentProxy = proxy;
		this.parentInvoker = invoker;
		this.parentProxy.addChild(this);
	}
	
	@Override
	protected T createObject(D database) throws E
	{
		P object = this.parentProxy.getObject(database);
		
		if (object == null)
		{
			throw new IllegalStateException();
		}
		
		try
		{
			return this.parentInvoker.invoke(database, object);
		}
		catch (Exception e)
		{
			throw this.getExceptionFactory().createException(e);
		}
	}

	@Override
	protected void close(D database, T object)
	{
		try
		{
			this.close(this.parentProxy.getObject(database), object);
		}
		catch (Exception e)
		{
			this.logger.log(Level.INFO, e, e.getMessage());
		}
	}
	
	protected abstract void close(P parent, T object) throws E;
	
	/**
	 * @see net.sf.hajdbc.sql.SQLProxy#getRoot()
	 */
	@Override
	public final RootSQLProxy<Z, D, ? extends Exception> getRoot()
	{
		return this.parentProxy.getRoot();
	}
	
	protected P getParent()
	{
		return this.parentObject;
	}
	
	protected SQLProxy<Z, D, P, PE> getParentProxy()
	{
		return this.parentProxy;
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.sql.SQLProxy#getDatabaseCluster()
	 */
	@Override
	public DatabaseCluster<Z, D> getDatabaseCluster()
	{
		return this.getRoot().getDatabaseCluster();
	}
}
