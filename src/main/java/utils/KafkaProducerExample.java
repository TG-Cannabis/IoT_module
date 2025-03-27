package utils;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;
public class KafkaProducerExample {
    public static void main(String[] args) {
        // Configure the producer properties
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "20.119.83.42:9093");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("key.serializer", "org.+6*+vie.kafka.common.serialization.StringSerializer");
        // Create a Kafka producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        // Create a message
        String topic = "sensor-data";
        String key = "key1";
        String value = "Hello, Kafka!";
        // Send the message
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Message sent successfully. Offset: " + metadata.offset());
            } else {
                System.out.println("Failed to send message: " + exception.getMessage());
            }
        });
        // Close the producer
        producer.close();
    }
}