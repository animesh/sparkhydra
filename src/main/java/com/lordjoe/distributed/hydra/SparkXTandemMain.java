package com.lordjoe.distributed.hydra;

import com.lordjoe.distributed.*;
import org.systemsbiology.xtandem.*;
import org.systemsbiology.xtandem.hadoop.*;

import java.io.*;

/**
 * com.lordjoe.distributed.hydra.SparkXTandemMain
 * Override to use spark files not local
 * User: Steve
 * Date: 10/21/2014
 */
public class SparkXTandemMain extends XTandemMain {

    public SparkXTandemMain(final InputStream is, final String url) {
        super(is, url);
        addOpener(new SparkFileOpener(this));
        String pathPrepend = SparkUtilities.getSparkProperties().getProperty("com.lordjoe.distributed.PathPrepend");

        if (pathPrepend != null) {
            System.err.println("Setting default path " + pathPrepend);
            XTandemHadoopUtilities.setDefaultPath(pathPrepend);
            setParameter("com.lordjoe.distributed.PathPrepend",pathPrepend);
        }
    }

    /**
     * open a file from a string
     *
     * @param fileName  string representing the file
     * @param otherData any other required data
     * @return possibly null stream
     */
    @Override
    public InputStream open(final String fileName, final Object... otherData) {
        return super.open(fileName, otherData);
    }


}
