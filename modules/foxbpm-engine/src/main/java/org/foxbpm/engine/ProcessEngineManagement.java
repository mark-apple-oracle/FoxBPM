/**
 * Copyright 1996-2014 FoxBPM ORG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author kenshin
 * @author ych
 */
package org.foxbpm.engine;

import java.util.HashMap;
import java.util.Map;

public abstract class ProcessEngineManagement {

	public static final String NAME_DEFAULT = "default";
	
	public static final String NAME_DESIGNER = "designer";

	protected static boolean isInitialized = false;
	
	protected static Map<String, ProcessEngine> processEngines = new HashMap<String, ProcessEngine>();

	/**
	 * 注册一个流程引擎 获取一系列包含工作流方法Services。
	 * ProcessEngine和服务对象都是线程安全的，因此你可以在整个服务器中保留对它们任何一个的引用。
	 * 
	 * @param processEngine
	 */
	public static void registerProcessEngine(ProcessEngine processEngine) {
		processEngines.put(processEngine.getName(), processEngine);
	}

	/**
	 * 注销的流程引擎。
	 * 
	 * @param processEngine
	 *            流程引擎实例
	 */
	public static void unregister(ProcessEngine processEngine) {
		processEngines.remove(processEngine.getName());
	}

	/**
	 * 获取默认的流程引擎
	 * 
	 * @return 流程引擎实例
	 */
	public static ProcessEngine getDefaultProcessEngine() {
		return getProcessEngine(NAME_DEFAULT);
	}

	/**
	 * 初始化流程引擎管理器
	 */
	public synchronized static void init(String processEngineName) {
		if (!isInitialized) {
			if (processEngines == null) {
				processEngines = new HashMap<String, ProcessEngine>();
			}
			ProcessEngineConfiguration.createProcessEngineConfiguration(processEngineName)
			.setProcessEngineName(NAME_DEFAULT).buildProcessEngine();
			
			isInitialized = true;
		} else {
			// 记录日志
		}
	}

	/**
	 * 获取流程引擎
	 * 
	 * @param processEngineName
	 *            流程引擎名称
	 * @return 流程引擎实例
	 */
	public static ProcessEngine getProcessEngine(String processEngineName) {
		if (!isInitialized) {
			init(processEngineName);
		}

		return processEngines.get(processEngineName);
	}

	/**
	 * 关闭所有的流程引擎。
	 */
	public synchronized static void destroy() {
		if (isInitialized) {
			Map<String, ProcessEngine> engines = new HashMap<String, ProcessEngine>(processEngines);
			processEngines = new HashMap<String, ProcessEngine>();

			for (String processEngineName : engines.keySet()) {
				ProcessEngine processEngine = engines.get(processEngineName);
				try {
					processEngine.closeEngine();
				} catch (Exception e) {
					// 抛出异常
				}
			}

			isInitialized = false;
		}
	}
	
	/**
	 * 手工设置引擎初始化状态，手工初始化时使用
	 * 引擎初始化部分存在问题，需要重构
	 * 问题1.内部调用引擎方式是否不应该从getDefaultEngine
	 * 问题2.是否应该支持多引擎操作方式
	 * 问题3.多引擎时，dbUtils等很多地方会出现问题，因为都是从默认引擎拿配置。是否考虑改成线程副本形式。
	 */
	public static void setInit(){
		isInitialized = true;
	}
}
