package edu.asu.diging.citesphere.exporter.core.kafka.impl;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.asu.diging.citesphere.exporter.core.exception.MessageCreationException;
import edu.asu.diging.citesphere.exporter.core.kafka.IJsonMessageCreator;
import edu.asu.diging.citesphere.messages.model.KafkaExportReturnMessage;

@Component
public class JsonMessageCreator implements IJsonMessageCreator {

    /* (non-Javadoc)
     * @see edu.asu.giles.service.kafka.impl.IJsonMessageCreator#createMessage(edu.asu.giles.service.requests.IRequest)
     */
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.importer.core.kafka.impl.IJsonMessageCreator#createMessage(edu.asu.diging.citesphere.importer.core.kafka.impl.KafkaReturnMessage)
     */
    @Override
    public String createMessage(KafkaExportReturnMessage msg) throws MessageCreationException {
        ObjectMapper mapper = new ObjectMapper(); 
        try {
            return mapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new MessageCreationException("Could not create JSON.", e);
        }
    }
}