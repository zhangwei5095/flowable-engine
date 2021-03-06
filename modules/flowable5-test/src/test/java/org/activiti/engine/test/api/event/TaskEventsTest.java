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
package org.activiti.engine.test.api.event;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.impl.delegate.event.FlowableEngineEntityEvent;
import org.flowable.engine.impl.delegate.event.FlowableEngineEvent;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;
import org.flowable.engine.test.Deployment;

/**
 * Test case for all {@link FlowableEvent}s related to tasks.
 * 
 * @author Frederik Heremans
 */
public class TaskEventsTest extends PluggableFlowableTestCase {

	private TestFlowableEntityEventListener listener;
	
	/**
	 * Check create, update and delete events for a task. 
	 */
	@Deployment(resources= {"org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
	public void testTaskEventsInProcess() throws Exception {
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
		assertNotNull(processInstance);
		
		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		assertNotNull(task);
		
		// Check create event
		assertEquals(3, listener.getEventsReceived().size());
		FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
		assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
		assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);
		org.activiti.engine.task.Task taskFromEvent = (org.activiti.engine.task.Task) event.getEntity(); 
		assertEquals(task.getId(), taskFromEvent.getId());
		assertExecutionDetails(event, processInstance);
		
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
		assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());

    event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
    assertEquals(FlowableEngineEventType.TASK_CREATED, event.getType());
    assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);
    taskFromEvent = (org.activiti.engine.task.Task) event.getEntity();
    assertEquals(task.getId(), taskFromEvent.getId());
    assertExecutionDetails(event, processInstance);

    listener.clearEventsReceived();

		// Update duedate, owner and priority should trigger update-event
		taskService.setDueDate(task.getId(), new Date());
		assertEquals(1, listener.getEventsReceived().size());
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
		assertExecutionDetails(event, processInstance);
		assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
		listener.clearEventsReceived();
		
		taskService.setPriority(task.getId(), 12);
		assertEquals(1, listener.getEventsReceived().size());
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
		assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
		assertExecutionDetails(event, processInstance);
		listener.clearEventsReceived();
		
		taskService.setOwner(task.getId(), "kermit");
		assertEquals(1, listener.getEventsReceived().size());
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
		assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
		assertExecutionDetails(event, processInstance);
		listener.clearEventsReceived();
		
		// Updating detached task and calling saveTask should trigger a single update-event
		task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		
		task.setDueDate(new Date());
		task.setOwner("john");
		taskService.saveTask(task);
		
		assertEquals(1, listener.getEventsReceived().size());
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
		assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
		assertExecutionDetails(event, processInstance);
		listener.clearEventsReceived();
		
