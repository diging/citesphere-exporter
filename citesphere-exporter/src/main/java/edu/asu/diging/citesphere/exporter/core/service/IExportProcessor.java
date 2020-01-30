package edu.asu.diging.citesphere.exporter.core.service;

import edu.asu.diging.citesphere.messages.model.KafkaJobMessage;

public interface IExportProcessor {

    void process(KafkaJobMessage message);

}