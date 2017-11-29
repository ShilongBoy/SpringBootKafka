package event;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class ContextPropertiesListener implements UpdateListener {
    public void update(EventBean[] newEvents, EventBean[] oldEvents) {

        if (newEvents != null)
        {
            EventBean event = newEvents[0];
            System.out.println("Average Price: " + event.get("cast(avg(price), int)") + ", DataType is "
                    + event.get("cast(avg(price), int)").getClass().getName());
        }
    }

}

