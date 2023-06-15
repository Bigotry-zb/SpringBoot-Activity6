package com.bpm.activiti.modeler.controller;

import com.bpm.example.modeler.common.ResultListDataRepresentation;
import com.bpm.example.modeler.converter.BpmnDisplayJsonConverter;
import com.bpm.example.modeler.domain.Model;
import com.bpm.example.modeler.repository.ModelRepository;
import com.bpm.example.modeler.util.XmlUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.BpmnAutoLayout;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.editor.language.json.converter.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@RestController
public class ModelsController {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ModelsController.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected BpmnDisplayJsonConverter bpmnDisplayJsonConverter;

    /**
     * 查询流程模型列表
     * @param filter
     * @param sort
     * @param modelType
     * @param request
     * @return
     */
    @RequestMapping(value = "/rest/models", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResultListDataRepresentation getModels(@RequestParam(required=false) String filter, @RequestParam(required=false) String sort, @RequestParam(required=false) Integer modelType, HttpServletRequest request) {
        String filterText = null;
        List<NameValuePair> params = URLEncodedUtils.parse(request.getQueryString(), Charset.forName("UTF-8"));
        if (params != null) {
            for (NameValuePair nameValuePair : params) {
                if ("filterText".equalsIgnoreCase(nameValuePair.getName())) {
                    filterText = nameValuePair.getValue();
                }
            }
        }

        List<Model> models = null;
        String validFilter = makeValidFilterText(filterText);
        if (validFilter != null) {
            models = this.modelRepository.findModelsByModelType(modelType, validFilter);
        } else {
            models = this.modelRepository.findModelsByModelType(modelType);
        }

        ResultListDataRepresentation result = new ResultListDataRepresentation(models);
        return result;
    }

    /**
     * 创建流程模型
     * @param jsonMap
     * @return
     */
    @RequestMapping(value = "/rest/models", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public Model createModel(@RequestBody Map<String,String> jsonMap) {
        Model model = new Model();
        model.setKey(jsonMap.get("key"));
        model.setName(jsonMap.get("name"));
        model.setDescription(jsonMap.get("description"));
        model.setModelType(Integer.valueOf(jsonMap.get("modelType")));
        createObjectNode(model);
        model = (Model)this.modelRepository.save(model);
        return model;
    }

    /**
     * 获取流程模型缩列图
     * @param modelId
     * @return
     */
    @RequestMapping(value={"/rest/models/{modelId}/thumbnail"}, method=RequestMethod.GET, produces="image/png")
    @ResponseStatus(value = HttpStatus.OK)
    public byte[] getModelThumbnail(@PathVariable String modelId) {
        Model model = (Model)this.modelRepository.findOne(modelId);
        if (model != null) {
            return model.getThumbnail();
        }
        return null;
    }

    /**
     * 根据modelId查询流程模型
     * @param modelId
     * @return
     */
    @RequestMapping(value="/rest/models/{modelId}", method=RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public Model getModel(@PathVariable String modelId) {
        Model model = (Model)this.modelRepository.findOne(modelId);
        return model;
    }

    /**
     * 根据modelId查询流程模型的JSON内容
     * @param modelId
     * @return
     */
    @RequestMapping(value="/rest/models/{modelId}/model-json", method=RequestMethod.GET, produces="application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public JsonNode getModelJSON(@PathVariable String modelId) {
        ObjectNode displayNode = this.objectMapper.createObjectNode();
        Model model = this.modelRepository.findOne(modelId);
        this.bpmnDisplayJsonConverter.processProcessElements(model, displayNode, new GraphicInfo());
        return displayNode;
    }

    /**
     * 导入流程定义xml文件
     * @param request
     * @param file
     * @return
     */
    @RequestMapping(value="rest/import-process-model", method=RequestMethod.POST, produces="application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public Model importProcessModel(HttpServletRequest request, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName != null && (fileName.endsWith(".bpmn") || fileName.endsWith(".bpmn20.xml"))) {
            try {
                XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
                InputStreamReader xmlIn = new InputStreamReader(file.getInputStream(), "UTF-8");
                XMLStreamReader xtr = xif.createXMLStreamReader(xmlIn);
                BpmnXMLConverter bpmnXmlConverter = new BpmnXMLConverter();
                BpmnModel bpmnModel = bpmnXmlConverter.convertToBpmnModel(xtr);
                if (CollectionUtils.isEmpty(bpmnModel.getProcesses())) {
                    throw new RuntimeException("No process found in definition " + fileName);
                }

                if (bpmnModel.getLocationMap().size() == 0) {
                    BpmnAutoLayout bpmnLayout = new BpmnAutoLayout(bpmnModel);
                    bpmnLayout.execute();
                }

                BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
                ObjectNode modelNode = bpmnJsonConverter.convertToJson(bpmnModel);

                org.activiti.bpmn.model.Process process = bpmnModel.getMainProcess();
                String name = process.getId();
                if (StringUtils.isNotEmpty(process.getName())) {
                    name = process.getName();
                }
                String description = process.getDocumentation();

                Model model = new Model();
                model.setVersion(1);
                model.setKey(process.getId());
                model.setName(name);
                model.setDescription(description);
                model.setModelType(0);
                model.setCreated(Calendar.getInstance().getTime());
                model.setLastUpdated(Calendar.getInstance().getTime());
                model.setModelEditorJson(modelNode.toString());
                model = (Model)this.modelRepository.save(model);
                return model;
            } catch (Exception e) {
                throw new RuntimeException("Import failed for " + fileName + ", error message " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Invalid file name, only .bpmn and .bpmn20.xml files are supported not " + fileName);
        }
    }

    @RequestMapping(value = "/rest/import-process-model/text", method = RequestMethod.POST)
    public String importProcessModelText(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        Model model = importProcessModel(request, file);
        String modelRepresentationJson = null;
        try {
            modelRepresentationJson = objectMapper.writeValueAsString(model);
        } catch (Exception e) {
            throw new RuntimeException("Model Representation could not be saved");
        }
        return modelRepresentationJson;
    }

    private String makeValidFilterText(String filterText) {
        String validFilter = null;
        if (filterText != null) {
            String trimmed = StringUtils.trim(filterText);
            if (trimmed.length() >= 1) {
                validFilter = "%" + trimmed.toLowerCase() + "%";
            }
        }
        return validFilter;
    }

    private void createObjectNode(Model model) {
        ObjectNode editorNode = this.objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = this.objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.put("stencilset", stencilSetNode);
        ObjectNode propertiesNode = this.objectMapper.createObjectNode();
        propertiesNode.put("process_id", model.getKey());
        propertiesNode.put("name", model.getName());
        if (StringUtils.isNotEmpty(model.getDescription())) {
            propertiesNode.put("documentation", model.getDescription());
        }
        editorNode.put("properties", propertiesNode);

        ArrayNode childShapeArray = this.objectMapper.createArrayNode();
        editorNode.put("childShapes", childShapeArray);
        ObjectNode childNode = this.objectMapper.createObjectNode();
        childShapeArray.add(childNode);
        ObjectNode boundsNode = this.objectMapper.createObjectNode();
        childNode.put("bounds", boundsNode);
        ObjectNode lowerRightNode = this.objectMapper.createObjectNode();
        boundsNode.put("lowerRight", lowerRightNode);
        lowerRightNode.put("x", 130);
        lowerRightNode.put("y", 193);
        ObjectNode upperLeftNode = this.objectMapper.createObjectNode();
        boundsNode.put("upperLeft", upperLeftNode);
        upperLeftNode.put("x", 100);
        upperLeftNode.put("y", 163);
        childNode.put("childShapes", this.objectMapper.createArrayNode());
        childNode.put("dockers", this.objectMapper.createArrayNode());
        childNode.put("outgoing", this.objectMapper.createArrayNode());
        childNode.put("resourceId", "startEvent1");
        ObjectNode stencilNode = this.objectMapper.createObjectNode();
        childNode.put("stencil", stencilNode);
        stencilNode.put("id", "StartNoneEvent");
        String json = editorNode.toString();
        model.setModelEditorJson(json);
    }

}