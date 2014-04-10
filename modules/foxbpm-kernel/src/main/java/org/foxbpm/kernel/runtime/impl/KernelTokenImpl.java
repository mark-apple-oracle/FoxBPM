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
 */
package org.foxbpm.kernel.runtime.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.foxbpm.kernel.event.KernelEvent;
import org.foxbpm.kernel.process.KernelBaseElement;
import org.foxbpm.kernel.process.KernelException;
import org.foxbpm.kernel.process.KernelSequenceFlow;
import org.foxbpm.kernel.process.impl.KernelFlowNodeImpl;
import org.foxbpm.kernel.process.impl.KernelProcessDefinitionImpl;
import org.foxbpm.kernel.process.impl.KernelSequenceFlowImpl;
import org.foxbpm.kernel.runtime.FlowNodeExecutionContext;
import org.foxbpm.kernel.runtime.InterpretableExecutionContext;
import org.foxbpm.kernel.runtime.KernelToken;
import org.foxbpm.kernel.runtime.ListenerExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KernelTokenImpl implements FlowNodeExecutionContext, 
ListenerExecutionContext, 
KernelToken,
InterpretableExecutionContext  {
	
	private static Logger LOG = LoggerFactory.getLogger(KernelTokenImpl.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected KernelProcessInstanceImpl processInstance;

	protected boolean isEnded = false;

	protected boolean isActive = true;

	protected KernelFlowNodeImpl currentFlowNode;
	
	/**
	 * 需要跳转的节点
	 */
	protected KernelFlowNodeImpl toFlowNode;
	

	protected String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected KernelTokenImpl parent;

	protected boolean isLocked = false;

	protected boolean isSuspended = false;

	protected boolean isSubProcessRootToken = false;
	
	protected KernelSequenceFlowImpl sequenceFlow;

	/**
	 * 子令牌集合
	 */
	protected List<KernelTokenImpl> children = new ArrayList<KernelTokenImpl>();
	
	protected HashMap<String, KernelTokenImpl> namedChildren=new HashMap<String, KernelTokenImpl>();
	
	

	// 事件 ///////////////////////////////////////////////////////////////////
	protected String eventName;
	protected KernelBaseElement eventSource;
	protected int KernelListenerIndex = 0;
	protected KernelEvent nextEvent;
	protected boolean isOperating = false;

	public KernelFlowNodeImpl getFlowNode() {
		return currentFlowNode;
	}

	public void setFlowNode(KernelFlowNodeImpl flowNode) {
		this.currentFlowNode = flowNode;
	}

	public String getId() {
		return null;
	}

	public KernelProcessInstanceImpl getProcessInstance() {
		return processInstance;
	}

	public void setProcessInstance(KernelProcessInstanceImpl processInstance) {
		this.processInstance = processInstance;
	}
	
	public void enter(KernelFlowNodeImpl flowNode) {
		setFlowNode(flowNode);
		fireEvent(KernelEvent.NODE_ENTER);
		flowNode.getKernelFlowNodeBehavior().enter(this);
	}
	public void execute() {
		fireEvent(KernelEvent.NODE_EXECUTE);
		getFlowNode().getKernelFlowNodeBehavior().execute(this);
	}
	

	public void signal() {
		getFlowNode().getKernelFlowNodeBehavior().leave(this);
	}



	public void fireEvent(KernelEvent kernelEvent) {

		this.nextEvent = kernelEvent;
		if (!isOperating) {
			isOperating = true;
			while (nextEvent != null) {
				KernelEvent currentEvent = this.nextEvent;
				this.nextEvent = null;
				currentEvent.execute(this);
			}
			isOperating = false;
		}

	}

	public Integer getKernelListenerIndex() {
		return KernelListenerIndex;
	}

	public void setKernelListenerIndex(int kernelListenerIndex) {
		KernelListenerIndex = kernelListenerIndex;
	}
	

	
	public void leave() {
		

		// 发生节点离开事件
		fireEvent(KernelEvent.NODE_LEAVE);
		// 执行线条的进入方法
		getFlowNode().getKernelFlowNodeBehavior().cleanData(this);
		
		// kenshin  2013.1.2
		// 用来处理非线条流转令牌,如退回、跳转
	
		
		if(getToFlowNode()!=null){
			
			//发现上下文中有直接跳转节点,则流程引擎不走正常处理直接跳转到指定借点。
			
			
			LOG.debug("＝＝执行跳转机制,跳转目标: {}({}),离开节点: {}({}),令牌号: {}({}).",toFlowNode.getName(),toFlowNode.getId(), getFlowNode().getName(),getFlowNode().getId(),this.getName(),this.getId());

			setToFlowNode(null);
			enter(getToFlowNode());
			
			return;
		}
		
		
		//定义可通过线条集合
		List<KernelSequenceFlow> sequenceFlowList = new ArrayList<KernelSequenceFlow>();

		//获取正常离开的所有线条
		for (KernelSequenceFlow sequenceFlow : getFlowNode().getOutgoingSequenceFlows()) {
			//验证线条上的条件
			if (sequenceFlow.isContinue(this)) {
				sequenceFlowList.add(sequenceFlow);
			}

		}

		// 节点后面没有线的处理
		if (sequenceFlowList.size() == 0) {
			if (getFlowNode().getOutgoingSequenceFlows().size() == 0) {
				
				LOG.error("节点: {}({}) 后面没有配置处理线条！",getFlowNode().getName(),getFlowNode().getId());
				
				throw new KernelException("节点: "+getFlowNode().getName()+"("+getFlowNode().getId()+") 后面没有配置处理线条！");
				
			} else {
				
				LOG.error("节点: {}({}) 后面的条件都不满足导致节点后面没有处理线条,请检查后续线条条件！",getFlowNode().getName(),this.getId());
				
				throw new KernelException("节点: "+getFlowNode().getName()+"("+getFlowNode().getId()+") 后面的条件都不满足导致节点后面没有处理线条,请检查后续线条条件！");
			}
		}

		// 节点后面就一条线的处理
		if (sequenceFlowList.size() == 1) {
			take(sequenceFlowList.get(0));
			return;
		}

		// 节点后面大于一条线的处理
		if (sequenceFlowList.size() > 1) {

			// 创建分支令牌集合
			ArrayList<ForkedToken> forkedTokens = new ArrayList<ForkedToken>();

			
			//这里为什么做两个遍历,因为一定要在令牌都生成出来之后才能进行线条的take
			
			
			// 遍历满足条件线条
			for (KernelSequenceFlow sequenceFlow : sequenceFlowList) {
				// 获取线条名称
				String sequenceFlowId = sequenceFlow.getId();
				// 创建分支令牌并添加到集合中
				forkedTokens.add(this.createForkedToken(this, sequenceFlowId));
			}
			// 遍历分支令牌集合
			for (ForkedToken forkedToken : forkedTokens) {
				// 获取令牌
				KernelTokenImpl childToken = forkedToken.token;
				// 获取令牌编号
				String leavingSequenceFlowId = forkedToken.leavingSequenceFlowId;
	
				// 执行节点离开方法
				childToken.take(this.getFlowNode().findOutgoingSequenceFlow(leavingSequenceFlowId));

			}
		}
	}
	
	// 分支处理///////////////////////////////

	public ForkedToken createForkedToken(KernelTokenImpl parent, String sequenceFlowId) {
		// 创建一个令牌实例

		KernelTokenImpl childToken = getProcessInstance().createChildrenToken(parent);
		childToken.setName(sequenceFlowId);

		// 创建分支令牌
		ForkedToken forkedToken = null;
		forkedToken = new ForkedToken(childToken, sequenceFlowId);
		return forkedToken;
	}

	public static class ForkedToken {
		public KernelTokenImpl token = null;
		String leavingSequenceFlowId = null;

		public ForkedToken(KernelTokenImpl token, String leavingSequenceFlowId) {
			this.token = token;
			this.leavingSequenceFlowId = leavingSequenceFlowId;
		}
	}



	public void take(KernelSequenceFlow sequenceFlow) {

		sequenceFlow.take(this);
	}

	public void take(KernelFlowNodeImpl flowNode) {
		enter(flowNode);
	}

	public void end() {
		end(true);
	}

	public void end(boolean verifyParentTermination) {

		// 如果令牌已经有结束时间则不执行令牌结束方法
		if (!isEnded) {

			// 结束令牌.使他不能再启动父令牌
			isActive = false;
			// 结束日期的标志，表明此令牌已经结束。
			isEnded = true;
			// 结束所有子令牌
			if (getChildren() != null) {

				List<KernelTokenImpl> childrenTokens = getChildren();

				for (KernelTokenImpl childrenToken : childrenTokens) {
					if (!childrenToken.isEnded()) {
						childrenToken.end(false);
					}
				}
			}
			// 清理当前节点遗留信息
			getFlowNode().getKernelFlowNodeBehavior().cleanData(this);

			if (verifyParentTermination) {
				// 如果这是根令牌,则需要结束流程实例.
				notifyParentOfTokenEnd();
			}
		}
	}

	protected void notifyParentOfTokenEnd() {
		// 判断是否为根令牌
		if (isRoot()) {
			getProcessInstance().end();
		} else {
			if (getParent() != null) {
				// 下面这句话是用来当前存在分支时 一个分支走到结束另一个分支没结束的时候,不能结束他的父令牌
				// 只有当到达结束节点的令牌的同级分支都结束才能结束父令牌
				// 子流程多实例的时候 每个子流程结束的时候去触发验证完成条件
				if (isSignalParentToken()) {
					// 推动父令牌向后执行
					getParent().signal();

				}else{
					if (!getParent().hasActiveChildren()) {
						getParent().end();
					}
				}


			}
		}
	}

	/** 子类需要重写这个方法 */
	protected boolean isSignalParentToken() {

		return false;

	}

	public boolean hasActiveChildren() {
		boolean foundActiveChildToken = false;
		// 发现至少有一个子令牌,仍然活跃（没有结束.
		List<KernelTokenImpl> childrenTokens = getChildren();
		if (childrenTokens.size() > 0) {
			for (KernelTokenImpl childrenToken : childrenTokens) {
				if (childrenToken.isActive()) {
					foundActiveChildToken = true;
					return foundActiveChildToken;
				}
			}

		}
		return foundActiveChildToken;
	}

	/** 阻止令牌 */
	public void inactivate() {
		this.isActive = false;
	}

	public boolean isEnded() {
		return isEnded;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean isActive() {
		return isActive;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public KernelBaseElement getEventSource() {
		return eventSource;
	}

	public void setEventSource(KernelBaseElement eventSource) {
		this.eventSource = eventSource;
	}

	public KernelProcessDefinitionImpl getProcessDefinition() {
		return getProcessInstance().getProcessDefinition();
	}

	public boolean isRoot() {
		return (getParent() == null);
	}

	/** 子类需要重写这个方法从持久层拿父令牌 */
	public void ensureParentInitialized() {

	}

	public KernelTokenImpl getParent() {

		ensureParentInitialized();
		return parent;
	}

	public void setParent(KernelTokenImpl parent) {
		this.parent = parent;
	}

	public List<KernelTokenImpl> getChildren() {
		return children;
	}

	public void setChildren(List<KernelTokenImpl> children) {
		this.children = children;
	}

	public void addChild(KernelTokenImpl token) {

		if (token != null) {
			
			getChildren().add(token);
			
			if (token.getId() != null) {
				if (namedChildren.containsKey(token.getId())) {
					throw new KernelException("token "+token.getId()+"已经存在");
				}
				namedChildren.put(token.getId(), token);
			}
		}

	}

	public KernelSequenceFlowImpl getSequenceFlow() {
		return sequenceFlow;
	}

	public void setSequenceFlow(KernelSequenceFlowImpl sequenceFlow) {
		this.sequenceFlow=sequenceFlow;
	}


	public Object getProperty(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	public KernelFlowNodeImpl getToFlowNode() {
		return toFlowNode;
	}

	public void setToFlowNode(KernelFlowNodeImpl toFlowNode) {
		this.toFlowNode = toFlowNode;
	}


}
