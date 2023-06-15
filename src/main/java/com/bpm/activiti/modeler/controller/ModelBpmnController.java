package com.bpm.activiti.modeler.controller;

import com.bpm.example.modeler.domain.Model;
import com.bpm.example.modeler.repository.ModelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ModelBpmnController {

    private static final Logger logger = LoggerFactory.getLogger(ModelBpmnController.class);

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * GET /rest/models/{modelId}/bpmn -> Get BPMN 2.0 xml
     */
    @RequestMapping(value = "/rest/models/{processModelId}/bpmn20", method = RequestMethod.GET)
    public void getProcessModelBpmn20Xml(HttpServletResponse response, @PathVariable String processModelId) throws IOException {
        if (processModelId == null) {
            throw new RuntimeException("No process model id provided");
        }

        Model model = (Model)this.modelRepository.findOne(processModelId);
        generateBpmn20Xml(response, model);
    }

    protected void generateBpmn20Xml(HttpServletResponse response, Model model) {
        String name = model.getName().replaceAll(" ", "_");
        response.setHeader("Content-Disposition", "attachment; filename=" + name + ".bpmn20.xml");
        if (model.getModelEditorJson() != null) {
            try {
                ServletOutputStream servletOutputStream = response.getOutputStream();
                response.setContentType("application/xml");

                BpmnModel bpmnModel = getBpmnModel(model);
                byte[] xmlBytes = getBpmnXML(bpmnModel);
                BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(xmlBytes));

                byte[] buffer = new byte[8096];
                while (true) {
                    int count = in.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    servletOutputStream.write(buffer, 0, count);
                }

                // Flush and close stream
                servletOutputStream.flush();
                servletOutputStream.close();

            } catch (Exception e) {
                throw new RuntimeException("Could not generate BPMN 2.0 xml");
            }
        }
    }

    private BpmnModel getBpmnModel(Model model) {
        BpmnModel bpmnModel = null;
        try {
            Map<String, Model> formMap = new HashMap<String, Model>();
            Map<String, Model> decisionTableMap = new HashMap<String, Model>();

            List<Model> referencedModels = modelRepository.findModelsByParentModelId(model.getId());
            for (Model childModel : referencedModels) {
                if (Model.MODEL_TYPE_FORM == childModel.getModelType()) {
                    formMap.put(childModel.getId(), childModel);

                } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
                    decisionTableMap.put(childModel.getId(), childModel);
                }
            }

            bpmnModel = getBpmnModel(model, formMap, decisionTableMap);

        } catch (Exception e) {
            throw new RuntimeException("Could not generate BPMN 2.0 model");
        }

        return bpmnModel;
    }

    private BpmnModel getBpmnModel(Model model, Map<String, Model> formMap, Map<String, Model> decisionTableMap) {
        try {
            ObjectNode editorJsonNode = (ObjectNode) objectMapper.readTree(model.getModelEditorJson());
            Map<String, String> formKeyMap = new HashMap<String, String>();
            for (Model formModel : formMap.values()) {
                formKeyMap.put(formModel.getId(), formModel.getKey());
            }

            Map<String, String> decisionTableKeyMap = new HashMap<String, String>();
            for (Model decisionTableModel : decisionTableMap.values()) {
                decisionTableKeyMap.put(decisionTableModel.getId(), decisionTableModel.getKey());
            }
            BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
            return bpmnJsonConverter.convertToBpmnModel(editorJsonNode, formKeyMap, decisionTableKeyMap);

        } catch (Exception e) {
            throw new RuntimeException("Could not generate BPMN 2.0 model");
        }
    }

    public byte[] getBpmnXML(Model model) {
        BpmnModel bpmnModel = getBpmnModel(model);
        return getBpmnXML(bpmnModel);
    }

    public byte[] getBpmnXML(BpmnModel bpmnModel) {
        for (Process process : bpmnModel.getProcesses()) {
            if (StringUtils.isNotEmpty(process.getId())) {
                char firstCharacter = process.getId().charAt(0);
                // no digit is allowed as first character
                if (Character.isDigit(firstCharacter)) {
                    process.setId("a" + process.getId());
                }
            }
        }
        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
        byte[] xmlBytes = bpmnXMLConverter.convertToXML(bpmnModel);
        return xmlBytes;
    }
}