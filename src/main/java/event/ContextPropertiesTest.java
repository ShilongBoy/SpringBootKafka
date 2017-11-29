package event;

import com.espertech.esper.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextPropertiesTest {

    private static Logger logger= LoggerFactory.getLogger(ContextPropertiesTest.class);
    public static void main(String[] args)
    {
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider();
        EPAdministrator admin = epService.getEPAdministrator();
        EPRuntime runtime = epService.getEPRuntime();

        String personEvent=PersonEvent.class.getName();
        logger.info("---personEvent---:"+personEvent);

        String epl1="select cast(avg(price),int) from PersonEvent.win.length_batch(2)";
        String epl2="select avg(price) from "+ personEvent +".win.length_batch(2)";

        EPStatement statementState=admin.createEPL(epl1);
        statementState.addListener(new ContextPropertiesListener());

        PersonEvent p1=new PersonEvent();
        p1.setPrice(10);
        runtime.sendEvent(p1);

    }
}
