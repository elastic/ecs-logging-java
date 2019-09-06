package co.elastic.logging.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class IntegrationTest {

    @BeforeEach
    public void init() throws IOException {
//        ConfigurationSource source = new ConfigurationSource(new FileInputStream("log4j2.properties"));
        Configurator.initialize(null, "log4j2.properties");
    }

    @Test
    public void testxxx(){
        Logger logger = LogManager.getLogger(getClass());
        LoggerContext.getContext().getConfiguration().getProperties().put("node.id", "foo");
        final LoggerContext ctx = LoggerContext.getContext();
//        ctx.reconfigure();

        logger.info("he");
        Logger ff = LogManager.getLogger("ff");
ff.info("ff");
    }
}
