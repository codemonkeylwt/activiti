package com.activiti.demo.deploy;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * @author lwt
 * @date 2018/7/31 13:50
 */
@Service
public class DeployTest implements Deploy {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final IdentityService identityService;
    @Autowired
    ProcessEngine processEngine;

    @Autowired
    public DeployTest(RepositoryService repositoryService, RuntimeService runtimeService, TaskService taskService, HistoryService historyService, IdentityService identityService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.identityService = identityService;
    }

    /**
     * 部署流程定义——即把请假规则应用到数据库里面去
     */
    @Override
    public void deploy() {

        // 创建一个部署构建器对象，用于加载流程定义文件(bpmn文件和png文件)
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("processes/qjlc.bpmn20.xml");
        deploymentBuilder.addClasspathResource("processes/qjlc.bpmn20.png");
        // 部署，并返回一个部署对象(其实Deployment是一个接口)
        Deployment deployment = deploymentBuilder.deploy();
        System.out.println(deployment.getId());
    }

    @Override
        public void queryList() {
        // 流程定义查询对象，用于查询流程定义表（act_re_procdef）
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        // 以下查询的是所有的流程定义
        List<ProcessDefinition> list = query.list();
        for (ProcessDefinition pd : list) {
            System.out.println(pd.getId() + "    " + pd.getName() + "    " + pd.getVersion());
        }
    }

    @Override
    public void begin(String key,String name) {
        //启动流程实例，字符串"vacation"是BPMN模型文件里process元素的id
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key);
        //流程实例启动后，流程会跳转到请假申请节点
        Task vacationApply = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        //设置请假申请任务的执行人
        taskService.setAssignee(vacationApply.getId(), name);
        //设置流程参数：请假天数和表单ID  流程引擎会根据请假天数days>3判断流程走向  formId是用来将流程数据和表单数据关联起来
        Map<String, Object> args = new HashMap<>();
        args.put("day", 3);
        args.put("formId", 1);
        //完成请假申请任务
        taskService.complete(vacationApply.getId(), args);
    }

    @Override
    public List<Task> waitDo(String name) {
        // 任务查询对象，操作的是任务表(act_ru_task)
        TaskQuery query = taskService.createTaskQuery();
        // 根据任务的办理人过滤
        query.taskAssignee(name);
        List<Task> list = query.list();
        for (Task task : list) {
            System.out.println(task.getId() + "\t" + task.getName() + "\t" + task.getAssignee());
        }
        return list;
    }

    @Override
    public void excute(String taskId, String auditId, Integer result, String userId) {
        //查询当前审批节点
        Task vacationAudit = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (result == 1) {
            //审批通过
            Map<String, Object> args = new HashMap<>();
            //设置流程参数：审批ID
            args.put("auditId", auditId);
            //设置审批任务的执行人
            taskService.claim(vacationAudit.getId(), userId);
            //完成审批任务
            taskService.complete(vacationAudit.getId(), args);
        } else {
            //审批不通过，结束流程
            runtimeService.deleteProcessInstance(vacationAudit.getProcessInstanceId(), auditId);
        }
    }

    @Override
    public InputStream getDiagram(String processInstanceId) {
        //获得流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        String processDefinitionId;
        if (processInstance == null) {
            //查询已经结束的流程实例
            HistoricProcessInstance processInstanceHistory =historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            if (processInstanceHistory == null) {
                return null;
            }
            else {
                processDefinitionId = processInstanceHistory.getProcessDefinitionId();
            }
        } else {
            processDefinitionId = processInstance.getProcessDefinitionId();
        }
        //使用宋体
        String fontName = "宋体";
        //获取BPMN模型对象
        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        //获取流程实例当前的节点，需要高亮显示
        List<String> currentActs = Collections.EMPTY_LIST;
        if (processInstance != null) {
            currentActs = runtimeService.getActiveActivityIds(processInstance.getId());
        }
        return processEngine.getProcessEngineConfiguration().getProcessDiagramGenerator()
                .generateDiagram(model, "png", currentActs, new ArrayList<>(),fontName, fontName, fontName, null, 1.0);
    }
}
