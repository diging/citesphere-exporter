package edu.asu.diging.citesphere.exporter.core.service.iterator.impl;

import java.util.Iterator;
import java.util.List;

import org.springframework.data.domain.PageRequest;

import edu.asu.diging.citesphere.data.bib.CollectionCitationMappingRepository;
import edu.asu.diging.citesphere.exporter.core.service.iterator.CitationIterator;
import edu.asu.diging.citesphere.model.bib.ICitation;
import edu.asu.diging.citesphere.model.bib.impl.CitationCollection;
import edu.asu.diging.citesphere.model.bib.impl.CollectionCitationMapping;

public class DbCollectionCitationIterator implements Iterator<ICitation>, CitationIterator {
    
    private CollectionCitationMappingRepository repo;
    private Iterator<CollectionCitationMapping> mappings;
    private long totalPages;
    private int currentPage;
    private int pageSize;
    private CitationCollection collection;
    
    public DbCollectionCitationIterator(CollectionCitationMappingRepository repo, CitationCollection collection, int pageSize) {
        this.repo = repo;
        this.pageSize = pageSize;
        this.currentPage = 0;
        this.collection = collection;
        long totalCount = repo.countByCollection(collection);
        totalPages = totalCount/pageSize + (totalCount%pageSize > 0 ? 1 : 0);
    }

    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.service.iterator.impl.CitationIteratorX#hasNext()
     */
    @Override
    public boolean hasNext() {
        if ((mappings == null || !mappings.hasNext()) && currentPage>=totalPages) {
            return false;
        }
        
        if (mappings == null) {
            List<CollectionCitationMapping> ccMappings = repo.findByCollection(collection, PageRequest.of(currentPage, pageSize));
            currentPage += 1;
            mappings = ccMappings.iterator();
        }
        
        return mappings.hasNext();
    }

    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.service.iterator.impl.CitationIteratorX#next()
     */
    @Override
    public ICitation next() {
        CollectionCitationMapping mapping = mappings.next();
        return mapping.getCitation();
    }

}
