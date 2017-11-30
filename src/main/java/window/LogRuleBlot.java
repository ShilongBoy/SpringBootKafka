package window;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatelessKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.AppCardInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

public class LogRuleBlot extends BaseRichBolt {
    private static final long serialVersionUID = 1258L;
    Logger logger= LoggerFactory.getLogger(LogRuleBlot.class);
    private StatelessKnowledgeSession ksession;
    private String driFile;
    private OutputCollector collector;

    public LogRuleBlot(String driFile){
        this.driFile=driFile;
    }

    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector=outputCollector;
        KnowledgeBuilder kbuilder= KnowledgeBuilderFactory.newKnowledgeBuilder();
        try {
            kbuilder.add(ResourceFactory.newInputStreamResource(new FileInputStream(new File(driFile))), ResourceType.DRL);
        } catch (FileNotFoundException e) {
            logger.info(e.getMessage());
        }
        KnowledgeBase kbase= KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        ksession=kbase.newStatelessKnowledgeSession();

    }

    public void execute(Tuple tuple) {
        AppCardInfo appCardInfo=(AppCardInfo)tuple.getValueByField("message");

    }

    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }
}
