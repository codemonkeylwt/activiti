package com.activiti.demo.deploy;

import org.activiti.engine.task.Task;

import java.io.InputStream;
import java.util.List;

/**
 * @author lwt
 * @date 2018/7/31 13:54
 */
public interface Deploy {
    void deploy();

    void queryList();

    void begin(String key,String name);

    List<Task> waitDo(String name);

    void excute(String taskId, String auditId, Integer result, String userId);

    InputStream getDiagram(String processInstanceId);
}
