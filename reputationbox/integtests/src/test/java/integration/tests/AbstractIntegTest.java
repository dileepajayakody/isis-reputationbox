package integration.tests;

import integration.SimpleAppSystemInitializer;

import java.util.List;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.services.wrapper.WrapperFactory;
import org.apache.isis.core.integtestsupport.IsisSystemForTest;
import org.apache.isis.core.wrapper.WrapperFactoryDefault;
import org.junit.Before;
import org.junit.Rule;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.services.ContextIOService;

public abstract class AbstractIntegTest {

    protected List<UserMailBox> toDoItems;
    protected WrapperFactory wrapperFactory;
    protected DomainObjectContainer container;

   
    public IsisSystemForTest bootstrapIsis ;
       
    @Before
    public void init() {
    	bootstrapIsis = SimpleAppSystemInitializer.initIsft();
        wrapperFactory = bootstrapIsis.getService(WrapperFactoryDefault.class);
        container = bootstrapIsis.getContainer();
    }

    protected <T> T wrap(T obj) {
        return wrapperFactory.wrap(obj);
    }

    protected <T> T unwrap(T obj) {
        return wrapperFactory.unwrap(obj);
    }

    // other boilerplate omitted
}    