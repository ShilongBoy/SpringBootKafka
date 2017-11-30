package window;


import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.MessageToRedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.List;
import java.util.Map;

public class RedisStoreBlot extends BaseBasicBolt{

    private static final long serialVersionUID = 1L;
    public static final Logger logger= LoggerFactory.getLogger(RedisStoreBlot.class);
    public static final String LOG_ENTRY = "str";
    private OutputCollector collector;

    private Jedis jedis;
    private Pipeline pipeline;

    public  void   insertToRedis(List<MessageToRedis> redisRecords) {
        for(MessageToRedis message :redisRecords)
        {
            switch (message.getDataType())
            {

                case STRING:
                    pipeline.set(message.getKey(),message.getValues());
                    break;
                case SET:
                    pipeline.zadd(message.getKey(),Double.parseDouble(message.getValues()),message.getValues());
                    break;
                default:break;
            }
        }
        pipeline.sync();
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }

    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector=collector;
        this.jedis=new Jedis("10.200.187.73", 6379);
        this.pipeline=this.jedis.pipelined();
    }


    public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {

    }
}
