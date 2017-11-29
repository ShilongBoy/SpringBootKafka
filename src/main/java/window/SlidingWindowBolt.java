package window;

import clojure.lang.Obj;
import com.alibaba.fastjson.JSON;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;
import org.apache.storm.windowing.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.AppCardInfo;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowBolt  extends BaseWindowedBolt {


    private static final long serialVersionUID = 1L;
    private OutputCollector collector;
    private ConcurrentHashMap<String,Object> concurrentHashMap;
    JedisPool pool;

    public static final Logger logger = LoggerFactory.getLogger(SlidingWindowBolt.class);

    @SuppressWarnings("rawtypes")
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        concurrentHashMap=new ConcurrentHashMap<String,Object>();
        JedisPoolConfig config = new JedisPoolConfig();
        // 一个pool最多有多少个状态为idle(空闲的)的jedis实例。
        config.setMaxIdle(10);
        // 最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
        config.setMaxWaitMillis(1000 * 3);
        this.pool = new JedisPool(config, "10.1.69.11", 6379);
        logger.debug("prepare ok");
    }



    public void execute(TupleWindow input) {
        // TODO Auto-generated method stub
        Jedis jedis=pool.getResource();
        int sum=0;
        System.out.print("一个窗口内的数据");
        for(Tuple tuple: ((Window<Tuple>) input).get()){
            Double tranAmt=0.0;
            String str= tuple.getString(0);
            Object appCardInfo= JSON.parseObject(str).get("AppCardInfo");
            AppCardInfo cardInf=JSON.parseObject(String.valueOf(appCardInfo), AppCardInfo.class);
            logger.info("cardInf:"+cardInf);

            String cardNo=cardInf.getCardNo();

            if(jedis.exists(cardNo)){
                String keyValue=jedis.get(cardNo);
                jedis.set(cardNo,String.valueOf(Double.valueOf(keyValue)+Double.valueOf(cardInf.getTranAmt())));
            }else {
                jedis.set(cardNo,String.valueOf(cardInf.getTranAmt()));
            }

/*            if(concurrentHashMap.containsKey(cardInf.getCardNo())){
                Double values=(Double)concurrentHashMap.get(cardInf.getCardNo());
                tranAmt=Double.valueOf(cardInf.getTranAmt())+values;
                concurrentHashMap.put(cardInf.getCardNo(),String.valueOf(tranAmt));
                jedis.set(cardInf.getCardNo(),String.valueOf(tranAmt));
            }else {
                jedis.set(cardInf.getCardNo(),String.valueOf(cardInf.getTranAmt()));
            }*/
        }
        System.out.println("======="+sum);

        pool.returnResource(jedis);
        collector.emit(new Values(sum));
    }



    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // TODO Auto-generated method stub
        declarer.declare(new Fields("newMessageBean"));
    }
}
