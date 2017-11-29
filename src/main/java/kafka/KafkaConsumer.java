package kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

public class KafkaConsumer {

    Logger logger= LoggerFactory.getLogger(KafkaConsumer.class);

    /**
     * KafkaLinser监听
     * @param record
     */
    @KafkaListener(topics = {"testTopic"})
    public void listen(ConsumerRecord<?, ?> record) {
    }
}
