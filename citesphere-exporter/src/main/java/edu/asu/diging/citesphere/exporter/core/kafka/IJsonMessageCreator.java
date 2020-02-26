package edu.asu.diging.citesphere.exporter.core.kafka;

import edu.asu.diging.citesphere.exporter.core.exception.MessageCreationException;
import edu.asu.diging.citesphere.messages.model.KafkaExportReturnMessage;

public interface IJsonMessageCreator {

    /* (non-Javadoc)
     * @see edu.asu.giles.service.kafka.impl.IJsonMessageCreator#createMessage(edu.asu.giles.service.requests.IRequest)
     */
    String createMessage(KafkaExportReturnMessage msg) throws MessageCreationException;

}