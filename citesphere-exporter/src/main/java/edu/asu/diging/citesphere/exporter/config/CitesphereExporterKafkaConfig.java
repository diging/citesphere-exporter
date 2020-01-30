package edu.asu.diging.citesphere.exporter.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import edu.asu.diging.citesphere.exporter.core.kafka.ReferenceExportListener;

@Configuration
@EnableKafka
@PropertySource("classpath:/config.properties")
public class CitesphereExporterKafkaConfig {
    
    @Value("${_kafka_hosts}")
    private String kafkaHosts;
    
    @Value("${_producer_id_prefix}")
    private String producerIdPrefix;
    
    @Value("${_consumer_group}")
    private String consumerGroup;
    
    @Value("${_consumer_client_id_prefix}")
    private String consumerClientIdPrefix;
    
    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        // list of host:port pairs used for establishing the initial connections
        // to the Kakfa cluster
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                getHosts());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                IntegerDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerClientIdPrefix + new Random().nextInt(100));
        // consumer groups allow a pool of processes to divide the work of
        // consuming and processing records
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);

        return props;
    }

    @Bean
    public ConsumerFactory consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        return factory;
    }
    
    @Bean
    public ReferenceExportListener referenceExportListener() {
        return new ReferenceExportListener();
    }

    public String getHosts() {
        return kafkaHosts;
    }

    public String getProducerId() {
        return producerIdPrefix + new Random().nextInt(100);
    }
}