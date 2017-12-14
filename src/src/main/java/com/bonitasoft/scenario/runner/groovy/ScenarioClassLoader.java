package com.bonitasoft.scenario.runner.groovy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;

import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;

public class ScenarioClassLoader extends ClassLoader {
	private final Map<String, String> dependencyLinks;
	private final Map<String, byte[]> dependencies;

	public ScenarioClassLoader(ClassLoader parentClassLoader, Map<String, byte[]> dependencies) {
		super(parentClassLoader);

		this.dependencies = Collections.unmodifiableMap(dependencies);

		Map<String, String> dependencyLinks = new HashMap<String, String>();
		for (String dependency : dependencies.keySet()) {
			JarInputStream jis = null;
			try {
				jis = new JarInputStream(new ByteArrayInputStream(dependencies.get(dependency)));
				ZipEntry entry;
				while ((entry = jis.getNextEntry()) != null) {
					if (entry.getName().endsWith(".class")) {
						dependencyLinks.put(entry.getName(), dependency);
					}
				}
			} catch (Exception e) {
				ScenarioConfiguration.logger.log(Level.SEVERE, "Impossible to load the scenario jar dependency " + dependency, e);
			} finally {
				if (jis != null)
					try {
						jis.close();
					} catch (IOException e) {

					}
			}
		}
		this.dependencyLinks = dependencyLinks;
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		try {
			return super.loadClass(name, resolve);
		} catch (ClassNotFoundException e) {
			try {
				InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
				byte[] bytes = IOUtils.toByteArray(in);
				Class clazz = defineClass(name, bytes, 0, bytes.length);
				if (resolve) {
					resolveClass(clazz);
				}
				return clazz;
			} catch (Exception e1) {
				throw e;
			}
		}
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		if (!dependencyLinks.containsKey(name)) {
			return super.getResourceAsStream(name);
		}

		boolean found = false;
		JarInputStream jis = null;
		try {
			jis = new JarInputStream(new ByteArrayInputStream(dependencies.get(dependencyLinks.get(name))));
			ZipEntry entry;
			while ((entry = jis.getNextEntry()) != null) {
				if (entry.getName().equals(name)) {
					found = true;
					return jis;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Only close the stream if the entry could not be found
			if (jis != null && !found) {
				try {
					jis.close();
				} catch (IOException e) {
				}
			}
		}

		return null;
	}
}