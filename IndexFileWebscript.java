package com.swissre.alfrm.webscripts;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import com.swissre.alfrm.constants.CommonConstants;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.io.*;

public class IndexFileWebscript extends DeclarativeWebScript {

	private static final Logger LOG = LoggerFactory.getLogger(IndexFileWebscript.class);
	private ServiceRegistry serviceRegistry;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		LOG.info("Start of searching and indexing nodes present in a particular site.....");

		NodeRef parentFolderNodeRef = getPathNodeRef();
		List<ChildAssociationRef> childRef = serviceRegistry.getNodeService().getChildAssocs(parentFolderNodeRef);
		for (int i = 0; i < childRef.size(); i++) {
			LOG.info("Node Present in a folder is:  " + childRef.get(i).getChildRef());
			FileInfo childFileInfo = serviceRegistry.getFileFolderService().getFileInfo(childRef.get(i).getChildRef());
			LOG.info("Node Present in folder and it's properties are ..." + childFileInfo.getProperties());
			String path = "workspace://SpacesStore/" + childRef.get(i).getChildRef().getId();
			File file = new File(childFileInfo.toString());
			LOG.info("File info ..." + file);
			//createFile(childFileInfo, childFileInfo.getName() + "_" + System.currentTimeMillis() + "latest");
			createFile(childFileInfo, childFileInfo.getName() + "_" + "index", file);
			

		}

		LOG.info("Index File has been created ......");
		model.put("message", "message");

		return model;

	}

	private NodeRef getPathNodeRef() {
		Map<String, Serializable> params = new HashMap<>();
		params.put("query", CommonConstants.SR_FOLDER_XPATH_QUERY);
		return serviceRegistry.getNodeLocatorService().getNode("xpath", null, params);
	}

	private FileInfo createFile(FileInfo folderInfo, String filename, File newFile) throws FileExistsException {

		FileInfo fileInfo = serviceRegistry.getFileFolderService().create(getPathNodeRef(), filename,
				ContentModel.TYPE_CONTENT);

		NodeRef newFileNodeRef = fileInfo.getNodeRef();
		ContentWriter writer = serviceRegistry.getFileFolderService().getWriter(newFileNodeRef);
		writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_CSV);
		writer.setEncoding("UTF-8");
		//writer.putContent("hello");
		writer.putContent(newFile);

		return fileInfo;
	}

}
