package org.activiti.engine.test.logging.mdc;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class TestService implements JavaDelegate{
	static String processInstanceId = null;
	static String processDefinitionId = null;
	static String executionId = null;
	static String businessKey = null;
	

	@Override
	public void execute(DelegateExecution execution) {
		TestService.processDefinitionId = execution.getProcessDefinitionId();
		TestService.processInstanceId =  execution.getProcessInstanceId();
		TestService.executionId = execution.getId();
		TestService.businessKey = execution.getProcessInstanceBusinessKey();
		
		throw new RuntimeException("test");
	}
	
	public void clearProcessVariables() {
		processDefinitionId = null;
		processInstanceId = null;
		executionId = null;
		businessKey = null;
	}

}
