package edu.asu.diging.citesphere.exporter.core.service.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

import edu.asu.diging.citesphere.data.bib.CitationCollectionRepository;
import edu.asu.diging.citesphere.data.bib.CitationGroupRepository;
import edu.asu.diging.citesphere.data.bib.ICitationDao;
import edu.asu.diging.citesphere.exporter.core.exception.AccessForbiddenException;
import edu.asu.diging.citesphere.exporter.core.exception.GroupDoesNotExistException;
import edu.asu.diging.citesphere.exporter.core.exception.OutOfDateException;
import edu.asu.diging.citesphere.exporter.core.exception.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.exporter.core.service.ICitationManager;
import edu.asu.diging.citesphere.exporter.core.service.IZoteroManager;
import edu.asu.diging.citesphere.model.bib.ICitation;
import edu.asu.diging.citesphere.model.bib.ICitationCollection;
import edu.asu.diging.citesphere.model.bib.ICitationGroup;

@Service
@PropertySource("classpath:/config.properties")
public class CitationManager implements ICitationManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${_db_page_size}")
    private int pageSize;

    @Autowired
    private CitationGroupRepository groupRepo;

    @Autowired
    private CitationCollectionRepository collectionRepo;
    
    @Autowired
    private ICitationDao citationDao;

    @Autowired
    private IZoteroManager zoteroManager;

    
    @Override
    @SuppressWarnings("unchecked")
    public CloseableIterator<ICitation> getAllGroupItems(JobInfo info) throws ZoteroHttpStatusException, GroupDoesNotExistException, AccessForbiddenException, OutOfDateException {
        Optional<ICitationGroup> groupOptional = groupRepo.findByGroupId(new Long(info.getGroupId()));
        if (groupOptional.isPresent()) {
            ICitationGroup group = groupOptional.get();
            if (!group.getUsers().contains(info.getUsername())) {
                throw new AccessForbiddenException();
            }
            boolean isModified = zoteroManager.isGroupModified(info.getZoteroId(), info.getZotero(), group.getGroupId() + "", group.getContentVersion());
            if (isModified) {
                throw new OutOfDateException();
            } 
            
            return (CloseableIterator<ICitation>) citationDao.getCitationIterator(info.getGroupId(), info.getCollectionId());
        }
        
        throw new GroupDoesNotExistException("Group " + info.getGroupId() + " does not exist.");
    }
    
    @Override
    public ICitationGroup getGroup(JobInfo info) {
        Optional<ICitationGroup> groupOptional = groupRepo.findByGroupId(new Long(info.getGroupId()));
        if (groupOptional.isPresent()) {
            return groupOptional.get();
        }
        return null;
    }
    
    @Override
    public ICitationCollection getCollection(JobInfo info) {
       Optional<ICitationCollection> collection = collectionRepo.findByKey(info.getCollectionId());
       return collection.isPresent() ? collection.get() : null;
    }
}
