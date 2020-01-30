package edu.asu.diging.citesphere.exporter.core.service.iterator.impl;

import java.util.List;

import edu.asu.diging.citesphere.data.bib.CollectionCitationMappingRepository;
import edu.asu.diging.citesphere.exporter.core.exception.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.exporter.core.service.IZoteroManager;
import edu.asu.diging.citesphere.exporter.core.service.iterator.CitationIterator;
import edu.asu.diging.citesphere.model.bib.ICitation;
import edu.asu.diging.citesphere.model.bib.ICitationCollection;
import edu.asu.diging.citesphere.model.bib.impl.CitationCollection;
import edu.asu.diging.citesphere.model.bib.impl.CitationResults;
import edu.asu.diging.citesphere.model.bib.impl.CollectionCitationMapping;

public class ZoteroCollectionCitationIterator implements CitationIterator {

    private IZoteroManager zoteroManager;
    private CollectionCitationMappingRepository mappingRepo;
    private String zoteroUserId;
    private String token;
    private String groupId;
    private ICitationCollection collection;
    private String sortBy;
    private int currentPage;
    private int totalPages;

    private List<CollectionCitationMapping> mappings;

    public ZoteroCollectionCitationIterator(IZoteroManager zoteroManager, String zoteroUserId, String token,
            String groupId, ICitationCollection collection, String sortBy, CollectionCitationMappingRepository repo) throws ZoteroHttpStatusException {
        this.zoteroManager = zoteroManager;
        this.mappingRepo = repo;
        this.zoteroUserId = zoteroUserId;
        this.token = token;
        this.groupId = groupId;
        this.collection = collection;
        this.sortBy = sortBy;
        this.currentPage = 0;
        init();
    }

    private void init() throws ZoteroHttpStatusException {
        
    }

    private void retrievePage() throws ZoteroHttpStatusException {
        
        
    }

    @Override
    public boolean hasNext() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ICitation next() {
        // TODO Auto-generated method stub
        return null;
    }

}
