/* Licensed under the Apache License, Version 2.0 (the "License");
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
 */
package org.foxbpm.bpmn.converter.export;

import org.dom4j.CDATA;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.foxbpm.bpmn.constants.BpmnXMLConstants;
import org.foxbpm.model.Process;
import org.springframework.util.StringUtils;

public class ProcessExport extends BpmnExport {
	
	public static void writeProcess(Process process, Element parentElement) throws Exception {
		if (StringUtils.isEmpty(process)) {
			// 创建流程元素
			Element processEle = DocumentFactory.getInstance().createElement(BpmnXMLConstants.BPMN2_PREFIX + ':'
			        + BpmnXMLConstants.ELEMENT_PROCESS);
			processEle.addAttribute(BpmnXMLConstants.ATTRIBUTE_ID, process.getId());
			processEle.addAttribute(BpmnXMLConstants.ATTRIBUTE_NAME, process.getName());
			processEle.addAttribute(BpmnXMLConstants.ATTRIBUTE_CATEGORY, process.getCategory());
			processEle.addAttribute(BpmnXMLConstants.ATTRIBUTE_KEY, process.getKey());
			// 基本属性设置
			Element extensionElements = DocumentFactory.getInstance().createElement(BpmnXMLConstants.BPMN2_PREFIX + ':'
			        + BpmnXMLConstants.ELEMENT_EXTENSION_ELEMENTS);
			// 表单url
			Element formUri = DocumentFactory.getInstance().createElement(BpmnXMLConstants.FOXBPM_PREFIX + ':'
			        + BpmnXMLConstants.ELEMENT_FORMURI);
			Element expression = DocumentFactory.getInstance().createElement(BpmnXMLConstants.FOXBPM_PREFIX + ':'
			        + BpmnXMLConstants.ELEMENT_EXPRESSION);
			expression.addAttribute("xsi:type", "foxbpm:Expression");
			expression.addAttribute(BpmnXMLConstants.ATTRIBUTE_ID, BpmnXMLConstants.FOXBPM_PREFIX + ':'
			        + BpmnXMLConstants.ELEMENT_EXPRESSION);
			expression.addAttribute(BpmnXMLConstants.ATTRIBUTE_NAME, BpmnXMLConstants.EMPTY_STRING);
			CDATA cdata = DocumentFactory.getInstance().createCDATA(process.getFormUri());
			expression.add(cdata);
			formUri.add(expression);
			extensionElements.add(formUri);
			
		}
		
	}
}
