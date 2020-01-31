package edu.asu.diging.citesphere.exporter.core.service.impl;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.management.RuntimeErrorException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import edu.asu.diging.citesphere.data.bib.CitationCollectionRepository;
import edu.asu.diging.citesphere.data.bib.CitationGroupRepository;
import edu.asu.diging.citesphere.data.bib.CitationRepository;
import edu.asu.diging.citesphere.data.bib.CollectionCitationMappingRepository;
import edu.asu.diging.citesphere.data.bib.GroupCitationMappingRepository;
import edu.asu.diging.citesphere.exporter.core.exception.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.exporter.core.service.IZoteroManager;
import edu.asu.diging.citesphere.exporter.core.service.iterator.CitationIterator;
import edu.asu.diging.citesphere.exporter.core.service.iterator.impl.CollectionCitationIterator;
import edu.asu.diging.citesphere.model.bib.ICitation;
import edu.asu.diging.citesphere.model.bib.ICitationCollection;
import edu.asu.diging.citesphere.model.bib.ICitationGroup;
import edu.asu.diging.citesphere.model.bib.IGrouping;
import edu.asu.diging.citesphere.model.bib.impl.Citation;
import edu.asu.diging.citesphere.model.bib.impl.CitationCollection;
import edu.asu.diging.citesphere.model.bib.impl.CitationGroup;
import edu.asu.diging.citesphere.model.bib.impl.CitationResults;
import edu.asu.diging.citesphere.model.bib.impl.CollectionCitationMapping;
import edu.asu.diging.citesphere.model.bib.impl.GroupCitationMapping;

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
    private GroupCitationMappingRepository gcMappingRepo;

    @Autowired
    private IZoteroManager zoteroManager;

    private final String SORT_BY_TITLE = "title";

    /*
     * (non-Javadoc)
     * 
     * @see edu.asu.diging.citesphere.exporter.core.service.impl.ICitationManager#
     * getCitations(edu.asu.diging.citesphere.exporter.core.service.impl.JobInfo)
     */
    @Override
    public CitationIterator getCitations(JobInfo info) throws ZoteroHttpStatusException {
        if (info.getCollectionId() != null && !info.getCollectionId().trim().isEmpty()) {
            return getCollectionItemsIterator(info);
        }
        return null;
    }

    protected CitationIterator getGroupItemsIterator(JobInfo info) throws ZoteroHttpStatusException {
        Optional<CitationGroup> groupOptional = groupRepo.findById(new Long(info.getGroupId()));
        ICitationGroup group = null;
        if (groupOptional.isPresent()) {
            CitationResults results = zoteroManager.getGroupItems(info.getZoteroId(), info.getZotero(),
                    info.getGroupId(), 0, SORT_BY_TITLE, groupOptional.get().getVersion());
            if (!results.isNotModified()) {
                updateCitations(info, groupOptional.get());
            }
            group = groupOptional.get();
        } else {
            group = createGroupCitations(info);
        }
    }

    protected CitationIterator getCollectionItemsIterator(JobInfo info) throws ZoteroHttpStatusException {
        Optional<CitationCollection> collection = collectionRepo.findById(info.getCollectionId());
        ICitationCollection citationCollection;
        if (collection.isPresent()) {
            CitationResults results = zoteroManager.getCollectionItems(info.getZoteroId(), info.getZotero(),
                    info.getGroupId(), info.getCollectionId(), 1, SORT_BY_TITLE, collection.get().getVersion());
            if (!results.isNotModified()) {
                updateCitations(info, collection.get());
            }
            citationCollection = collection.get();
        } else {
            citationCollection = createCitations(info);
        }
        return new CollectionCitationIterator(ccMappingRepo, citationCollection, pageSize);
    }

    protected ICitationCollection createCitations(JobInfo info) throws ZoteroHttpStatusException {
        ICitationCollection collection = zoteroManager.getCitationCollection(info.getZoteroId(), info.getZotero(),
                info.getGroupId(), info.getCollectionId());
        collection = collectionRepo.save((CitationCollection) collection);
        downloadCitations(info, collection, this::getCollectionItems, this::createCollectionMapping);
        return collection;
    }
    
    protected ICitationGroup createGroupCitations(JobInfo info) throws ZoteroHttpStatusException {
        ICitationGroup group = zoteroManager.getGroup(info.getZoteroId(), info.getZotero(),
                info.getGroupId(), true);
        group = groupRepo.save((CitationGroup)group);
        downloadCitations(info, group, this::getGroupItems, this::createGroupMapping);
        return group;
    }

    protected void updateCitations(JobInfo info, ICitationCollection collection) throws ZoteroHttpStatusException {
        ccMappingRepo.deleteByCollection((CitationCollection) collection);
        downloadCitations(info, collection, this::getCollectionItems, this::createCollectionMapping);
    }

    protected void updateCitations(JobInfo info, ICitationGroup group) throws ZoteroHttpStatusException {
        gcMappingRepo.deleteByGroup((CitationGroup) group);
        downloadCitations(info, group, this::getGroupItems, this::createGroupMapping);
    }

    private void downloadCitations(JobInfo info, IGrouping collection,
            Function<JobInfo, CitationResults> retrivalFunction, BiConsumer<ICitation, IGrouping> mappingFunction)
            throws ZoteroHttpStatusException {
        CitationResults result = retrivalFunction.apply(info);

        long totalResults = result.getTotalResults();
        long pageCount = totalResults / pageSize + (totalResults % pageSize > 0 ? 1 : 0);
        int currentPage = 0;

        while (currentPage < pageCount) {
            // we need to get the first page above to know who many pages there are,
            // afterwards though we need to retrieve the next one, hence this workaround
            if (result == null) {
                result = zoteroManager.getCollectionItems(info.getZoteroId(), info.getZotero(), info.getGroupId(),
                        info.getCollectionId(), currentPage, SORT_BY_TITLE, null);
            }
            for (ICitation citation : result.getCitations()) {
                citation = citationRepository.save((Citation) citation);

                mappingFunction.accept(citation, collection);
            }
            result = null;
            currentPage += 1;
        }
    }

    protected CitationResults getCollectionItems(JobInfo info) {
        try {
            return zoteroManager.getCollectionItems(info.getZoteroId(), info.getZotero(),
                    info.getGroupId(), info.getCollectionId(), 0, SORT_BY_TITLE, null);
        } catch (ZoteroHttpStatusException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    protected CitationResults getGroupItems(JobInfo info) {
        try {
            return zoteroManager.getGroupItems(info.getZoteroId(), info.getZotero(),
                        info.getGroupId(), 0, SORT_BY_TITLE, null);
        } catch (ZoteroHttpStatusException e) {
            throw new RuntimeException(e);
        }
    }

    protected void createCollectionMapping(ICitation citation, IGrouping collection) {
        CollectionCitationMapping mapping = new CollectionCitationMapping();
        mapping.setCollection((ICitationCollection) collection);
        mapping.setCitation(citation);
        ccMappingRepo.save(mapping);
    }
    
    protected void createGroupMapping(ICitation citation, IGrouping group) {
        GroupCitationMapping mapping = new GroupCitationMapping();
        mapping.setGroup((ICitationGroup)group);
        mapping.setCitation(citation);
        gcMappingRepo.save(mapping);
    }
}
