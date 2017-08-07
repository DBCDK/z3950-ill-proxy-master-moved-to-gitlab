package dk.dbc.z3950IllProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;

@ApplicationPath("")
public class Z3950Application extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z3950Application.class);

    private static final Set<Class<?>> classes = new HashSet<>();
    static {
        classes.add(Z3950Endpoint.class);
        for (Class<?> clazz : classes) {
            LOGGER.info("Registered {} resource", clazz.getName());
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
