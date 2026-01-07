/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2026
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
package org.citydb.plugins.ade_manager.registry.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DBStoredFunctionCollection implements Map<String, DBStoredFunction> {
	private final String br = System.lineSeparator();
	private Map<String, DBStoredFunction> functions = new TreeMap<String, DBStoredFunction>();
	
	@Override
	public void clear() {
		functions.clear();	
	}

	@Override
	public boolean containsKey(Object key) {
		return functions.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return functions.containsValue(value);
	}

	@Override
	public Set<Entry<String, DBStoredFunction>> entrySet() {
		return functions.entrySet();
	}

	@Override
	public DBStoredFunction get(Object key) {
		return functions.get(key);
	}

	@Override
	public boolean isEmpty() {
		return functions.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return functions.keySet();
	}

	@Override
	public DBStoredFunction put(String key, DBStoredFunction value) {
		return functions.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends DBStoredFunction> m) {
		functions.putAll(m);
	}

	@Override
	public DBStoredFunction remove(Object key) {
		return functions.remove(key);
	}

	@Override
	public int size() {
		return functions.size();
	}

	@Override
	public Collection<DBStoredFunction> values() {
		return functions.values();
	}
	
	public String printFunctionNameList() {
		return printFunctionNameList("");
	}
	
	public String printFunctionNameList(String prefix) {
		StringBuilder builder = new StringBuilder();
		for (DBStoredFunction func: values()) 
			builder.append(prefix).append(func.getDeclareField()).append(br);
		
		return builder.toString();
	}
	
	
	public String printFunctionDeclareFields(String prefix) {
		StringBuilder builder = new StringBuilder();
		for (DBStoredFunction func: values()) {
			builder.append(prefix).append(func.getDeclareField()).append(";").append(br);
		};
		
		return builder.toString();
	}

	public String printFunctionDefinitions(String separatorLine) {
		StringBuilder builder = new StringBuilder();
		for (DBStoredFunction func: values()) {
			String funcDefinition = func.getDefinition();
			builder.append(funcDefinition).append(br);
			builder.append(separatorLine).append(br).append(br);
		};
		
		return builder.toString();
	}
}
