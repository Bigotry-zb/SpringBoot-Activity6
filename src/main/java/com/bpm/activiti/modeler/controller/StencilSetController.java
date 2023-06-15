package com.bpm.activiti.modeler.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StencilSetController {

    protected static final Logger LOGGER = LoggerFactory.getLogger(StencilSetController.class);

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * 获取编辑器组件及配置项信息
     * @return
     */
    @RequestMapping(value="/app/rest/stencil-sets/editor", method= RequestMethod.GET, produces="application/json")
    public JsonNode getStencilSetForEditor() {
        try {
            //英文
            return this.objectMapper.readTree(getClass().getClassLoader().getResourceAsStream("static/editor-app/stencilsets/stencilset_bpmn.json"));
            //中文
            //return this.objectMapper.readTree(getClass().getClassLoader().getResourceAsStream("static/editor-app/stencilsets/stencilset_bpmn_cn.json"));
        } catch (Exception e) {
            this.LOGGER.error("Error reading bpmn stencil set json", e);
            throw new RuntimeException("Error reading bpmn stencil set json");
        }
    }

}
