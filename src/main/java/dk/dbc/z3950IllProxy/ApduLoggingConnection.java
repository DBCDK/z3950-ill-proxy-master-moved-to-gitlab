package dk.dbc.z3950IllProxy;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.yaz4j.ConnectionExtended;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Connection wrapper used to log catched APDU's on info level on connection close
 */
public class ApduLoggingConnection extends ConnectionExtended {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(ApduLoggingConnection.class);

    public ApduLoggingConnection(String host, int port) {
        super(host, port);
        this.option("saveAPDU","1");
    }

    @Override
    public void close() {
        splitAndLogSavedApdus(this.option("APDU") );
        super.close();
    }
    
    private void splitAndLogSavedApdus(String apdus) {
        if( ! LOGGER.isInfoEnabled() ) return ;

        Pattern p = Pattern.compile("\n[a-zA-Z]+Request \\{");
        Matcher m = p.matcher(apdus);
        int index = 0;
        while( m.find() ) {
            LOGGER.info(apdus.substring( index , m.start() ));
            index = m.start();
        }
        LOGGER.info(apdus.substring( index ));
    }
}
