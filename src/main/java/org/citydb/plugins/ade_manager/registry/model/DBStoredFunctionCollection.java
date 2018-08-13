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
