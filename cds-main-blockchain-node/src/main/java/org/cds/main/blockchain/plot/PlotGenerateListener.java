package org.cds.main.blockchain.plot;

import java.util.Collection;

public interface PlotGenerateListener {
	public enum PlotGenerateStatus{
		PLOT_GENERATE_START,
		PLOT_GENERATE_SUCCESS,
		PLOT_GENERATE_ERROR
	}
	
	public class GenerateFileResult{
		private boolean plotted;
		private PlotFile plotFile;
		private String message;
		
		public GenerateFileResult(boolean plotted, PlotFile plotFile, String message) {
			this.plotted = plotted;
			this.plotFile = plotFile;
			this.message = message;
		}

		public boolean isPlotted() {
			return plotted;
		}


		public PlotFile getPlotFile() {
			return plotFile;
		}


		public String getMessage() {
			return message;
		}

	}
	
	public void onPlotGenerateStatusUpdate(PlotGenerateStatus status,GenerateFileResult result);
	
	public void onPlotGenerateReady();
	
	static void firePlotGenerateStatusUpdate(PlotGenerateStatus status,GenerateFileResult result,Collection<PlotGenerateListener> listeners){
		if(listeners!=null && listeners.size()>0){
			for(PlotGenerateListener listener : listeners){
				listener.onPlotGenerateStatusUpdate(status, result);
			}
		}
	}
	
	static void firePlotGenerateReady(Collection<PlotGenerateListener> listeners){
		if(listeners!=null && listeners.size()>0){
			for(PlotGenerateListener listener : listeners){
				listener.onPlotGenerateReady();
			}
		}
	}
}
