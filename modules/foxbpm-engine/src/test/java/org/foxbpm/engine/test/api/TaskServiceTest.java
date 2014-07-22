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
package org.foxbpm.engine.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.foxbpm.engine.exception.FoxBPMBizException;
import org.foxbpm.engine.impl.entity.TaskEntity;
import org.foxbpm.engine.impl.identity.Authentication;
import org.foxbpm.engine.impl.task.command.ExpandTaskCommand;
import org.foxbpm.engine.impl.util.GuidUtil;
import org.foxbpm.engine.task.Task;
import org.foxbpm.engine.task.TaskQuery;
import org.foxbpm.engine.test.AbstractFoxBpmTestCase;
import org.foxbpm.engine.test.Deployment;
import org.junit.Test;

public class TaskServiceTest extends AbstractFoxBpmTestCase {

	
	/**
	 * <p>应用场景：多用于需要人工插入任务时，或者同步其他系统任务时使用</p>
	 * <p>测试用例：创建任务（两种方式），验证必须字段（id,createTime,isIdentityLinksInitialized）是否正确，</p>
	 */
	@Test
	public void testNewTask(){
		Task task = taskService.newTask();
		//强制转换使用有风险，实际项目慎用
		TaskEntity tmpTask = (TaskEntity)task;
		assertTrue(tmpTask.isIdentityLinksInitialized());
		assertNotNull(tmpTask.getId());
		assertNotNull(tmpTask.getCreateTime());
		task = taskService.newTask("taskId");
		assertEquals("taskId", task.getId());
	}
	
	/**
	 * <p>应用场景：多用于需要人工插入任务时，向数据库run_task表插入数据</p>
	 * <p>测试用例：创建任务->保存任务->查询任务->验证任务部分属性，</p>
	 */
	@Test
	public void testSaveAndFindTask(){
		
		//构造task
		String taskId = GuidUtil.CreateGuid();
		Task task = taskService.newTask(taskId);
		task.setName("name");
		task.setAssignee("assignee");
		
		//保存task
		taskService.saveTask(task);
		
		//查询task
		Task tmpTask = taskService.findTask(taskId);
		
		//验证结果正确性，这里只验证两个简单字段，字段完整性统一放到queryTest里面验证
		assertEquals("name", tmpTask.getName());
		assertEquals("assignee",tmpTask.getAssignee());
	}
	
	/**
	 * <p>测试级联删除任务</p>
	 * <p>测试用例：发布任务->启动 任务->删除当前待办任务->taskQuery、sql查询验证任务表和候选人表均被删除</p>
	 */
	@Test
	@Deployment(resources = {"org/foxbpm/test/api/Test_taskService_1.bpmn"})
	public void testDeleteTaskCascade(){
		
		//启动一个流程
		Authentication.setAuthenticatedUserId("admin");
		ExpandTaskCommand expandTaskCommand = new ExpandTaskCommand();
		expandTaskCommand.setInitiator("admin");
		expandTaskCommand.setProcessDefinitionKey("Test_taskService_1");
		expandTaskCommand.setBusinessKey("bizKey");
		expandTaskCommand.setCommandType("startandsubmit");
		expandTaskCommand.setTaskCommandId("HandleCommand_1");
		
		taskService.expandTaskComplete(expandTaskCommand, null);
		
		
		TaskQuery taskQuery = taskService.createTaskQuery();
		Task task = taskQuery.processDefinitionKey("Test_taskService_1").taskNotEnd().singleResult();
		
		String taskId = task.getId();
		//查询此时的identityLink
		String sql = "select * from foxbpm_run_taskIdentitylink where task_id = ?";
		List<Map<String,Object>> results = jdbcTemplate.queryForList(sql, taskId);
		//删除前候选人存在
		assertEquals(1, results.size());
		
		//级联删除任务
		taskService.deleteTask(taskId);
		
		long taskCount = taskService.createTaskQuery().taskId(taskId).count();
		assertEquals(0, taskCount);
		
		int identityCount = jdbcTemplate.queryForList(sql, taskId).size();
		assertEquals(0, identityCount);
		
	}
	
