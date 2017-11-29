package kafka;

import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Properties;

public class SimpleProducer {

    static Logger logger= LoggerFactory.getLogger(SimpleProducer.class);

    public static void sedMessage(String str){
    Properties props = new Properties();
        props.put("bootstrap.servers", "10.1.69.11:6667");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.METADATA_FETCH_TIMEOUT_CONFIG, "3000");
        KafkaProducer<String , String> producer = new KafkaProducer<String, String>(props);

        ProducerRecord<String,String> record = new ProducerRecord<String, String>("stormKafka", str);
        producer.send(record ,
                new Callback() {
                    public void onCompletion(RecordMetadata metadata, Exception e) {
                        if(e != null)
                            e.printStackTrace();
                        System.out.println("Partition:"+metadata.partition()+"  The offset of the record we just sent is: " + metadata.offset());
                        logger.info("Partition:"+metadata.partition()+"  The offset of the record we just sent is: " + metadata.offset());
                    }
                });
        producer.close();
  }

  public static void  main(String []args){
      sedMessage("");
  }

}
