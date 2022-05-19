package com.swissre.alfrm.webscripts;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.swissre.alfrm.constants.CommonConstants;
import com.swissre.alfrm.constants.CommonConstants2;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.io.*;

public class CreateIndexFileWebscript extends DeclarativeWebScript {

	private static final Logger LOG = LoggerFactory.getLogger(CreateIndexFileWebscript.class);
	private ServiceRegistry serviceRegistry;
	private static JSONArray dataObj;
	private static JSONObject obj;
	private NodeRef nodeRef;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		LOG.info("Start of searching and indexing nodes present in a particular site.....");

		Object content = req.parseContent();
		if (!(content instanceof JSONArray)) {
			status.setCode(Status.STATUS_BAD_REQUEST);
			status.setRedirect(true);
			return model;
		}

		try {
			dataObj = (JSONArray) content;
			for (int i = 0; i < dataObj.length(); i++) {
				obj = (JSONObject) dataObj.get(i);
				nodeRef = new NodeRef((String) obj.get("nodeRef"));
				LOG.info("Node/Nodes Selected is/are: " + nodeRef);
				FileInfo childFileInfo = serviceRegistry.getFileFolderService().getFileInfo(nodeRef);
				Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
				properties = childFileInfo.getProperties();
				LOG.info("Node Present in folder and it's properties are ..." + properties);
				if (childFileInfo.getName().contains("_index.csv")) {
					LOG.info("Index File has already been created ......");

				} else {
					int dotIndex = childFileInfo.getName().lastIndexOf('.');
					String fileName = null;
					if (dotIndex != -1) {
						fileName = childFileInfo.getName().substring(0, dotIndex);
					}
					// createFile(childFileInfo, childFileInfo.getName().trim() + "_" + "index.csv",
					// properties);
					else {
						fileName = childFileInfo.getName();
					}

					createFile(childFileInfo, fileName + "_" + "index.csv", properties);
					LOG.info("Index File has been created ......");
				}
			}

		} catch (Exception e) {
			status.setCode(Status.STATUS_INTERNAL_SERVER_ERROR);
			status.setException(e);
			status.setMessage("Problem executing web script");
		}

		model.put("message", "message");

		return model;

	}

	private NodeRef getPathNodeRef() {
		Map<String, Serializable> params = new HashMap<>();
		params.put("query", CommonConstants2.SR_SITE_XPATH_QUERY);
		return serviceRegistry.getNodeLocatorService().getNode("xpath", null, params);
	}

	private FileInfo createFile(FileInfo folderInfo, String filename, Map<QName, Serializable> newFile)
			throws FileExistsException {

		FileInfo fileInfo = serviceRegistry.getFileFolderService().create(getPathNodeRef(), filename,
				ContentModel.TYPE_CONTENT);
		NodeRef newFileNodeRef = fileInfo.getNodeRef();
		ContentWriter writer = serviceRegistry.getFileFolderService().getWriter(newFileNodeRef);
		writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_CSV);
		writer.setEncoding("UTF-8");
		writer.putContent(newFile.toString());
		LOG.info("Properties : " + fileInfo.getProperties());

		return fileInfo;
	}

}