	/**
	 * <p>测试非级联删除任务</p>
	 * <p>测试用例：发布任务->启动 任务->删除任务->taskQuery、sql查询验证 任务被删除，候选人未删除</p>
	 */
	@Test
	@Deployment(resources = {"org/foxbpm/test/api/Test_taskService_1.bpmn"})
	public void testDeleteTaskNotCascade(){
		
		//启动一个流程
		Authentication.setAuthenticatedUserId("admin");
		ExpandTaskCommand expandTaskCommand = new ExpandTaskCommand();
		expandTaskCommand.setInitiator("admin");
		expandTaskCommand.setProcessDefinitionKey("Test_taskService_1");
		expandTaskCommand.setBusinessKey("bizKey");
		expandTaskCommand.setCommandType("startandsubmit");
		expandTaskCommand.setTaskCommandId("HandleCommand_1");
		
		taskService.expandTaskComplete(expandTaskCommand, null);
		
		
		TaskQuery taskQuery = taskService.createTaskQuery();
		Task task = taskQuery.processDefinitionKey("Test_taskService_1").taskNotEnd().singleResult();
		
		String taskId = task.getId();
		//查询此时的identityLink
		String sql = "select * from foxbpm_run_taskIdentitylink where task_id = ?";
		List<Map<String,Object>> results = jdbcTemplate.queryForList(sql, taskId);
		assertEquals(1, results.size());
		
		taskService.deleteTask(taskId,false);
		
		results = jdbcTemplate.queryForList(sql, taskId);
		assertEquals(1, results.size());
		
		long taskCount = taskService.createTaskQuery().taskId(taskId).count();
		assertEquals(0, taskCount);
		
		
	}
	
	/**
	 * <p>测试非级联删除任务集合</p>
	 * <p>测试用例：发布任务->启动 任务->删除任务->taskQuery、sql查询验证Test_taskService_1的所有任务被删除，并且候选人未被清空</p>
	 */
	@Test
	@Deployment(resources = {"org/foxbpm/test/api/Test_taskService_1.bpmn"})
	public void testDeleteTasksNotCascade(){
		
		//启动一个流程
		Authentication.setAuthenticatedUserId("admin");
		ExpandTaskCommand expandTaskCommand = new ExpandTaskCommand();
		expandTaskCommand.setInitiator("admin");
		expandTaskCommand.setProcessDefinitionKey("Test_taskService_1");
		expandTaskCommand.setBusinessKey("bizKey");
		expandTaskCommand.setCommandType("startandsubmit");
		expandTaskCommand.setTaskCommandId("HandleCommand_1");
		
		taskService.expandTaskComplete(expandTaskCommand, null);
		
		
		TaskQuery taskQuery = taskService.createTaskQuery();
		List<Task> tasks = taskQuery.processDefinitionKey("Test_taskService_1").list();
		List<String> taskIds = new ArrayList<String>();
		for(Task tmp : tasks){
			taskIds.add(tmp.getId());
		}
		String taskId = taskQuery.taskNotEnd().singleResult().getId();
		
		taskService.deleteTasks(taskIds,false);
		
		
		//查询此时的identityLink
		String sql = "select * from foxbpm_run_taskIdentitylink where task_id = ?";
		
		List<Map<String,Object>> results = jdbcTemplate.queryForList(sql, taskId);
		assertEquals(1, results.size());
		
		long taskCount = taskService.createTaskQuery().processDefinitionKey("Test_taskService_1").count();
		assertEquals(0, taskCount);
	}
	
	/**
	 * <p>测试级联删除任务集合</p>
	 * <p>测试用例：发布任务->启动 任务->删除任务->taskQuery、sql查询验证Test_taskService_1的任务被删除，并且候选人也被全部删除</p>
	 */
	@Test
	@Deployment(resources = {"org/foxbpm/test/api/Test_taskService_1.bpmn"})
	public void testDeleteTasksCascade(){
		
		//启动一个流程
		Authentication.setAuthenticatedUserId("admin");
		ExpandTaskCommand expandTaskCommand = new ExpandTaskCommand();
		expandTaskCommand.setInitiator("admin");
		expandTaskCommand.setProcessDefinitionKey("Test_taskService_1");
		expandTaskCommand.setBusinessKey("bizKey");
		expandTaskCommand.setCommandType("startandsubmit");
		expandTaskCommand.setTaskCommandId("HandleCommand_1");
		
		taskService.expandTaskComplete(expandTaskCommand, null);
		
		
		TaskQuery taskQuery = taskService.createTaskQuery();
		List<Task> tasks = taskQuery.processDefinitionKey("Test_taskService_1").list();
		List<String> taskIds = new ArrayList<String>();
		for(Task tmp : tasks){
			taskIds.add(tmp.getId());
		}
		
		taskService.deleteTasks(taskIds);
		
		//查询此时的identityLink
		String sql = "select * from foxbpm_run_taskIdentitylink where task_id = ?";
		
		for(String tmpId: taskIds){
			List<Map<String,Object>> results = jdbcTemplate.queryForList(sql, tmpId);
			assertEquals(0, results.size());
		}
		
		long taskCount = taskService.createTaskQuery().processDefinitionKey("Test_taskService_1").count();
		assertEquals(0, taskCount);
	}
	
