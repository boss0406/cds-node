package org.cds.main.blockchain.plot;

import java.io.File;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PlotFiles {
	
	private static Logger logger = LoggerFactory.getLogger("PlotFiles");
	
    private ArrayList<PlotFile> mPlotFiles;
    private String mPath;
    private String mNumericID;	

    public PlotFiles(String path, String numericID) {
        mPath = path;
        mNumericID = numericID;
        mPlotFiles = new ArrayList<PlotFile>();
        refreshPlotFiles();
    }

    // Returns the number of plots
    public int size() { return mPlotFiles.size(); }

    // Delete the last plot in the list
    public void deletePlot() {
        if (mPlotFiles.size() > 0) {
            String mDeleteFile = "";
            int mHighestNonce = 0;
            for (PlotFile mPF : mPlotFiles) {
                String mWorkingFile = mPF.getFileName();
                String[] mParts = mWorkingFile.split("_");
                int mCheckingNonce = Integer.parseInt(mParts[1]);
                if (mCheckingNonce >= mHighestNonce) {
                    mHighestNonce = mCheckingNonce;
                    mDeleteFile = mWorkingFile;
                    logger.debug("New Highest Found:" + mWorkingFile);
                }
            }
            File file = new File(mPath+'/'+mDeleteFile);
            logger.debug("We deleted:"+mPath+'/'+mDeleteFile);
            file.delete();
        }
        refreshPlotFiles();
    }

    // refresh the PlotFiles from external
    public void rescan() {
        refreshPlotFiles();
    }

    public ArrayList<PlotFile> getPlotFiles() {
        return mPlotFiles;
    }
    // Internal refresh the PlotFiles worker
    private void refreshPlotFiles() {
        mPlotFiles = null;
        mPlotFiles = new ArrayList<PlotFile>();
        String workingFileName = "";
        logger.debug("Files Path: {} ", mPath);
        File f = new File(mPath);
        File file[] = f.listFiles();
        // Maybe people are trying to open Mining before Plotting and we have no files
        try {
        	if(file == null) {
        		logger.debug("Files Size: {} ", 0);
        		return;
        	}
        	logger.debug("Files Size: {} ", file.length);
            for (int i = 0; i < file.length; i++) {
                workingFileName = file[i].getName();
                logger.debug("FileName:" + workingFileName);
                if (workingFileName.contains(mNumericID)) {
                    mPlotFiles.add(new PlotFile(this.mPath,workingFileName)); // Put it on the stack if it starts with numericID
                    logger.debug("Found Plot:" + workingFileName);
                }
            }
        }  catch (NullPointerException e) {
            logger.error("NULL Pointer Caught",e);
        }
    }

}
