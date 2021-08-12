/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2021
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.lrg.tum.de/gis/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * Virtual City Systems, Berlin <https://vc.systems/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.citydb.plugins.ade_manager.event;

import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.util.event.Event;

public final class ScriptCreationEvent extends Event {
	private final DBSQLScript script;
	private final boolean autoInstall;
	
	public ScriptCreationEvent(DBSQLScript script, boolean autoInstall, Object source) {
		super(EventType.SCRIPT_CREATION_EVENT, GLOBAL_CHANNEL, source);
		this.script =  script;
		this.autoInstall = autoInstall;
	}

	public DBSQLScript getScript() {
		return script;
	}
	
	public boolean isAutoInstall() {
		return autoInstall;
	}
	
}
