package com.bpm.activiti.modeler.controller;

import com.bpm.example.modeler.domain.Model;
import com.bpm.example.modeler.repository.ModelRepository;
import com.bpm.example.modeler.service.ModelImageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
public class ModelController {

    private static final Logger logger = LoggerFactory.getLogger(ModelController.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected ModelImageService modelImageService;

    /**
     * 根据modelId查询模型信息
     * @param modelId
     * @return
     */
    @RequestMapping(value="/rest/model/{modelId}/json", method=RequestMethod.GET, produces="application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ObjectNode getModelJSON(@PathVariable String modelId) {
        Model model = (Model)this.modelRepository.findOne(modelId);
        ObjectNode modelNode = this.objectMapper.createObjectNode();
        modelNode.put("modelId", model.getId());
        modelNode.put("name", model.getName());
        modelNode.put("key", model.getKey());
        modelNode.put("description", model.getDescription());
        modelNode.putPOJO("lastUpdated", model.getLastUpdated());
        modelNode.put("lastUpdatedBy", model.getLastUpdatedBy());
        if (StringUtils.isNotEmpty(model.getModelEditorJson())) {
            try {
                ObjectNode editorJsonNode = (ObjectNode)this.objectMapper.readTree(model.getModelEditorJson());
                editorJsonNode.put("modelType", "model");
                modelNode.put("model", editorJsonNode);
            } catch (Exception e) {
                throw new RuntimeException("Error reading editor json " + modelId);
            }
        } else {
            ObjectNode editorJsonNode = this.objectMapper.createObjectNode();
            editorJsonNode.put("id", "canvas");
            editorJsonNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = this.objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
            editorJsonNode.put("modelType", "model");
            modelNode.put("model", editorJsonNode);
        }
        return modelNode;
    }

    /**
     * 保存流程模型
     * @param modelId
     * @param values
     * @return
     */
    @RequestMapping(value="/rest/model/{modelId}/editor/json", method=RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public Model saveModel(@PathVariable String modelId, @RequestBody MultiValueMap<String, String> values) {
        Model model = (Model)modelRepository.findOne(modelId);
        String name = (String)values.getFirst("name");
        String key = (String)values.getFirst("key");
        String description = (String)values.getFirst("description");
        String isNewVersionString = (String)values.getFirst("newversion");
        String newVersionComment = (String)values.getFirst("comment");
        String json = (String)values.getFirst("json_xml");

        model.setLastUpdated(new Date());
        model.setName(name);
        model.setKey(key);
        model.setDescription(description);
        model.setModelEditorJson(json);
        model.setVersion(1);
        ObjectNode jsonNode = null;
        try {
            jsonNode = (ObjectNode)this.objectMapper.readTree(model.getModelEditorJson());
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize json model");
        }

        modelImageService.generateThumbnailImage(model, jsonNode);
        modelRepository.save(model);
        return model;
    }
}