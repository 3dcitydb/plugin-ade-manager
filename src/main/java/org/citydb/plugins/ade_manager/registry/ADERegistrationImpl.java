package org.citydb.plugins.ade_manager.registry;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class ADERegistrationImpl implements ADERegistration {
    protected Connection connection;
    
    @Override
	public void commit() throws SQLException {
		try {
    		connection.commit();
    	} catch (SQLException e) {
			throw new SQLException("Failed to exeute the database transaction", e);
		} 	
	}
}
