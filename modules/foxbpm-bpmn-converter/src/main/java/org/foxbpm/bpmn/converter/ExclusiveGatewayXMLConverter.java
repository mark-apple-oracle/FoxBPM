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
 * @author yangguangftlp
 */
package org.foxbpm.bpmn.converter;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.foxbpm.bpmn.constants.BpmnXMLConstants;
import org.foxbpm.model.BaseElement;
import org.foxbpm.model.ExclusiveGateway;
import org.foxbpm.model.FlowElement;

/**
 * 排他网关转换器
 * 
 * @author yangguangftlp
 * @date 2014年10月15日
 */
public class ExclusiveGatewayXMLConverter extends GatewayXMLConverter {
	
	public FlowElement cretateFlowElement() {
		return new ExclusiveGateway();
	}
	
	@Override
	public Class<? extends BaseElement> getBpmnElementType() {
		return ExclusiveGateway.class;
	}
	
	@Override
	public void convertXMLToModel(Element element, BaseElement baseElement) {
		// TODO Auto-generated method stub
		super.convertXMLToModel(element, baseElement);
	}
	
	@Override
	public void convertModelToXML(Element element, BaseElement baseElement) {
		// TODO Auto-generated method stub
		super.convertModelToXML(element, baseElement);
	}
	
	@Override
	public String getXMLElementName() {
		return BpmnXMLConstants.ELEMENT_EXCLUSIVEGATEWAY;
	}
	
	public Element cretateXMLElement() {
		return DocumentFactory.getInstance().createElement(BpmnXMLConstants.BPMN2_PREFIX + ':'
		        + BpmnXMLConstants.ELEMENT_EXCLUSIVEGATEWAY, BpmnXMLConstants.BPMN2_NAMESPACE);
	}
	
}