	/**
	 * <p>测试领取任务</p>
	 * <p>测试用例：发布任务->启动 任务->领取任务->验证：任务assgine字段正确，验证重复领取时抛出异常->释放任务->验证任务是否已被释放</p>
	 */
	@Test
	@Deployment(resources = {"org/foxbpm/test/api/Test_taskService_1.bpmn"})
	public void testClaimAndUnClaim(){
		//启动一个流程
		Authentication.setAuthenticatedUserId("admin");
		ExpandTaskCommand expandTaskCommand = new ExpandTaskCommand();
		expandTaskCommand.setInitiator("admin");
		expandTaskCommand.setProcessDefinitionKey("Test_taskService_1");
		expandTaskCommand.setBusinessKey("bizKey");
		expandTaskCommand.setCommandType("startandsubmit");
		expandTaskCommand.setTaskCommandId("HandleCommand_1");
		
		taskService.expandTaskComplete(expandTaskCommand, null);
		
		String taskId = taskService.createTaskQuery().processDefinitionKey("Test_taskService_1").taskNotEnd().singleResult().getId();
		
		//领取任务
		taskService.claim(taskId, "admin2");
		
		//查询任务
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		//验证结果
		assertEquals("admin2", task.getAssignee());
		
		try{
			//领取任务
			taskService.claim(taskId, "admin2");
			fail();
		}catch(FoxBPMBizException ex){
			
		}
		
		//释放任务
		taskService.unclaim(taskId);
		
		task = taskService.createTaskQuery().taskId(taskId).singleResult();
		//验证结果
		assertEquals(null, task.getAssignee());
		
	}
	
	
	
	public void testTaskQuery(){
		
//		Authentication.setAuthenticatedUserId("2222");
//		Map<String, Object> transientVariables=new HashMap<String, Object>();
//		transientVariables.put("value", 10);
//		ProcessInstance processInstance=runtimeService.startProcessInstanceById
//		("process_foxbpm_1:1:ded8ceeb-6d8e-4cd9-b6e1-8e49a25beef7","bizkey",transientVariables, null);
//		
//		
//		Task task=taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskNotEnd().singleResult();
//		
//		ExpandTaskCommand expandCommand = new ExpandTaskCommand();
//		expandCommand.setCommandType("general");
//		expandCommand.setTaskCommandId("HandleCommand_1");
//		expandCommand.setTaskId(task.getId());
//		taskService.expandTaskComplete(expandCommand, null);
		
//		taskService.createTaskQuery().taskAssignee("2222").taskCandidateUser("2222").taskNotEnd().listPagination(1, 10);
		
//		ProcessDefinition processDefinition = modelService.getProcessDefinition("process_test_1", 1);
//		System.out.println(processDefinition.getName());
		
//		task=taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskNotEnd().singleResult();
//		ExpandTaskCommand expandCommand2 = new ExpandTaskCommand();
//		expandCommand2.setCommandType("general");
//		expandCommand2.setTaskCommandId("HandleCommand_1");
//		expandCommand2.setTaskId(task.getId());
//		taskService.expandTaskComplete(expandCommand2, null);
//		
//		processInstance=runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
//		
//		assertTrue(processInstance.isEnd());
		
	}
	
//	public void testGetTaskCommand(){
//		Authentication.setAuthenticatedUserId("2222");
//		Map<String, Object> transientVariables=new HashMap<String, Object>();
//		transientVariables.put("value", 10);
//		ProcessInstance processInstance=runtimeService.startProcessInstanceById
//		("process_foxbpm_1:1:ded8ceeb-6d8e-4cd9-b6e1-8e49a25beef7","bizkey",transientVariables, null);
//		Task task=taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskNotEnd().singleResult();
//		
//		List<TaskCommand> taskCommands = taskService.getTaskCommandByTaskId(task.getId());
//		System.out.println(taskCommands.size());
//		
//	}
//	
//	public void testGetTaskCommandByKey(){
//		Authentication.setAuthenticatedUserId("2222");
//		List<TaskCommand> taskCommands = taskService.getSubTaskCommandByKey("process_foxbpm_1");
//		
//		System.out.println(taskCommands);
//	}
	
	
}
