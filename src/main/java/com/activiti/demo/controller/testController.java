package com.activiti.demo.controller;

import com.activiti.demo.deploy.Deploy;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lwt
 * @date 2018/7/31 13:52
 */
@RestController
public class testController {
    private final Deploy deploy;

    @Autowired
    public testController(Deploy deploy) {
        this.deploy = deploy;
    }

    @RequestMapping("deploy")
    public String deploy(){
        deploy.deploy();
        return "ok";
    }

    @RequestMapping("begin")
    public String begin(String key,String name){
        deploy.begin(key, name);
        return "ok";
    }

    @RequestMapping("excute")
    public String excute(String taskId, String auditId, Integer result, String userId){
        deploy.excute(taskId, auditId, result, userId);
        return "ok";
    }

    @RequestMapping("queryWait")
    public String queryWait(String name){
        List<Task> taskList = deploy.waitDo(name);
        return "ok";
    }

    @RequestMapping(value = "/image", method = RequestMethod.GET)
    public String image(HttpServletResponse response, @RequestParam String processInstanceId) {
        try {
            InputStream is = deploy.getDiagram(processInstanceId);
            if (is == null) {
                return "null";
            }
            response.setContentType("image/png");
            BufferedImage image = ImageIO.read(is);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "png", out);
            is.close();
            out.close();
        } catch (Exception ex) {

        }
        return "ok";
    }



    @RequestMapping("query")
    public String queryList(){
        deploy.queryList();
        return "ok";
    }
}
