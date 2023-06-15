package com.bpm.activiti.modeler.converter;

import com.bpm.example.modeler.domain.Model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.*;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.editor.language.json.converter.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class BpmnDisplayJsonConverter
{

    private static final Logger logger = LoggerFactory.getLogger(BpmnDisplayJsonConverter.class);

    protected BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
    protected ObjectMapper objectMapper = new ObjectMapper();
    protected List<String> eventElementTypes = new ArrayList();

    public BpmnDisplayJsonConverter()
    {
        this.eventElementTypes.add("StartEvent");
        this.eventElementTypes.add("EndEvent");
        this.eventElementTypes.add("BoundaryEvent");
        this.eventElementTypes.add("IntermediateCatchEvent");
        this.eventElementTypes.add("ThrowEvent");

    }

    public void processProcessElements(Model processModel, ObjectNode displayNode, GraphicInfo diagramInfo)
    {
        BpmnModel pojoModel = null;
        if (!StringUtils.isEmpty(processModel.getModelEditorJson())) {
            try
            {
                JsonNode modelNode = this.objectMapper.readTree(processModel.getModelEditorJson());
                pojoModel = this.bpmnJsonConverter.convertToBpmnModel(modelNode);
            }
            catch (Exception e)
            {
                this.logger.error("Error transforming json to pojo " + processModel.getId(), e);
            }
        }
        if ((pojoModel == null) || (pojoModel.getLocationMap().isEmpty())) {
            return;
        }
        ArrayNode elementArray = this.objectMapper.createArrayNode();
        ArrayNode flowArray = this.objectMapper.createArrayNode();
        if (CollectionUtils.isNotEmpty(pojoModel.getPools()))
        {
            ArrayNode poolArray = this.objectMapper.createArrayNode();
            boolean firstElement = true;
            for (Pool pool : pojoModel.getPools())
            {
                ObjectNode poolNode = this.objectMapper.createObjectNode();
                poolNode.put("id", pool.getId());
                poolNode.put("name", pool.getName());
                GraphicInfo poolInfo = pojoModel.getGraphicInfo(pool.getId());
                fillGraphicInfo(poolNode, poolInfo, true);
                Process process = pojoModel.getProcess(pool.getId());
                if ((process != null) && (CollectionUtils.isNotEmpty(process.getLanes())))
                {
                    ArrayNode laneArray = this.objectMapper.createArrayNode();
                    for (Lane lane : process.getLanes())
                    {
                        ObjectNode laneNode = this.objectMapper.createObjectNode();
                        laneNode.put("id", lane.getId());
                        laneNode.put("name", lane.getName());
                        fillGraphicInfo(laneNode, pojoModel.getGraphicInfo(lane.getId()), true);
                        laneArray.add(laneNode);
                    }
                    poolNode.put("lanes", laneArray);
                }
                poolArray.add(poolNode);

                double rightX = poolInfo.getX() + poolInfo.getWidth();
                double bottomY = poolInfo.getY() + poolInfo.getHeight();
                double middleX = poolInfo.getX() + poolInfo.getWidth() / 2.0D;
                if ((firstElement) || (middleX < diagramInfo.getX())) {
                    diagramInfo.setX(middleX);
                }
                if ((firstElement) || (poolInfo.getY() < diagramInfo.getY())) {
                    diagramInfo.setY(poolInfo.getY());
                }
                if (rightX > diagramInfo.getWidth()) {
                    diagramInfo.setWidth(rightX);
                }
                if (bottomY > diagramInfo.getHeight()) {
                    diagramInfo.setHeight(bottomY);
                }
                firstElement = false;
            }
            displayNode.put("pools", poolArray);
        }
        else
        {
            diagramInfo.setX(9999.0D);
            diagramInfo.setY(1000.0D);
        }
        for (Process process : pojoModel.getProcesses())
        {
            processElements(process.getFlowElements(), pojoModel, elementArray, flowArray, diagramInfo);
            processArtifacts(process.getArtifacts(), pojoModel, elementArray, flowArray, diagramInfo);
        }
        displayNode.put("elements", elementArray);
        displayNode.put("flows", flowArray);

        displayNode.put("diagramBeginX", diagramInfo.getX());
        displayNode.put("diagramBeginY", diagramInfo.getY());
        displayNode.put("diagramWidth", diagramInfo.getWidth());
        displayNode.put("diagramHeight", diagramInfo.getHeight());
    }

    protected void processElements(Collection<FlowElement> elementList, BpmnModel model, ArrayNode elementArray, ArrayNode flowArray, GraphicInfo diagramInfo)
    {
        for (FlowElement element : elementList) {
            if ((element instanceof SequenceFlow))
            {
                ObjectNode elementNode = this.objectMapper.createObjectNode();
                SequenceFlow flow = (SequenceFlow)element;
                elementNode.put("id", flow.getId());
                elementNode.put("type", "sequenceFlow");
                elementNode.put("sourceRef", flow.getSourceRef());
                elementNode.put("targetRef", flow.getTargetRef());
                List<GraphicInfo> flowInfo = model.getFlowLocationGraphicInfo(flow.getId());
                if (CollectionUtils.isNotEmpty(flowInfo))
                {
                    ArrayNode waypointArray = this.objectMapper.createArrayNode();
                    for (GraphicInfo graphicInfo : flowInfo)
                    {
                        ObjectNode pointNode = this.objectMapper.createObjectNode();
                        fillGraphicInfo(pointNode, graphicInfo, false);
                        waypointArray.add(pointNode);
                        fillDiagramInfo(graphicInfo, diagramInfo);
                    }
                    elementNode.put("waypoints", waypointArray);

                    String className = element.getClass().getSimpleName();

                    flowArray.add(elementNode);
                }
            }
            else
            {
                ObjectNode elementNode = this.objectMapper.createObjectNode();
                elementNode.put("id", element.getId());
                elementNode.put("name", element.getName());

                GraphicInfo graphicInfo = model.getGraphicInfo(element.getId());
                if (graphicInfo != null) {
                    fillGraphicInfo(elementNode, graphicInfo, true);
                    fillDiagramInfo(graphicInfo, diagramInfo);
                }
                String className = element.getClass().getSimpleName();
                elementNode.put("type", className);
                fillEventTypes(className, element, elementNode);
                if ((element instanceof ServiceTask)) {
                    ServiceTask serviceTask = (ServiceTask)element;
                    if ("mail".equals(serviceTask.getType())) {
                        elementNode.put("taskType", "mail");
                    } else if ("camel".equals(serviceTask.getType())) {
                        elementNode.put("taskType", "camel");
                    } else if ("mule".equals(serviceTask.getType())) {
                        elementNode.put("taskType", "mule");
                    }
                }

                elementArray.add(elementNode);
                if ((element instanceof SubProcess)) {
                    SubProcess subProcess = (SubProcess)element;
                    processElements(subProcess.getFlowElements(), model, elementArray, flowArray, diagramInfo);
                    processArtifacts(subProcess.getArtifacts(), model, elementArray, flowArray, diagramInfo);
                }
            }
        }
    }

    protected void processArtifacts(Collection<Artifact> artifactList, BpmnModel model, ArrayNode elementArray, ArrayNode flowArray, GraphicInfo diagramInfo) {
        for (Artifact artifact : artifactList) {
            if ((artifact instanceof Association))
            {
                ObjectNode elementNode = this.objectMapper.createObjectNode();
                Association flow = (Association)artifact;
                elementNode.put("id", flow.getId());
                elementNode.put("type", "association");
                elementNode.put("sourceRef", flow.getSourceRef());
                elementNode.put("targetRef", flow.getTargetRef());
                fillWaypoints(flow.getId(), model, elementNode, diagramInfo);
                flowArray.add(elementNode);
            }
            else
            {
                ObjectNode elementNode = this.objectMapper.createObjectNode();
                elementNode.put("id", artifact.getId());
                if ((artifact instanceof TextAnnotation))
                {
                    TextAnnotation annotation = (TextAnnotation)artifact;
                    elementNode.put("text", annotation.getText());
                }
                GraphicInfo graphicInfo = model.getGraphicInfo(artifact.getId());
                if (graphicInfo != null)
                {
                    fillGraphicInfo(elementNode, graphicInfo, true);
                    fillDiagramInfo(graphicInfo, diagramInfo);
                }
                String className = artifact.getClass().getSimpleName();
                elementNode.put("type", className);

                elementArray.add(elementNode);
            }
        }
    }

    protected void fillWaypoints(String id, BpmnModel model, ObjectNode elementNode, GraphicInfo diagramInfo) {
        List<GraphicInfo> flowInfo = model.getFlowLocationGraphicInfo(id);
        if (flowInfo != null) {
            ArrayNode waypointArray = this.objectMapper.createArrayNode();
            for (GraphicInfo graphicInfo : flowInfo) {
                ObjectNode pointNode = this.objectMapper.createObjectNode();
                fillGraphicInfo(pointNode, graphicInfo, false);
                waypointArray.add(pointNode);
                fillDiagramInfo(graphicInfo, diagramInfo);
            }
            elementNode.put("waypoints", waypointArray);
        }
    }

    protected void fillEventTypes(String className, FlowElement element, ObjectNode elementNode)
    {
        if (this.eventElementTypes.contains(className))
        {
            Event event = (Event)element;
            if (CollectionUtils.isNotEmpty(event.getEventDefinitions()))
            {
                EventDefinition eventDef = (EventDefinition)event.getEventDefinitions().get(0);
                ObjectNode eventNode = this.objectMapper.createObjectNode();
                if ((eventDef instanceof TimerEventDefinition))
                {
                    TimerEventDefinition timerDef = (TimerEventDefinition)eventDef;
                    eventNode.put("type", "timer");
                    if (StringUtils.isNotEmpty(timerDef.getTimeCycle())) {
                        eventNode.put("timeCycle", timerDef.getTimeCycle());
                    }
                    if (StringUtils.isNotEmpty(timerDef.getTimeDate())) {
                        eventNode.put("timeDate", timerDef.getTimeDate());
                    }
                    if (StringUtils.isNotEmpty(timerDef.getTimeDuration())) {
                        eventNode.put("timeDuration", timerDef.getTimeDuration());
                    }
                }
                else if ((eventDef instanceof ErrorEventDefinition))
                {
                    ErrorEventDefinition errorDef = (ErrorEventDefinition)eventDef;
                    eventNode.put("type", "error");
                    if (StringUtils.isNotEmpty(errorDef.getErrorCode())) {
                        eventNode.put("errorCode", errorDef.getErrorCode());
                    }
                }
                else if ((eventDef instanceof SignalEventDefinition))
                {
                    SignalEventDefinition signalDef = (SignalEventDefinition)eventDef;
                    eventNode.put("type", "signal");
                    if (StringUtils.isNotEmpty(signalDef.getSignalRef())) {
                        eventNode.put("signalRef", signalDef.getSignalRef());
                    }
                }
                else if ((eventDef instanceof MessageEventDefinition))
                {
                    MessageEventDefinition messageDef = (MessageEventDefinition)eventDef;
                    eventNode.put("type", "message");
                    if (StringUtils.isNotEmpty(messageDef.getMessageRef())) {
                        eventNode.put("messageRef", messageDef.getMessageRef());
                    }
                }
                elementNode.put("eventDefinition", eventNode);
            }
        }
    }

    protected void fillGraphicInfo(ObjectNode elementNode, GraphicInfo graphicInfo, boolean includeWidthAndHeight)
    {
        commonFillGraphicInfo(elementNode, graphicInfo.getX(), graphicInfo.getY(), graphicInfo.getWidth(), graphicInfo.getHeight(), includeWidthAndHeight);
    }

    protected void commonFillGraphicInfo(ObjectNode elementNode, double x, double y, double width, double height, boolean includeWidthAndHeight)
    {
        elementNode.put("x", x);
        elementNode.put("y", y);
        if (includeWidthAndHeight)
        {
            elementNode.put("width", width);
            elementNode.put("height", height);
        }
    }

    protected void fillDiagramInfo(GraphicInfo graphicInfo, GraphicInfo diagramInfo)
    {
        double rightX = graphicInfo.getX() + graphicInfo.getWidth();
        double bottomY = graphicInfo.getY() + graphicInfo.getHeight();
        double middleX = graphicInfo.getX() + graphicInfo.getWidth() / 2.0D;
        if (middleX < diagramInfo.getX()) {
            diagramInfo.setX(middleX);
        }
        if (graphicInfo.getY() < diagramInfo.getY()) {
            diagramInfo.setY(graphicInfo.getY());
        }
        if (rightX > diagramInfo.getWidth()) {
            diagramInfo.setWidth(rightX);
        }
        if (bottomY > diagramInfo.getHeight()) {
            diagramInfo.setHeight(bottomY);
        }
    }
}
