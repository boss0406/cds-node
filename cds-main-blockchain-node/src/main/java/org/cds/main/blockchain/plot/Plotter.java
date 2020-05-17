package org.cds.main.blockchain.plot;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.cds.main.blockchain.plot.PlotGenerateListener.GenerateFileResult;
import org.cds.main.blockchain.plot.PlotGenerateListener.PlotGenerateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Plotter {
	private static Logger logger = LoggerFactory.getLogger("Plotter");
    private String mNumericID = "";
    private PlotFiles mPlotFiles;
    private String mPath = "";
    
    private Set<PlotGenerateListener> listeners = new CopyOnWriteArraySet <>();
    
    public Plotter (String nID,String plotPath,Collection<PlotGenerateListener> listeners) {
        this.mNumericID = nID;
        mPath = plotPath;
        mPlotFiles = new PlotFiles(mPath, mNumericID);
        logger.debug("Plotter Inited without Callback");
        this.listeners.addAll(listeners);
        
        File mPathDir = new File(mPath);
        if(!mPathDir.exists() || !mPathDir.isDirectory()){
        	mPathDir.mkdirs();
        }
    }
    
    public Plotter (String nID,String plotPath,PlotGenerateListener listener) {
    	this(nID, plotPath, Collections.singleton(listener));
    }

    public void reload() {
        mPlotFiles.rescan();
    }

    public int getPlotSize() {
        return mPlotFiles.size();
    }

    public void delete1GB() {
        mPlotFiles.deletePlot();
    }
    
    public PlotFiles getPlotFiles(){
    	return this.mPlotFiles;
    }
    
    //aysn write
    public void plotGBs(int mGBs) {
        int mStartingGB = mPlotFiles.size();
    	PlotGenerateListener.firePlotGenerateStatusUpdate(PlotGenerateStatus.PLOT_GENERATE_START, null, listeners);
        for (int i = 0; i<mGBs; i++) {
        	PlotFile mNewPlot = new PlotFile(this.mPath,listeners);
        	try {
                mNewPlot.setNumericID(mNumericID);
                mNewPlot.setStartNonce((mStartingGB + i) * PlotFile.NonceToComplete);
                mNewPlot.plot();
                PlotGenerateListener.firePlotGenerateStatusUpdate(PlotGenerateStatus.PLOT_GENERATE_SUCCESS, 
                		new GenerateFileResult(true, mNewPlot, null), listeners);
            } catch (Exception e) {
                PlotGenerateListener.firePlotGenerateStatusUpdate(PlotGenerateStatus.PLOT_GENERATE_ERROR, 
                		new GenerateFileResult(false, mNewPlot, e.getMessage()), listeners);
                logger.error("STACK TRACE:", e);
                break;
            }
        }
        this.reload();
        PlotGenerateListener.firePlotGenerateReady(listeners);
    }
    
    private static boolean syncCreating = false;
    
    private boolean hasFileError = false;
    
    public boolean isSyncCreating(){
    	return syncCreating;
    }
    
    private CountDownLatch gbLatch;
    
    //sync write
    public void plotGBs(int mGBs,int threadNum,List<PlotFile> rebuildFiles) {
    	if(syncCreating){
    		return;
    	}
    	syncCreating = true;
    	if(threadNum<=0){
    		threadNum = 3;
    	}
    	BlockingQueue<Runnable> executorQueue = new LinkedBlockingQueue<Runnable>();
        ExecutorService executor = new ThreadPoolExecutor(threadNum, threadNum, 0L,
                TimeUnit.MILLISECONDS, executorQueue, r -> new Thread(r, "Plotter create")
        );
        
        gbLatch = rebuildFiles == null?new CountDownLatch(mGBs):new CountDownLatch(mGBs+rebuildFiles.size());
        
        new Thread(()->{
        	logger.info("Plotting thread start");
        	PlotGenerateListener.firePlotGenerateStatusUpdate(PlotGenerateStatus.PLOT_GENERATE_START, null, listeners);
        	
        	if(rebuildFiles != null){
        		rebuildFiles.forEach((PlotFile plotFile)->{
        			executor.submit(new RebuildPlotFileThread(plotFile));
        		});
        	}
        	int mStartingGB = mPlotFiles.size();
        	for (int i = 0; i<mGBs; i++) {
            	executor.submit(new PlotGBThead(mStartingGB+i));
            }
        	try {
				gbLatch.await();
			} catch (Exception e) {
                logger.error("STACK TRACE:", e);
			}
        	reload();
        	executor.shutdown();
        	syncCreating = false;
        	if(gbLatch.getCount()==0 && !hasFileError){
        		PlotGenerateListener.firePlotGenerateReady(listeners);
        	}
        	logger.info("Plotting thread done");
        }).start();
    }
    
    private class RebuildPlotFileThread implements Runnable{
    	PlotFile plotFile;
    	
    	public RebuildPlotFileThread(PlotFile plotFile){
    		this.plotFile = plotFile;
    	}
    	
		@Override
		public void run() {
			if(plotFile == null){
				return;
			}
			try {
				File file = new File(plotFile.getFileName());
				if(file.exists() && file.isFile()){
					file.delete();
				}
				plotFile.plot();
			}catch (Exception e) {
            	logger.error("STACK TRACE:", e);
            	hasFileError = true;
                PlotGenerateListener.firePlotGenerateStatusUpdate(PlotGenerateStatus.PLOT_GENERATE_ERROR, 
                		new GenerateFileResult(false, plotFile, e.getMessage()), listeners);
            }
			gbLatch.countDown();
		}
    	
    }
    
    private class PlotGBThead implements Runnable {
    	int mStartingGB;
    	public PlotGBThead(int mStartingGB){
    		this.mStartingGB = mStartingGB;
    	}
    	
		public void run() {
			PlotFile mNewPlot = new PlotFile(mPath,listeners);
			try {
                mNewPlot.setNumericID(mNumericID);
                mNewPlot.setStartNonce(mStartingGB * PlotFile.NonceToComplete);
                mNewPlot.plot();
                PlotGenerateListener.firePlotGenerateStatusUpdate(PlotGenerateStatus.PLOT_GENERATE_SUCCESS, 
                		new GenerateFileResult(true, mNewPlot, null), listeners);
            } catch (Exception e) {
            	logger.error("STACK TRACE:", e);
            	hasFileError = true;
                PlotGenerateListener.firePlotGenerateStatusUpdate(PlotGenerateStatus.PLOT_GENERATE_ERROR, 
                		new GenerateFileResult(false, mNewPlot, e.getMessage()), listeners);
            }
			gbLatch.countDown();
		}
	}
    
     
}
