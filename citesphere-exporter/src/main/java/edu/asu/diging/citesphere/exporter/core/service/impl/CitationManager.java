package edu.asu.diging.citesphere.exporter.core.service.impl;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import edu.asu.diging.citesphere.exporter.core.service.ICitationManager;
import edu.asu.diging.citesphere.exporter.core.service.IZoteroManager;
import edu.asu.diging.citesphere.exporter.core.service.iterator.CitationIterator;
import edu.asu.diging.citesphere.exporter.core.service.iterator.impl.CollectionCitationIterator;
import edu.asu.diging.citesphere.exporter.core.service.iterator.impl.GroupCitationIterator;
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

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
        return getGroupItemsIterator(info);
    }

    protected CitationIterator getGroupItemsIterator(JobInfo info) throws ZoteroHttpStatusException {
        Optional<CitationGroup> groupOptional = groupRepo.findById(new Long(info.getGroupId()));
        ICitationGroup group = null;
        if (groupOptional.isPresent()) {
            ICitationGroup latestGroup = zoteroManager.getGroup(info.getZoteroId(), info.getZotero(), info.getGroupId(),
                    true);
            if (latestGroup.getVersion() != groupOptional.get().getVersion()) {
                updateCitations(info, groupOptional.get());
            }
            group = groupOptional.get();
        } else {
            group = createGroupCitations(info);
        }

        return new GroupCitationIterator(gcMappingRepo, group, pageSize);
    }

    protected CitationIterator getCollectionItemsIterator(JobInfo info) throws ZoteroHttpStatusException {
        Optional<CitationCollection> collection = collectionRepo.findById(info.getCollectionId());
        ICitationCollection citationCollection;
        if (collection.isPresent()) {
            ICitationCollection latestCollection = zoteroManager.getCitationCollection(info.getZoteroId(),
                    info.getZotero(), info.getGroupId(), info.getCollectionId());
            if (latestCollection.getVersion() != collection.get().getVersion()) {
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
        ICitationGroup group = zoteroManager.getGroup(info.getZoteroId(), info.getZotero(), info.getGroupId(), true);
        group = groupRepo.save((CitationGroup) group);
        downloadCitations(info, group, this::getGroupItems, this::createGroupMapping);
        return group;
    }

    protected void updateCitations(JobInfo info, ICitationCollection collection) throws ZoteroHttpStatusException {
        ccMappingRepo.deleteByCollection((CitationCollection) collection);
        collectionRepo.delete((CitationCollection) collection);
        createCitations(info);
    }

    protected void updateCitations(JobInfo info, ICitationGroup group) throws ZoteroHttpStatusException {
        gcMappingRepo.deleteByGroup((CitationGroup) group);
        groupRepo.delete((CitationGroup) group);
        createGroupCitations(info);
    }

    private void downloadCitations(JobInfo info, IGrouping collection,
            BiFunction<JobInfo, Integer, CitationResults> retrievalFunction,
            BiConsumer<ICitation, IGrouping> mappingFunction) throws ZoteroHttpStatusException {
        CitationResults result = retrievalFunction.apply(info, 0);

        long totalResults = result.getTotalResults();
        long pageCount = totalResults / pageSize + (totalResults % pageSize > 0 ? 1 : 0);
        int currentPage = 1;

        while (currentPage <= pageCount) {
            // we need to get the first page above to know who many pages there are,
            // afterwards though we need to retrieve the next one, hence this workaround
            if (result == null) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    logger.error("Could not sleep.", e);
                }
                result = retrievalFunction.apply(info, currentPage);
            }
            for (ICitation citation : result.getCitations()) {
                // this is so ugly but not sure what else we can do
                Optional<Citation> oldCitationOptional = citationRepository.findById(citation.getKey());
                if (oldCitationOptional.isPresent()) {
                    Citation oldCitation = oldCitationOptional.get();
                    citation = updateCitation(citation, oldCitation);
                } 
                citation = citationRepository.save((Citation)citation);
                mappingFunction.accept(citation, collection);
            }
            result = null;
            currentPage += 1;
        }
    }

    /**
     * Unfortunately, simply saving a citation through the repo doesn't work in our case.
     * References, authors, etc do not have ids so they can't be merged by Hibernate, which
     * means that if we just save a citation, the lists/sets of a ciation will be replace 
     * by new ones which causes a "A collection with cascade="all-delete-orphan" was no longer referenced"
     * exception.
     * @param citation new citation data
     * @param existingCitation existing citation
     * @return the updated existing citation.
     */
    private ICitation updateCitation(ICitation citation, Citation existingCitation) {
        for (Field citField : Citation.class.getDeclaredFields()) {
            if (!List.class.isAssignableFrom(citField.getType())
                    && !Set.class.isAssignableFrom(citField.getType())
                    && !Map.class.isAssignableFrom(citField.getType())) {
                citField.setAccessible(true);
                try {
                    Object newValue = citField.get(citation);
                    citField.set(existingCitation, newValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    logger.error("Could not update object.", e);
                }
            }
        }
        
        if (existingCitation.getReferences() != null) {
            existingCitation.getReferences().clear();
        } else {
            existingCitation.setReferences(new HashSet<>());
        }
        if (citation.getReferences() != null) {
            existingCitation.getReferences().addAll(citation.getReferences());
        }
        
        if (existingCitation.getConceptTags() != null) {
            existingCitation.getConceptTags().clear();
        }
        if (citation.getConceptTags() != null) {
            existingCitation.getConceptTags().addAll(citation.getConceptTags());
        }
        
        if (existingCitation.getOtherCreatorRoles() != null) {
            existingCitation.getOtherCreatorRoles().clear();
        } else {
            existingCitation.setOtherCreators(new HashSet<>());
        }
        if (citation.getOtherCreatorRoles() != null) {
            existingCitation.getOtherCreatorRoles().addAll(citation.getOtherCreatorRoles());
        }
        
        if (existingCitation.getAuthors() != null) {
            existingCitation.getAuthors().clear();
        } else {
            existingCitation.setAuthors(new HashSet<>());
        }
        if (citation.getAuthors() != null) {
            existingCitation.getAuthors().addAll(citation.getAuthors());
        }
        
        if (existingCitation.getEditors() != null) {
            existingCitation.getEditors().clear();
        } else {
            existingCitation.setEditors(new HashSet<>());
        }
        if (existingCitation.getEditors() != null) {
            existingCitation.getEditors().addAll(citation.getEditors());
        }
        
        return existingCitation;
    }

    protected CitationResults getCollectionItems(JobInfo info, int page) {
        try {
            return zoteroManager.getCollectionItems(info.getZoteroId(), info.getZotero(), info.getGroupId(),
                    info.getCollectionId(), page, SORT_BY_TITLE, null);
        } catch (ZoteroHttpStatusException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected CitationResults getGroupItems(JobInfo info, int page) {
        try {
            return zoteroManager.getGroupItems(info.getZoteroId(), info.getZotero(), info.getGroupId(), page,
                    SORT_BY_TITLE, null);
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
        mapping.setGroup((ICitationGroup) group);
        mapping.setCitation(citation);
        gcMappingRepo.save(mapping);
    }
}
