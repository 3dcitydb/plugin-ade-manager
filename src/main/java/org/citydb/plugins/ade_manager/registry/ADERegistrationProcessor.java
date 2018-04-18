package org.citydb.plugins.ade_manager.registry;

import java.sql.SQLException;

public interface ADERegistrationProcessor {
	public void commit() throws SQLException;
}
