/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.client.program;

import com.dtstack.engine.common.exception.RdosDefineException;
import com.dtstack.engine.base.enums.ClassLoaderType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.Plan;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.runtime.execution.librarycache.FlinkUserCodeClassLoaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A JobWithJars is a Flink dataflow plan, together with a bunch of JAR files that contain
 * the classes of the functions and libraries necessary for the execution.
 */
public class JobWithJars {

	private Plan plan;

	private List<URL> jarFiles;

	/**
	 * classpaths that are needed during user code execution.
	 */
	private List<URL> classpaths;

	private ClassLoader userCodeClassLoader;

	private static Map<String, URLClassLoader> cacheClassLoader = new ConcurrentHashMap<>();


	public JobWithJars(Plan plan, List<URL> jarFiles, List<URL> classpaths) throws IOException {
		this.plan = plan;
		this.jarFiles = new ArrayList<URL>(jarFiles.size());
		this.classpaths = new ArrayList<URL>(classpaths.size());

		for (URL jarFile: jarFiles) {
			checkJarFile(jarFile);
			this.jarFiles.add(jarFile);
		}

		for (URL path: classpaths) {
			this.classpaths.add(path);
		}
	}

	public JobWithJars(Plan plan, URL jarFile) throws IOException {
		this.plan = plan;

		checkJarFile(jarFile);
		this.jarFiles = Collections.singletonList(jarFile);
		this.classpaths = Collections.<URL>emptyList();
	}

	JobWithJars(Plan plan, List<URL> jarFiles, List<URL> classpaths, ClassLoader userCodeClassLoader) {
		this.plan = plan;
		this.jarFiles = jarFiles;
		this.classpaths = classpaths;
		this.userCodeClassLoader = userCodeClassLoader;
	}

	/**
	 * Returns the plan.
	 */
	public Plan getPlan() {
		return this.plan;
	}

	/**
	 * Returns list of jar files that need to be submitted with the plan.
	 */
	public List<URL> getJarFiles() {
		return this.jarFiles;
	}

	/**
	 * Returns list of classpaths that need to be submitted with the plan.
	 */
	public List<URL> getClasspaths() {
		return classpaths;
	}

	/**
	 * Gets the {@link java.lang.ClassLoader} that must be used to load user code classes.
	 *
	 * @return The user code ClassLoader.
	 */
	public ClassLoader getUserCodeClassLoader() {
		if (this.userCodeClassLoader == null) {
			this.userCodeClassLoader = buildUserCodeClassLoader(jarFiles, classpaths, getClass().getClassLoader(), new Configuration(), false);
		}
		return this.userCodeClassLoader;
	}

	public static void checkJarFile(URL jar) throws IOException {
		File jarFile;
		try {
			jarFile = new File(jar.toURI());
		} catch (URISyntaxException e) {
			throw new IOException("JAR file path is invalid '" + jar + "'");
		}
		if (!jarFile.exists()) {
			throw new IOException("JAR file does not exist '" + jarFile.getAbsolutePath() + "'");
		}
		if (!jarFile.canRead()) {
			throw new IOException("JAR file can't be read '" + jarFile.getAbsolutePath() + "'");
		}
		// TODO: Check if proper JAR file
	}

	public static ClassLoader buildUserCodeClassLoader(List<URL> jars, List<URL> classpaths, ClassLoader parent, Configuration flinkConfiguration, boolean classLoaderCache) {

		URL[] urls = new URL[jars.size() + classpaths.size()];
		String[] md5s = new String[urls.length];
		for (int i = 0; i < jars.size(); i++) {
			urls[i] = jars.get(i);
		}
		for (int i = 0; i < classpaths.size(); i++) {
			urls[i + jars.size()] = classpaths.get(i);
		}

		Arrays.sort(urls, Comparator.comparing(URL::toString));

		for (int i = 0; i < urls.length; ++i) {
			try (FileInputStream inputStream = new FileInputStream(urls[i].getPath())){
				md5s[i] = DigestUtils.md5Hex(inputStream);
			} catch (Exception e) {
				throw new RdosDefineException("Exceptions appears when read file:" + e);
			}
		}

		final String[] alwaysParentFirstLoaderPatterns = CoreOptions.getParentFirstLoaderPatterns(flinkConfiguration);
		final String classLoaderResolveOrder = flinkConfiguration.getString(CoreOptions.CLASSLOADER_RESOLVE_ORDER);
		FlinkUserCodeClassLoaders.ResolveOrder resolveOrder = FlinkUserCodeClassLoaders.ResolveOrder.fromString(classLoaderResolveOrder);
		final URLClassLoader classLoader = FlinkUserCodeClassLoaders.create(resolveOrder, urls, parent, alwaysParentFirstLoaderPatterns);

		if (classLoaderCache) {
			String keyCache = classLoaderResolveOrder + StringUtils.join(md5s, "_");
			return cacheClassLoader.computeIfAbsent(keyCache, k -> classLoader);
		} else {
			return classLoader;
		}
	}
}
