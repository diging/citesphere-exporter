package edu.asu.diging.citesphere.exporter.core.kafka;

import edu.asu.diging.citesphere.exporter.core.exception.MessageCreationException;
import edu.asu.diging.citesphere.messages.model.KafkaExportReturnMessage;

public interface IKafkaRequestProducer {

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.asu.giles.service.kafka.impl.IOCRRequestProducer#sendOCRRequest(java.lang
     * .String)
     */
    void sendRequest(KafkaExportReturnMessage msg, String topic) throws MessageCreationException;

}