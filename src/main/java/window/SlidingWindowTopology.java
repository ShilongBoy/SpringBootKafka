package window;

import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.kafka.*;
import org.apache.storm.kafka.bolt.KafkaBolt;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseWindowedBolt.Duration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class SlidingWindowTopology {

    public static void main(String []args){
        TopologyBuilder builder = new TopologyBuilder();

        String zks = "10.1.69.11:2181,10.1.69.12:2181,10.1.69.13:2181";
        String topic = "stormKafka";
        String zkRoot = "/storm";
        String id = "stormKafka";
        BrokerHosts brokerHosts = new ZkHosts(zks);
        SpoutConfig spoutConf = new SpoutConfig(brokerHosts, topic, zkRoot, id);
        spoutConf.scheme = new SchemeAsMultiScheme(new StringScheme());

        spoutConf.zkServers = Arrays.asList(new String[] { "10.1.69.11", "10.1.69.12", "10.1.69.13"});
        spoutConf.zkPort = 2181;

        //数据从Kafka读取
        builder.setSpout("spout", new KafkaSpout(spoutConf), 4);
        //定义滑动时间窗口为10 MINUTES
        builder.setBolt("slidingwindowbolt",
                new SlidingWindowBolt().withWindow(new Duration(10, TimeUnit.SECONDS), new Duration(10, TimeUnit.SECONDS)),
                1).shuffleGrouping("spout");
//        builder.setBolt("kafkabolt", new KafkaBolt()).shuffleGrouping("slidingwindowbolt");

        Config conf = new Config();
        conf.setDebug(true);
        conf.setNumWorkers(1);

        try {
            StormSubmitter.submitTopologyWithProgressBar(args[0], conf, builder.createTopology());
        } catch (AlreadyAliveException e) {
            e.printStackTrace();
        } catch (InvalidTopologyException e) {
            e.printStackTrace();
        } catch (AuthorizationException e) {
            e.printStackTrace();
        }
    }
}
