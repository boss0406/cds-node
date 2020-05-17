package org.cds.main.blockchain.plot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlotFile {
	
	private static Logger logger = LoggerFactory.getLogger("PlotFile");
	
	private Set<PlotGenerateListener> listeners = new CopyOnWriteArraySet <>();
	
    public static Long NonceToComplete = 4096L;  // This will have to be 4096 in the end
    private String mPath;
    private String mFileName;       // Complete File Name
    private String mNumericID;      // User numeric IC
    private long mStart;            // Starting Nonce
    private long mStgr = 1;         // Stagger Size plotted with
    private long address;           // Long version of numericID

    public PlotFile(String plotPath,Collection<PlotGenerateListener> listeners) {
    	this.mPath = plotPath;
    	this.listeners.addAll(listeners);
    }

    public PlotFile(String plotPath,String fName) {
    	this.mPath = plotPath;
        mFileName = fName;
        String[] mParts = fName.split("_");
        mNumericID = mParts[0];
        address = parseUnsignedLong(mParts[0], 10);
        mStart = Long.parseLong(mParts[1]);
        mStgr = Long.parseLong(mParts[3]);
    }

    public void setNumericID(String numericID) {
        mNumericID = numericID;
        // Run through a Bigint incase the 2^64 is greated than signed Long
        BigInteger bigNumericID = new BigInteger(numericID);
        address = bigNumericID.longValue();
    }

    public void setStartNonce(long start) {mStart = start;}
    public String getFileName() {
        return this.mPath+File.separator+this.mFileName;
    }

    @SuppressWarnings("resource")
	public void plot() throws Exception {
        FileOutputStream out;
        mFileName = mNumericID + '_' + Long.toString(mStart) + '_' + Long.toString(new Long(NonceToComplete)) + '_' + Long.toString(mStgr);
        String mPlotFile = this.mPath + '/' + mFileName;
        try {
            logger.debug("Writing to:" + mPlotFile);
            out = new FileOutputStream(mPlotFile);
        } catch (Exception ioex) {
            throw ioex;
        }
        for (int mWorkingNonce = 0; mWorkingNonce < NonceToComplete; mWorkingNonce++) {
            SinglePlot plot = new SinglePlot(address, mStart + mWorkingNonce);
            logger.debug("Plotting Nonce #:" + mWorkingNonce + " of " + NonceToComplete);
            try {
                out.write(plot.data);
                out.flush();
            } catch (Exception e) {
                throw e;
            }
        }
        try{
            out.close();
        }catch(IOException ioex){
        	return;
        }
    }
    public static long parseUnsignedLong(String s, int radix)
            throws NumberFormatException {
        BigInteger b= new BigInteger(s,radix);
        if(b.bitLength()>64)
            throw new NumberFormatException(s+" is to big!");
        return b.longValue();
    }

    public long getStaggeramt() {
        return 1;
    }

    public long getStartnonce() {
        return mStart;
    }

    public long getAddress () {return address; }
}
