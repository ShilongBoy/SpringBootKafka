package drools;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.io.ResourceFactory;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.StatelessKnowledgeSession;

public class DroolsTester {

    public static void main(String[] args) throws Exception {
            try {

                long start=System.currentTimeMillis();
                // load up the knowledge base
                KnowledgeBase kbase = readKnowledgeBase();
                StatelessKnowledgeSession ksession = kbase.newStatelessKnowledgeSession();//创建会话
                // go !
                Message message = new Message();
                message.setMessage("GoodBye");
                message.setStatus(Message.GOODBYE);
                ksession.execute(message);
                long end=System.currentTimeMillis();

                System.out.println("end-start:"+String.valueOf(end-start) +" \t message"+message);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        private static KnowledgeBase readKnowledgeBase() throws Exception {
            KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();//创建规则构建器
            kbuilder.add(ResourceFactory.newClassPathResource("Sample.drl"), ResourceType.DRL);//加载规则文件，并增加到构建器
            KnowledgeBuilderErrors errors = kbuilder.getErrors();
            if (errors.size() > 0) {//编译规则过程中发现规则是否有错误
                for (KnowledgeBuilderError error: errors) {System.out.println("规则中存在错误，错误消息如下：");
                    System.err.println(error);
                }
                throw new IllegalArgumentException("Could not parse knowledge.");
            }
            KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();//创建规则构建库
            kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());//构建器加载的资源文件包放入构建库
            return kbase;
        }

        public static class Message {

            public static final int HELLO = 0;
            public static final int GOODBYE = 1;

            private String message;

            private int status;

            public String getMessage() {
                return this.message;
            }

            public void setMessage(String message) {
                this.message = message;
            }

            public int getStatus() {
                return this.status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            @Override
            public String toString() {
                return "Message{" +
                        "message='" + message + '\'' +
                        ", status=" + status +
                        '}';
            }
        }
    }

