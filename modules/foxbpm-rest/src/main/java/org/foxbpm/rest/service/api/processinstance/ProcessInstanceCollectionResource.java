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
 * @author ych
 */
package org.foxbpm.rest.service.api.processinstance;

import java.util.Set;

import org.foxbpm.engine.RuntimeService;
import org.foxbpm.engine.impl.util.StringUtil;
import org.foxbpm.engine.runtime.ProcessInstanceQuery;
import org.foxbpm.rest.common.RestConstants;
import org.foxbpm.rest.common.api.AbstractRestResource;
import org.foxbpm.rest.common.api.DataResult;
import org.foxbpm.rest.common.api.FoxBpmUtil;
import org.restlet.data.Form;
import org.restlet.resource.Get;

/**
 * 流程实例rest接口
 * @author ych
 *
 */
public class ProcessInstanceCollectionResource extends AbstractRestResource {

	
	@Get
	public DataResult getProcessInstance(){
		if(!validationUser())
			return null;
		Form queryForm = getQuery();
		Set<String> queryNames = queryForm.getNames();
		
		RuntimeService runtimeService = FoxBpmUtil.getProcessEngine().getRuntimeService();
		ProcessInstanceQuery processIntanceQuery = runtimeService.createProcessInstanceQuery();
		if(queryNames.contains(RestConstants.PARTICIPATE)){
			processIntanceQuery.taskParticipants(userId);
			if(queryNames.contains(RestConstants.INITIATOR)){
				String initotor = StringUtil.getString(getQueryParameter(RestConstants.INITIATOR, queryForm));
				processIntanceQuery.initiator(initotor);
			}
		}else {
			processIntanceQuery.initiator(userId);
		}
		
		DataResult result = paginateList(processIntanceQuery);
		return result;
	}
}