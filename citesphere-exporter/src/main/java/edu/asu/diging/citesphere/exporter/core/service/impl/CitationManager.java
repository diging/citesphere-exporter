package edu.asu.diging.citesphere.exporter.core.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import edu.asu.diging.citesphere.data.bib.CitationCollectionRepository;
import edu.asu.diging.citesphere.data.bib.CitationGroupRepository;
import edu.asu.diging.citesphere.data.bib.CitationRepository;
import edu.asu.diging.citesphere.data.bib.CollectionCitationMappingRepository;
import edu.asu.diging.citesphere.exporter.core.exception.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.exporter.core.service.IZoteroManager;
import edu.asu.diging.citesphere.exporter.core.service.iterator.CitationIterator;
import edu.asu.diging.citesphere.exporter.core.service.iterator.impl.DbCollectionCitationIterator;
import edu.asu.diging.citesphere.model.bib.ICitation;
import edu.asu.diging.citesphere.model.bib.ICitationCollection;
import edu.asu.diging.citesphere.model.bib.impl.Citation;
import edu.asu.diging.citesphere.model.bib.impl.CitationCollection;
import edu.asu.diging.citesphere.model.bib.impl.CitationResults;
import edu.asu.diging.citesphere.model.bib.impl.CollectionCitationMapping;

@Service
@PropertySource("classpath:/config.properties")
public class CitationManager implements ICitationManager {
    
    @Value("${_db_page_size}")
    private int pageSize;
    
    @Autowired
    private CitationGroupRepository groupRepo;
    
    @Autowired
    private CitationCollectionRepository collectionRepo;
    
    @Autowired
    private CitationRepository citationRepository;
    
    @Autowired
    private CollectionCitationMappingRepository ccMappingRepo;
    
    @Autowired
    private IZoteroManager zoteroManager;
    
    private final String SORT_BY_TITLE = "title";
    

    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.service.impl.ICitationManager#getCitations(edu.asu.diging.citesphere.exporter.core.service.impl.JobInfo)
     */
    @Override
    public CitationIterator getCitations(JobInfo info) throws ZoteroHttpStatusException {
        if (info.getCollectionId() != null && !info.getCollectionId().trim().isEmpty()) {
            return getCollectionItemsIterator(info);
        }
        return null;
    }
    
    protected CitationIterator getCollectionItemsIterator(JobInfo info) throws ZoteroHttpStatusException {
        Optional<CitationCollection> collection = collectionRepo.findById(info.getCollectionId());
        if(collection.isPresent()) {
            CitationResults results = zoteroManager.getCollectionItems(info.getZoteroId(), info.getZotero(), info.getGroupId(), info.getCollectionId(), 1, SORT_BY_TITLE, collection.get().getVersion());
            if (results.isNotModified()) {
                updateCitations(info, collection.get());
            } 
            return new DbCollectionCitationIterator(ccMappingRepo, collection.get(), pageSize);
        }
        return null;
    }
    
    protected void updateCitations(JobInfo info, ICitationCollection collection) throws ZoteroHttpStatusException {
        ccMappingRepo.deleteByCollection((CitationCollection)collection);
        CitationResults result = zoteroManager.getCollectionItems(info.getZoteroId(), info.getZotero(), info.getGroupId(), info.getCollectionId(), 0, SORT_BY_TITLE, null);
        
        long totalResults = result.getTotalResults();
        long pageCount = totalResults/pageSize + (totalResults%pageSize > 0 ? 1 : 0);
        int currentPage = 0;
        
        while (currentPage < pageCount) {
            // we need to get the first page above to know who many pages there are,
            // afterwards though we need to retrieve the next one, hence this workaround
            if (result == null) {
                result = zoteroManager.getCollectionItems(info.getZoteroId(), info.getZotero(), info.getGroupId(), info.getCollectionId(), currentPage, SORT_BY_TITLE, null);
            }
            for (ICitation citation : result.getCitations()) {
                citationRepository.save((Citation)citation);
                
                CollectionCitationMapping mapping = new CollectionCitationMapping();
                mapping.setCollection(collection);
                mapping.setCitation(citation);
                ccMappingRepo.save(mapping);
            }
            result = null;
            currentPage += 1;
        }
        
    }
}
