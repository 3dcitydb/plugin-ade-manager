package org.citydb.plugins.ade_manager.registry;

import java.sql.Connection;
import java.sql.SQLException;

import org.citydb.plugins.ade_manager.config.ConfigImpl;

public abstract class DefaultADERegistrationProcessor {
    protected Connection connection;
    protected ConfigImpl config;

	public void commit() throws SQLException {
		try {
    		connection.commit();
    	} catch (SQLException e) {
			throw new SQLException("Failed to exeute the database transaction", e);
		} 	
	}
}