		// Check delete-event on complete
		taskService.complete(task.getId());
		assertEquals(2, listener.getEventsReceived().size());
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
		assertEquals(FlowableEngineEventType.TASK_COMPLETED, event.getType());
		assertExecutionDetails(event, processInstance);
		org.activiti.engine.impl.persistence.entity.TaskEntity taskEntity = (org.activiti.engine.impl.persistence.entity.TaskEntity) event.getEntity();
		assertNotNull(taskEntity.getDueDate());
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
		assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
		assertExecutionDetails(event, processInstance);
	}
	
	@Deployment(resources= {"org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
	public void testTaskAssignmentEventInProcess() throws Exception {
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
		assertNotNull(processInstance);
		listener.clearEventsReceived();
		
		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		assertNotNull(task);
		
		// Set assignee through API
		taskService.setAssignee(task.getId(), "kermit");
		assertEquals(2, listener.getEventsReceived().size());
		FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
		assertEquals(FlowableEngineEventType.TASK_ASSIGNED, event.getType());
		assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);
		org.activiti.engine.task.Task taskFromEvent = (org.activiti.engine.task.Task) event.getEntity();
		assertEquals(task.getId(), taskFromEvent.getId());
		assertEquals("kermit", taskFromEvent.getAssignee());
		assertExecutionDetails(event, processInstance);
		
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
		assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
		assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);
		assertExecutionDetails(event, processInstance);
		listener.clearEventsReceived();
		
		// Set assignee through updateTask
		task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		task.setAssignee("newAssignee");
		taskService.saveTask(task);
		
		assertEquals(2, listener.getEventsReceived().size());
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
		assertEquals(FlowableEngineEventType.TASK_ASSIGNED, event.getType());
		assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);
		taskFromEvent = (org.activiti.engine.task.Task) event.getEntity();
		assertEquals(task.getId(), taskFromEvent.getId());
		assertEquals("newAssignee", taskFromEvent.getAssignee());
		assertExecutionDetails(event, processInstance);
		
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
		assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
		assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);
		assertExecutionDetails(event, processInstance);
		listener.clearEventsReceived();
		
		// Unclaim 
		taskService.unclaim(task.getId());
		assertEquals(2, listener.getEventsReceived().size());
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
		assertEquals(FlowableEngineEventType.TASK_ASSIGNED, event.getType());
		assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);
		taskFromEvent = (org.activiti.engine.task.Task) event.getEntity();
		assertEquals(task.getId(), taskFromEvent.getId());
		assertEquals(null, taskFromEvent.getAssignee());
		assertExecutionDetails(event, processInstance);
		
		event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
		assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
		assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);
		assertExecutionDetails(event, processInstance);
		listener.clearEventsReceived();
	}
	
	/**
	 * Check events related to process instance delete and standalone task delete. 
	 */
	@Deployment(resources= {"org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
	public void testDeleteEventDoesNotDispathComplete() throws Exception {
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
		assertNotNull(processInstance);
		listener.clearEventsReceived();
		
		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		assertNotNull(task);
		
		// Delete process, should delete task as well, but not complete
		runtimeService.deleteProcessInstance(processInstance.getId(), "testing task delete events");
		
		assertEquals(1, listener.getEventsReceived().size());
		FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
		assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
		assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);
		org.activiti.engine.task.Task taskFromEvent = (org.activiti.engine.task.Task) event.getEntity();
		assertEquals(task.getId(), taskFromEvent.getId());
		assertExecutionDetails(event, processInstance);
	}
	
	/**
   * This method checks to ensure that the task.fireEvent(TaskListener.EVENTNAME_CREATE), fires before
   * the dispatchEvent ActivitiEventType.TASK_CREATED.  A ScriptTaskListener updates the priority and
   * assignee before the dispatchEvent() takes place.
     */
  @Deployment(resources= {"org/activiti/engine/test/api/event/TaskEventsTest.testEventFiring.bpmn20.xml"})
  public void testEventFiringOrdering() {
    
    // We need to add a special listener that copies the Task values - to record its state when the event fires,
    //otherwise the in-memory task instances is changed after the event fires.
    TestFlowableEntityEventTaskListener tlistener = new TestFlowableEntityEventTaskListener(org.activiti.engine.task.Task.class);
    processEngineConfiguration.getEventDispatcher().addEventListener(tlistener);

    try {

      runtimeService.startProcessInstanceByKey("testTaskLocalVars");

      // Fetch first task
      Task task = taskService.createTaskQuery().singleResult();

      // Complete first task
      Map<String, Object> taskParams = new HashMap<String, Object>();
      taskService.complete(task.getId(), taskParams, true);

      FlowableEntityEvent event = (FlowableEntityEvent) tlistener.getEventsReceived().get(0);
      assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
      assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);

      event = (FlowableEntityEvent) tlistener.getEventsReceived().get(1);
      assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
      assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);

      event = (FlowableEntityEvent) tlistener.getEventsReceived().get(2);
      assertEquals(FlowableEngineEventType.TASK_CREATED, event.getType());
      assertTrue(event.getEntity() instanceof org.activiti.engine.task.Task);
      org.activiti.engine.task.Task taskFromEvent = tlistener.getTasks().get(2);
      assertEquals(task.getId(), taskFromEvent.getId());

      // verify script listener has done its job, on create before ActivitiEntityEvent was fired
      assertEquals("The ScriptTaskListener must set this value before the dispatchEvent fires.","scriptedAssignee", taskFromEvent.getAssignee());
      assertEquals("The ScriptTaskListener must set this value before the dispatchEvent fires.", 877, taskFromEvent.getPriority());

    } finally {
      processEngineConfiguration.getEventDispatcher().removeEventListener(tlistener);
    }
  }
	
	protected void assertExecutionDetails(FlowableEngineEvent event, ProcessInstance processInstance) {
		assertEquals(processInstance.getId(), event.getProcessInstanceId());
		assertEquals(processInstance.getId(), event.getExecutionId());
		assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
	}
	
	@Override
	protected void setUp() throws Exception {
	  super.setUp();
	  listener = new TestFlowableEntityEventListener(org.activiti.engine.task.Task.class);
	  processEngineConfiguration.getEventDispatcher().addEventListener(listener);
	}
	
	@Override
	protected void tearDown() throws Exception {
	  super.tearDown();
	  
	  if (listener != null) {
	    processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
	  }
	}
}
