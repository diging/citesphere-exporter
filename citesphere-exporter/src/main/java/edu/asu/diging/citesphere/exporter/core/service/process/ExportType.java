package edu.asu.diging.citesphere.exporter.core.service.process;

public enum ExportType {
    CSV("text/csv");
    
    private String contentType;
    
    private ExportType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getContentType() {
        return contentType;
    }
}
