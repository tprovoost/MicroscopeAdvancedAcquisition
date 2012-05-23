package plugins.tprovoost.Microscopy.MicroscopeAdvancedAcquisition.wrapper;

import icy.canvas.Canvas3D;
import icy.canvas.IcyCanvas;
import icy.file.Saver;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import loci.formats.FormatException;
import mmcorej.CMMCore;
import mmcorej.Configuration;
import mmcorej.PropertySetting;
import mmcorej.StrVector;
import mmcorej.TaggedImage;

import org.micromanager.api.AcquisitionEngine;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.ImageCache;
import org.micromanager.api.ScriptInterface;
import org.micromanager.navigation.MultiStagePosition;
import org.micromanager.navigation.PositionList;
import org.micromanager.navigation.StagePosition;
import org.micromanager.utils.AutofocusManager;
import org.micromanager.utils.ChannelSpec;
import org.micromanager.utils.ContrastSettings;
import org.micromanager.utils.MMException;
import org.micromanager.utils.MemoryUtils;
import org.micromanager.utils.NumberUtils;
import org.micromanager.utils.ReportingUtils;

import plugins.tprovoost.Microscopy.MicroManagerForIcy.MMMainFrame;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopeCore;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePluginAcquisition;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopeSequence;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.Tools.ImageGetter;

public class MMAcquisitionEngineMT implements AcquisitionEngine, ViewerListener {

	protected MicroscopeSequence _actual_seq;
	protected ArrayList<MicroscopeSequence> _list_seq = new ArrayList<MicroscopeSequence>();
	private MicroscopePluginAcquisition _plugin;
	private Integer _nbImagesAcquired = 0;
	private int _totalImages;
	private int _numFramesDone;
	protected String acqName_;
	private String rootName_;
	protected boolean singleFrame_;
	private boolean singleWindow_;
	private Timer acqTimer_;
	private AcqFrameTask acqTask_;
	private MultiFieldThread multiFieldThread_;
	private String fileSeparator_;
	protected String cameraConfig_;
	protected Configuration oldChannelState_;
	protected double oldExposure_;
	protected int _numFrames;
	protected int requestedNumFrames_;
	protected int afSkipInterval_;
	protected double frameIntervalMs_;
	protected int posCount_;
	protected boolean saveFiles_;
	protected boolean acquisitionLagging_;
	protected MicroscopeCore _core;
	protected PositionList posList_;
	protected ScriptInterface parentGUI_;
	protected String zStage_;
	protected ArrayList<ChannelSpec> _channels;
	protected ArrayList<ChannelSpec> _requestedChannels;
	protected double sliceDeltaZ_[];
	double bottomZPos_;
	double topZPos_;
	double deltaZ_;
	protected double startZPosUm_;
	boolean absoluteZ_;
	protected boolean useSliceSetting_;
	protected boolean keepShutterOpenForStack_;
	protected boolean keepShutterOpenForChannels_;
	private boolean useFramesSetting_;
	private boolean useChannelsSetting_;
	protected long imgWidth_;
	protected long imgHeight_;
	protected long imgDepth_;
	protected boolean useMultiplePositions_;
	protected int posMode_;
	int sliceMode_;
	protected boolean _pause;
	protected int previousPosIdx_;
	protected boolean acqInterrupted_;
	protected boolean oldLiveRunning_;
	protected boolean acqFinished_;
	private AutofocusManager afMgr_;
	private boolean autofocusEnabled_;
	private boolean continuousFocusingWasEnabled_;
	private boolean autofocusHasFailed_;
	private boolean originalAutoShutterSetting_;
	private boolean shutterIsOpen_;
	private String lastImageFilePath_;
	private boolean abortRequest_;
	private int _actualVolumeDistance3D = 0;

	public MMAcquisitionEngineMT(MicroscopePluginAcquisition plugin, MMMainFrame gui) {
		_plugin = plugin;
		singleFrame_ = false;
		singleWindow_ = false;
		cameraConfig_ = "";
		oldExposure_ = 10D;
		saveFiles_ = false;
		acquisitionLagging_ = false;
		absoluteZ_ = false;
		useSliceSetting_ = true;
		keepShutterOpenForStack_ = false;
		keepShutterOpenForChannels_ = false;
		useFramesSetting_ = false;
		imgWidth_ = 0L;
		imgHeight_ = 0L;
		imgDepth_ = 0L;
		posMode_ = 1;
		sliceMode_ = 0;
		_pause = false;
		autofocusEnabled_ = false;
		shutterIsOpen_ = false;
		_channels = new ArrayList<ChannelSpec>();
		_requestedChannels = new ArrayList<ChannelSpec>();
		sliceDeltaZ_ = new double[1];
		sliceDeltaZ_[0] = 0.0D;
		bottomZPos_ = 0.0D;
		topZPos_ = 0.0D;
		deltaZ_ = 0.0D;
		_numFrames = 1;
		afSkipInterval_ = 0;
		frameIntervalMs_ = 1.0D;
		_numFramesDone = 0;
		acqInterrupted_ = false;
		acqFinished_ = true;
		posCount_ = 0;
		rootName_ = new String("C:/AcquisitionData");
		posList_ = new PositionList();
		afMgr_ = null;
		fileSeparator_ = System.getProperty("file.separator");
		if (fileSeparator_ == null)
			fileSeparator_ = "/";
	}

	public void setCore(MicroscopeCore c, AutofocusManager _afMgr) {
		_core = c;
		afMgr_ = _afMgr;
		imgDepth_ = c.getBytesPerPixel();
	}

	public void setPositionList(PositionList posList) {
		posList_ = posList;
	}

	public ArrayList<ChannelSpec> getChannels() {
		return _requestedChannels;
	}

	public void setChannels(ArrayList<ChannelSpec> ch) {
		_requestedChannels = ch;
		if (_requestedChannels.size() == 0 || !useChannelsSetting_) {
			_channels = new ArrayList<ChannelSpec>();
			ChannelSpec cs = new ChannelSpec();
			try {
				cs.exposure_ = _core.getExposure();
			} catch (Exception e) {
				ReportingUtils.logError(e);
				cs.exposure_ = 10D;
			}
			_channels.add(cs);
		} else {
			_channels = _requestedChannels;
		}
	}

	public boolean setChannelGroup(String group) {
		if (groupIsEligibleChannel(group)) {
			try {
				_core.setChannelGroup(group);
			} catch (Exception e) {
				try {
					_core.setChannelGroup("");
				} catch (Exception ex) {
					ReportingUtils.showError(e);
				}
				return false;
			}
			return true;
		} else {
			return false;
		}
	}

	public String getChannelGroup() {
		return _core.getChannelGroup();
	}

	public String acquire() throws MMException {
		cleanup();
		_plugin.notifyAcquisitionStarted(true);
		_nbImagesAcquired = 0;
		_plugin.notifyProgress(0);
		zStage_ = _core.getFocusDevice();
		if (zStage_ == null)
			throw new MMException("No Z Stage !");
		_pause = false;
		autofocusHasFailed_ = false;

		if (isAcquisitionRunning())
			throw new MMException("Busy with the current acquisition.");
		_numFrames = useFramesSetting_ ? requestedNumFrames_ : 1;
		if (useMultiplePositions_ && (posList_ == null || posList_.getNumberOfPositions() < 1))
			throw new MMException("\"Multiple positions\" is selected but position list is not defined");
		if (posMode_ == 0 && !saveFiles_) {
			ReportingUtils.showMessage("To use \"Time first\" mode, you must check the box\nlabeled \"Save image files to acquisition directory.\"");
			return "";
		}
		oldChannelState_ = null;
		try {
			oldExposure_ = _core.getExposure();
			String channelConfig = _core.getCurrentConfig(_core.getChannelGroup());
			if (channelConfig.length() > 0)
				oldChannelState_ = _core.getConfigGroupState(_core.getChannelGroup());
			if (useChannelsSetting_ && cameraConfig_.length() > 0) {
				_core.getConfigState("Camera", cameraConfig_);
				_core.setConfig("Camera", cameraConfig_);
			}
			_core.waitForSystem();
		} catch (Exception e) {
			throw new MMException(e.getMessage());
		}
		if (autofocusEnabled_ && afMgr_.getDevice() == null)
			throw new MMException("Auto-focus module was not loaded.\nAuto-focus option can not be used in this context.");
		continuousFocusingWasEnabled_ = false;
		if (autofocusEnabled_)
			continuousFocusingWasEnabled_ = afMgr_.getDevice().isContinuousFocusEnabled();
		acquisitionLagging_ = false;
		posCount_ = 0;
		if (useMultiplePositions_) {
			if (posMode_ == 1) {
				startAcquisition();
			} else {
				multiFieldThread_ = new MultiFieldThread();
				multiFieldThread_.start();
			}
		} else {
			startAcquisition();
		}
		return "";
	}

	public void clear() {
		_channels.clear();
		_numFramesDone = 0;
		posCount_ = 0;
		_actual_seq = null;
		_list_seq.clear();
		wipeSequence();
	}

	public boolean addChannel(String config, double exp, Boolean doZStack, double zOffset, ContrastSettings c8, ContrastSettings c16, int skip, Color c) {
		if (isConfigAvailable(config)) {
			ChannelSpec channel = new ChannelSpec();
			channel.config_ = config;
			channel.exposure_ = exp;
			channel.doZStack_ = doZStack;
			channel.zOffset_ = zOffset;
			channel.contrast_ = c16;
			channel.color_ = c;
			channel.skipFactorFrame_ = skip;
			_requestedChannels.add(channel);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @deprecated Method addChannel is deprecated
	 */
	public boolean addChannel(String config, double exp, double zOffset, ContrastSettings c8, ContrastSettings c16, int skip, Color c) {
		return addChannel(config, exp, Boolean.valueOf(true), zOffset, c8, c16, skip, c);
	}

	public void setFrames(int numFrames, double deltaT) {
		requestedNumFrames_ = numFrames;
		frameIntervalMs_ = deltaT;
		_numFrames = useFramesSetting_ ? requestedNumFrames_ : 1;
	}

	public int getCurrentFrameCount() {
		return _numFramesDone;
	}

	public void setSlices(double bottom, double top, double zStep, boolean absolute) {
		absoluteZ_ = absolute;
		bottomZPos_ = bottom;
		topZPos_ = top;
		zStep = Math.abs(zStep);
		deltaZ_ = zStep;
		int numSlices = 0;
		if (Math.abs(zStep) >= getMinZStepUm())
			numSlices = (int) (Math.abs(top - bottom) / zStep + 0.5D) + 1;
		sliceDeltaZ_ = new double[numSlices];
		for (int i = 0; i < sliceDeltaZ_.length; i++)
			if (topZPos_ > bottomZPos_)
				sliceDeltaZ_[i] = bottom + deltaZ_ * (double) i;
			else
				sliceDeltaZ_[i] = bottom - deltaZ_ * (double) i;

		if (numSlices == 0) {
			sliceDeltaZ_ = new double[1];
			sliceDeltaZ_[0] = 0.0D;
		}
	}

	public int getNumSlices() {
		return sliceDeltaZ_.length;
	}

	public void setZStageDevice(String label) {
		zStage_ = label;
	}

	public void setComment(String txt) {
	}

	public String getFirstConfigGroup() {
		if (_core == null)
			return new String("");
		String groups[] = getAvailableGroups();
		if (groups == null || groups.length < 1)
			return new String("");
		else
			return getAvailableGroups()[0];
	}

	public String[] getChannelConfigs() {
		if (_core == null)
			return new String[0];
		else
			return _core.getAvailableConfigs(_core.getChannelGroup()).toArray();
	}

	public boolean isConfigAvailable(String config) {
		StrVector vcfgs = _core.getAvailableConfigs(_core.getChannelGroup());
		for (int i = 0; (long) i < vcfgs.size(); i++)
			if (config.compareTo(vcfgs.get(i)) == 0)
				return true;

		return false;
	}

	public String[] getCameraConfigs() {
		if (_core == null)
			return new String[0];
		StrVector vcfgs = _core.getAvailableConfigs("Camera");
		String cfgs[] = new String[(int) vcfgs.size()];
		for (int i = 0; i < cfgs.length; i++)
			cfgs[i] = vcfgs.get(i);

		return cfgs;
	}

	public int getNumFrames() {
		return requestedNumFrames_;
	}

	public double getFrameIntervalMs() {
		return frameIntervalMs_;
	}

	public double getSliceZBottomUm() {
		return bottomZPos_;
	}

	public double getSliceZStepUm() {
		return deltaZ_;
	}

	public double getZTopUm() {
		return topZPos_;
	}

	public void setChannel(int row, ChannelSpec channel) {
		_requestedChannels.set(row, channel);
	}

	public void setUpdateLiveWindow(boolean flag) {
	}

	public boolean isAcquisitionLagging() {
		return acquisitionLagging_;
	}

	public void setCameraConfig(String cfg) {
		cameraConfig_ = cfg;
	}

	public void startAcquisition() throws MMException {
		previousPosIdx_ = -1;
		acqInterrupted_ = false;
		acqFinished_ = false;
		_numFramesDone = 0;
		// Runtime.getRuntime().gc();
//		for (int i = 0; i < _channels.size(); i++) {
//			_channels.get(i).min_ = 65535D;
//			_channels.get(i).max_ = 0.0D;
//		}

		if (!saveFiles_ || saveFiles_ && !singleWindow_) {
			long freeBytes = MemoryUtils.freeMemory();
			int numSlices = useSliceSetting_ ? sliceDeltaZ_.length : 1;
			int numPositions = useMultiplePositions_ ? posList_.getNumberOfPositions() : 1;
			int numFrames = singleFrame_ ? 1 : _numFrames;
			long requiredBytes = (long) numSlices * (long) _channels.size() * (long) numFrames * (long) numPositions * _core.getImageWidth() * _core.getImageHeight()
					* _core.getBytesPerPixel() + 10000000L;
			// ReportingUtils.logMessage((new StringBuilder())
			// .append("Remaining memory ").append(freeBytes)
			// .append(" bytes. Required: ").append(requiredBytes)
			// .toString());
			if (freeBytes < requiredBytes) {
				JOptionPane.showMessageDialog(null, "Not enough memory to complete this MD Acquisition.");
				return;
			}
		}
		try {
			if (isFocusStageAvailable())
				startZPosUm_ = _core.getPosition(zStage_);
			else
				startZPosUm_ = 0.0D;
		} catch (Exception e) {
			ReportingUtils.showError(e);
			throw new MMException(e.getMessage());
		}
		acqTimer_ = new Timer();
		acqTask_ = new AcqFrameTask();
		if (frameIntervalMs_ < 1.0D)
			frameIntervalMs_ = 1.0D;
		if (_numFrames > 0)
			acqTimer_.schedule(acqTask_, 0L, (long) frameIntervalMs_);
	}

	public void acquireOneFrame(int posIdx) {
		int numSlices = useSliceSetting_ ? sliceDeltaZ_.length : 1;
		boolean abortWasRequested = false;
		int posIndexNormalized;
		if (!useMultiplePositions_ || posMode_ == 1)
			posIndexNormalized = posIdx;
		else
			posIndexNormalized = 0;
		try {
			MultiStagePosition pos = null;
			if (useMultiplePositions_)
				if (posIdx != previousPosIdx_) {
					pos = posList_.getPosition(posIdx);
					goToPosition(posIdx);
				} else {
					pos = posList_.getPosition(previousPosIdx_);
				}
			if (continuousFocusingWasEnabled_) {
				afMgr_.getDevice().fullFocus();
				afMgr_.getDevice().enableContinuousFocus(false);
			} else {
				performAutofocus(pos, posIdx);
			}
			boolean zStageMoves = useSliceSetting_;
			originalAutoShutterSetting_ = _core.getAutoShutter();
			if (sliceMode_ == 0) {
				// ------------
				// Slices first
				// -------------
				if (originalAutoShutterSetting_ && keepShutterOpenForChannels_)
					_core.setAutoShutter(false);
				int j = 0;
				do {
					if (j >= numSlices || !acqTask_.isRunning())
						break;
					double z = startZPosUm_;
					if (useSliceSetting_)
						if (absoluteZ_)
							z = sliceDeltaZ_[j];
						else
							z = startZPosUm_ + sliceDeltaZ_[j];
					if (isFocusStageAvailable() && numSlices > 1)
						_core.setPosition(zStage_, z);
					for (int k = 0; k < _channels.size() && acqTask_.isRunning(); k++) {
						ChannelSpec cs = _channels.get(k);
						if (abortRequest_)
							break;
						executeProtocolBody(cs, z, j, k, posIdx, numSlices, posIndexNormalized);
						if (cs.zOffset_ != 0.0D)
							zStageMoves = true;
					}

					if (abortRequest_) {
						abortRequest_ = false;
						abortWasRequested = true;
						break;
					}
					j++;
				} while (true);
			} else if (sliceMode_ == 1) {
				// ------------
				// Channels first
				// -------------
				if (originalAutoShutterSetting_ && keepShutterOpenForStack_)
					_core.setAutoShutter(false);
				int k = 0;
				do {
					if (k >= _channels.size())
						break;
					ChannelSpec cs = _channels.get(k);
					for (int j = 0; j < numSlices; j++) {
						double z = startZPosUm_;
						if (abortRequest_)
							break;
						if (useSliceSetting_)
							if (absoluteZ_)
								z = sliceDeltaZ_[j];
							else
								z = startZPosUm_ + sliceDeltaZ_[j];
						if (isFocusStageAvailable() && numSlices > 1)
							_core.setPosition(zStage_, z);
						_core.waitForDevice(zStage_);
						executeProtocolBody(cs, z, j, k, posIdx, numSlices, posIndexNormalized);
						if (cs.zOffset_ != 0.0D)
							zStageMoves = true;
					}

					if (abortRequest_) {
						abortRequest_ = false;
						abortWasRequested = true;
						break;
					}
					k++;
				} while (true);
			} else {
				throw new MMException((new StringBuilder()).append("Unrecognized slice mode: ").append(sliceMode_).toString());
			}
			if (originalAutoShutterSetting_) {
				_core.setAutoShutter(true);
				if (_core.getShutterOpen())
					_core.setShutterOpen(false);
			}
			if (isFocusStageAvailable() && zStageMoves) {
				_core.setPosition(zStage_, startZPosUm_);
				_core.waitForDevice(zStage_);
			}
			if (autofocusEnabled_ && continuousFocusingWasEnabled_)
				afMgr_.getDevice().enableContinuousFocus(continuousFocusingWasEnabled_);
			/*
			 * } catch (MMException e) { terminate(); e.printStackTrace();
			 * return;
			 */
		} catch (OutOfMemoryError e) {
			terminate();
			new AnnounceFrame("Out of memory - acquisition stopped.\nIn the future you can try to increase the amount of \nmemory available to the Java VM (ImageJ).");
			return;
			/*
			 * } catch (IOException e) { terminate();
			 * System.out.println("IOException"); e.printStackTrace(); return; }
			 * catch (JSONException e) { terminate();
			 * System.out.println("JSONException"); e.printStackTrace(); return;
			 */
		} catch (Exception e) {
			terminate();
			// System.out.println("Exception");
			// e.printStackTrace();
			return;
		}
		if (abortWasRequested || _numFramesDone >= _numFrames)
			terminate();
	}

	public void terminate() {
		_actualVolumeDistance3D = 0;
		for (int i = 0; i < _actual_seq.getViewers().size(); ++i) {
			final Viewer v = _actual_seq.getViewers().get(i);
			v.addListener(MMAcquisitionEngineMT.this);
			if (v.getCanvas() instanceof Canvas3D) {
				ThreadUtil.invokeNow(new Runnable() {

					@Override
					public void run() {
						((Canvas3D) v.getCanvas()).setVolumeDistanceSample(_actualVolumeDistance3D);
					}
				});
			}
		}
		try {
			_plugin.notifyAcquisitionOver();
			stop(false);
			restoreSystem();
			acqFinished_ = true;
			if (posMode_ == 0)
				;
		} catch (Throwable tt) {
			ReportingUtils.showError(tt.getMessage());
		}
	}

	protected int getAvailablePosIndex(int posIndexNormalized) {
		int index = 0;
		if (null != _actual_seq && 0 < _actual_seq.getNumImage())
			index = (null == _actual_seq.getImages(posIndexNormalized) ? 0 : posIndexNormalized);
		return index;
	}

	public static boolean saveImageFile(String fname, Object img, int width, int height) {
		System.out.println("save !");
		IcyBufferedImage ip;
		if (img instanceof byte[]) {
			try {
				ip = new IcyBufferedImage(width, height, 1, DataType.UBYTE);
				ip.setDataXY(0, img);
			} catch (Exception e) {
				if (e.toString().contains("cannot be cast to")) {
					ip = new IcyBufferedImage(width, height, 1, DataType.BYTE);
					ip.setDataXY(0, img);
				}
				return false;
			}
		} else if (img instanceof short[]) {
			ip = new IcyBufferedImage(width, height, 1, DataType.USHORT);
			ip.setDataXY(0, img);
		} else {
			return false;
		}
		try {
			Saver.saveImage(ip, new File(fname), true);
		} catch (FormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void restoreSystem() {
		try {
			_core.setExposure(oldExposure_);
			if (isFocusStageAvailable() && (afMgr_.getDevice() == null || !afMgr_.getDevice().isContinuousFocusEnabled())) {
				_core.setPosition(zStage_, startZPosUm_);
				_core.waitForDevice(zStage_);
			}
			if (oldChannelState_ != null) {
				_core.setSystemState(oldChannelState_);
				_core.waitForSystem();
			}
			if (autofocusEnabled_)
				afMgr_.getDevice().enableContinuousFocus(continuousFocusingWasEnabled_);
			_core.waitForSystem();
		} catch (Exception e) {
			// ReportingUtils.logError(e);
		}
	}

	public String getVerboseSummary() {
		int numFrames = useFramesSetting_ ? requestedNumFrames_ : 1;
		int numSlices = useSliceSetting_ ? sliceDeltaZ_.length : 1;
		int numPositions = useMultiplePositions_ ? Math.max(1, posList_.getNumberOfPositions()) : 1;
		int numChannels = useChannelsSetting_ ? _requestedChannels.size() : 1;
		_totalImages = numFrames * numSlices * numChannels * numPositions;
		double totalDurationSec = (frameIntervalMs_ * (double) numFrames) / 1000D;
		int hrs = (int) (totalDurationSec / 3600D);
		double remainSec = totalDurationSec - (double) (hrs * 3600);
		int mins = (int) (remainSec / 60D);
		remainSec -= mins * 60;
		Runtime rt = Runtime.getRuntime();
		rt.gc();
		String txt = (new StringBuilder()).append("Number of time points: ").append(numFrames).append("\nNumber of positions: ").append(numPositions)
				.append("\nNumber of slices: ").append(numSlices).append("\nNumber of channels: ").append(numChannels).append("\nTotal images: ").append(_totalImages)
				.append("\nDuration: ").append(hrs).append("h ").append(mins).append("m ").append(NumberUtils.doubleToDisplayString(remainSec)).append("s").toString();
		String order = "\nOrder: ";
		String ptSetting = null;
		if (useMultiplePositions_ && useFramesSetting_) {
			if (posMode_ == 1)
				ptSetting = "Position, Time";
			else if (posMode_ == 0)
				ptSetting = "Time, Position";
		} else if (useMultiplePositions_ && !useFramesSetting_)
			ptSetting = "Position";
		else if (!useMultiplePositions_ && useFramesSetting_)
			ptSetting = "Time";
		String csSetting = null;
		if (useSliceSetting_ && useChannelsSetting_) {
			if (sliceMode_ == 0)
				csSetting = "Channel, Slice";
			else
				csSetting = "Slice, Channel";
		} else if (useSliceSetting_ && !useChannelsSetting_)
			csSetting = "Slice";
		else if (!useSliceSetting_ && useChannelsSetting_)
			csSetting = "Channel";
		if (ptSetting == null && csSetting == null)
			order = "";
		else if (ptSetting != null && csSetting == null)
			order = (new StringBuilder()).append(order).append(ptSetting).toString();
		else if (ptSetting == null && csSetting != null)
			order = (new StringBuilder()).append(order).append(csSetting).toString();
		else if (ptSetting != null && csSetting != null)
			order = (new StringBuilder()).append(order).append(csSetting).append(", ").append(ptSetting).toString();
		return (new StringBuilder()).append(txt).append(order).toString();
	}

	public void stop(boolean interrupted) {
		_core.setAutoShutter(originalAutoShutterSetting_);
		try {
			if (_core.getAutoShutter() && _core.getShutterOpen())
				_core.setShutterOpen(false);
		} catch (Throwable tt) {
		}
		acqInterrupted_ = interrupted;
		if (acqTask_ != null) {
			acqTask_.cancel();
			if (!acqInterrupted_)
				acqTask_.setRunning(false);
			waitForAcquisitionToStop();
		}
		if (_actual_seq == null)
			return;
	}

	public boolean isAcquisitionRunning() {
		if (acqTask_ == null)
			return false;
		else
			return acqTask_.isActive() || acqTask_.isRunning();
	}

	public boolean isMultiFieldRunning() {
		return multiFieldThread_.isAlive();
	}

	protected boolean isFocusStageAvailable() {
		return zStage_ != null && zStage_.length() > 0;
	}

	public void shutdown() {
		if (isAcquisitionRunning())
			stop(false);
		if (multiFieldThread_ != null)
			multiFieldThread_.interrupt();
		acqFinished_ = true;
	}

	protected void cleanup() {
		waitForAcquisitionToStop();
		_actual_seq = new MicroscopeSequence(ImageGetter.snapImage(_core));
		_list_seq.clear();
		abortRequest_ = false;
		_nbImagesAcquired = 0;
		_numFramesDone = 0;
	}

	private void waitForAcquisitionToStop() {
		while (isAcquisitionRunning())
			try {
				Thread.sleep(5L);
			} catch (InterruptedException ex) {
				ReportingUtils.logError(ex);
			}
	}

	public double getCurrentZPos() {
		if (isFocusStageAvailable()) {
			double z = 0.0D;
			try {
				z = _core.getPosition(_core.getFocusDevice());
			} catch (Exception e) {
				ReportingUtils.showError(e);
			}
			return z;
		} else {
			return 0.0D;
		}
	}

	public double getMinZStepUm() {
		return 0.01D;
	}

	public void setDirName(String dirName_) {
		acqName_ = dirName_;
	}

	public String getDirName() {
		return acqName_;
	}

	public void setRootName(String rootName_) {
		this.rootName_ = rootName_;
	}

	public String getRootName() {
		return rootName_;
	}

	public void setSaveFiles(boolean saveFiles) {
		saveFiles_ = saveFiles;
	}

	public boolean getSaveFiles() {
		return saveFiles_;
	}

	public void setSingleFrame(boolean singleFrame) {
		singleFrame_ = singleFrame;
	}

	public void setSingleWindow(boolean singleWindow) {
		singleWindow_ = singleWindow;
	}

	public void setDisplayMode(int mode) {
		if (mode == 0) {
			singleFrame_ = false;
			singleWindow_ = false;
		} else if (mode == 1) {
			singleFrame_ = true;
			singleWindow_ = false;
		} else if (mode == 2) {
			singleFrame_ = false;
			singleWindow_ = true;
		}
	}

	public int getDisplayMode() {
		if (singleFrame_)
			return 1;
		return !singleWindow_ ? 0 : 2;
	}

	public void setParameterPreferences(Preferences prefs) {
	}

	protected void acquisitionDirectorySetup(int posIdx) throws IOException, MMException {
		// System.out.println("acquisitionDirectorySetup");
		/*
		 * if (useMultiplePositions_) { if (posMode_ == 1) { acqData_ = new
		 * AcquisitionData[posList_.getNumberOfPositions()]; for (int i = 0; i <
		 * acqData_.length; i++) if (saveFiles_) acqData_[i] =
		 * well_.createNewImagingSite(posList_ .getPosition(i).getLabel(),
		 * false); else acqData_[i] = well_.createNewImagingSite();
		 * 
		 * } else { acqData_ = new AcquisitionData[1]; if (saveFiles_)
		 * acqData_[0] = well_.createNewImagingSite(posList_
		 * .getPosition(posIdx).getLabel(), false); else acqData_[0] =
		 * well_.createNewImagingSite(); } } else { acqData_ = new
		 * AcquisitionData[1]; acqData_[0] = new AcquisitionData(); if
		 * (saveFiles_) acqData_[0].createNew(acqName_, rootName_, true); else
		 * acqData_[0].createNew(); } if (saveFiles_) { for (int i = 0; i <
		 * acqData_.length; i++) { FileOutputStream os = new FileOutputStream(
		 * (new StringBuilder()).append(acqData_[i].getBasePath())
		 * .append("/").append("Acqusition.xml") .toString()); if (prefs_ ==
		 * null) continue; try { prefs_.exportNode(os); } catch
		 * (BackingStoreException e) { ReportingUtils.showError(e); } }
		 * 
		 * } for (int i = 0; i < imgData_.length; i++) { int index = null ==
		 * imgData_[i] ? 0 : i; acqData_[i].setImagePhysicalDimensions( (int)
		 * imgData_[index].imgWidth_, (int) imgData_[index].imgHeight_, (int)
		 * imgData_[index].imgDepth_); acqData_[i].setDimensions(0,
		 * channels_.size(), useSliceSetting_ ? sliceDeltaZ_.length : 1);
		 * acqData_[i].setComment(comment_);
		 * acqData_[i].setPixelSizeUm(pixelSize_um_);
		 * acqData_[i].setImageIntervalMs(frameIntervalMs_);
		 * acqData_[i].setSummaryValue("z-step_um", Double.toString(deltaZ_));
		 * acqData_[i].setSummaryValue("PixelAspect",
		 * Double.toString(pixelAspect_)); for (int j = 0; j < channels_.size();
		 * j++) { Color c = ((ChannelSpec) channels_.get(j)).color_;
		 * acqData_[i].setChannelColor(j, c.getRGB());
		 * acqData_[i].setChannelName(j, ((ChannelSpec)
		 * channels_.get(j)).config_); }
		 * 
		 * if (!useMultiplePositions_) continue; MultiStagePosition mps =
		 * posList_.getPosition(i); acqData_[i].setSummaryValue("Position",
		 * mps.getLabel()); acqData_[i].setSummaryValue("GridRow",
		 * Integer.toString(mps.getGridRow()));
		 * acqData_[i].setSummaryValue("GridColumn",
		 * Integer.toString(mps.getGridColumn())); String keys[] =
		 * mps.getPropertyNames(); for (int k = 0; k < keys.length; k++) {
		 * acqData_[i].setPositionProperty(keys[k], mps.getProperty(keys[k]));
		 * acqData_[i].setSummaryValue(keys[k], mps.getProperty(keys[k])); }
		 * 
		 * }
		 */
	}

	public void enableZSliceSetting(boolean b) {
		useSliceSetting_ = b;
	}

	public boolean isZSliceSettingEnabled() {
		return useSliceSetting_;
	}

	public void enableMultiPosition(boolean b) {
		useMultiplePositions_ = b;
	}

	public boolean isMultiPositionEnabled() {
		return useMultiplePositions_;
	}

	public void wipeSequence() {
		_actual_seq = null;
		/*
		 * Object emptyPixels = image5D.createEmptyPixels(); int nc =
		 * image5D.getNChannels(); int ns = image5D.getNSlices(); int nf =
		 * image5D.getNFrames(); for (int c = 1; c <= nc; c++) { for (int f = 1;
		 * f <= nf; f++) { for (int s = 1; s <= ns; s++)
		 * image5D.setPixels(emptyPixels, c, s, f); } }
		 */
	}

	private boolean groupIsEligibleChannel(String group) {
		StrVector cfgs = _core.getAvailableConfigs(group);
		if (cfgs.size() == 1L)
			try {
				Configuration presetData = _core.getConfigData(group, cfgs.get(0));
				if (presetData.size() == 1L) {
					PropertySetting setting = presetData.getSetting(0L);
					String devLabel = setting.getDeviceLabel();
					String propName = setting.getPropertyName();
					if (_core.hasPropertyLimits(devLabel, propName))
						return false;
				}
			} catch (Exception ex) {
				ReportingUtils.logError(ex);
				return false;
			}
		return true;
	}

	public String[] getAvailableGroups() {
		StrVector groups;
		try {
			groups = _core.getAllowedPropertyValues("Core", "ChannelGroup");
		} catch (Exception ex) {
			ReportingUtils.logError(ex);
			return new String[0];
		}
		ArrayList<String> strGroups = new ArrayList<String>();
		for (int i = 0; (long) i < groups.size(); i++) {
			String group = groups.get(i);
			if (groupIsEligibleChannel(group))
				strGroups.add(group);
		}

		return strGroups.toArray(new String[0]);
	}

	protected long getImageWidth() {
		return _core.getImageWidth();
	}

	protected long getImageHeight() {
		return _core.getImageHeight();
	}

	/**
	 * Creation of the sequence
	 * 
	 * @param posIndex
	 * @throws MMException
	 */
	protected void setupSequence(int posIndex) throws MMException {
		imgWidth_ = getImageWidth();
		imgHeight_ = getImageHeight();
		imgDepth_ = _core.getBytesPerPixel();
		_core.getPixelSizeUm();
		if (singleWindow_ && posIndex > 0)
			return;
		int type;
		if (imgDepth_ == 1L)
			type = 0;
		else if (imgDepth_ == 2L)
			type = 1;
		else if (4L == imgDepth_)
			type = 4;
		else
			throw new MMException("Unsupported pixel depth");
		int numSlices = 1;
		if (useSliceSetting_)
			numSlices = sliceDeltaZ_.length;
		_actual_seq = createSequence(type, numSlices, _numFrames);
		Calendar calendar = Calendar.getInstance();
		String sequence_name = "" + calendar.get(Calendar.MONTH) + "_" + calendar.get(Calendar.DAY_OF_MONTH) + "_" + calendar.get(Calendar.YEAR) + "-"
				+ calendar.get(Calendar.HOUR_OF_DAY) + "_" + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND);
		if (useMultiplePositions_) {
			int x = (int) (100 * posList_.getPosition(posIndex).getX()) / 100;
			int y = (int) (100 * posList_.getPosition(posIndex).getY()) / 100;
			int z = (int) (100 * posList_.getPosition(posIndex).getZ()) / 100;
			sequence_name += "-" + x + "_" + y + "_" + z;
		}
		_actual_seq.setName(sequence_name);
		_list_seq.add(_actual_seq);
		Icy.addSequence(_actual_seq);
	}

	/**
	 * Creation of the sequence.
	 * 
	 * @param type
	 * @param numSlices
	 * @param numFrames
	 * @return
	 */
	protected MicroscopeSequence createSequence(int type, int numSlices, int numFrames) {
		MicroscopeSequence seq;
		if (!(type == 0 || type == 1)) {
			System.out.println("type" + type);
			return null;
		}
		IcyBufferedImage empty = new IcyBufferedImage((int) imgWidth_, (int) imgHeight_, _channels.size(), DataType.USHORT);
		seq = new MicroscopeSequence(empty);
		for (int i = 1; i < numFrames; ++i) {
			seq.addVolumetricImage();
		}
		for (int i = 0; i < _channels.size(); ++i) {
			seq.getColorModel().getColormap(i).setARGBControlPoint(0, Color.BLACK);
			seq.getColorModel().getColormap(i).setARGBControlPoint(255, _channels.get(i).color_);
		}
		return seq;
	}

	/*
	 * protected void setupImage5DArray() { seq = new
	 * Image5D[posList_.getNumberOfPositions()]; seq = new
	 * Image5DWindow[posList_.getNumberOfPositions()]; }
	 */

	public int getPositionMode() {
		return posMode_;
	}

	public int getSliceMode() {
		return sliceMode_;
	}

	public void setPositionMode(int mode) {
		posMode_ = mode;
	}

	public void setSliceMode(int mode) {
		sliceMode_ = mode;
	}

	public void keepShutterOpenForStack(boolean open) {
		keepShutterOpenForStack_ = open;
	}

	public boolean isShutterOpenForStack() {
		return keepShutterOpenForStack_;
	}

	public void keepShutterOpenForChannels(boolean open) {
		keepShutterOpenForChannels_ = open;
	}

	public boolean isShutterOpenForChannels() {
		return keepShutterOpenForChannels_;
	}

	public void enableAutoFocus(boolean enable) {
		autofocusEnabled_ = enable;
	}

	public boolean isAutoFocusEnabled() {
		return autofocusEnabled_;
	}

	public int getAfSkipInterval() {
		return afSkipInterval_;
	}

	public void setAfSkipInterval(int interval) {
		if (interval < 0)
			interval = 0;
		if (interval > _numFrames - 1)
			interval = _numFrames - 1;
		afSkipInterval_ = interval;
	}

	/**
	 * Execute the normal protocal
	 * 
	 * @param cs
	 *            : channel type
	 * @param z
	 *            : z position
	 * @param sliceIdx
	 *            : slice Index
	 * @param channelIdx
	 *            : channel Index
	 * @param posIdx
	 *            : position Index
	 * @param numSlices
	 *            : number of slices
	 * @param posIndexNormalized
	 * @throws Exception
	 */
	protected void executeProtocolBody(ChannelSpec cs, double z, int sliceIdx, int channelIdx, int posIdx, int numSlices, int posIndexNormalized) throws Exception {
		int actualFrameCount;
		// double exposureMs;
		IcyBufferedImage img;
		double zAbsolutePos;
		shutterIsOpen_ = false;
		try {
			shutterIsOpen_ = _core.getShutterOpen();
		} catch (Exception ex) {
			ReportingUtils.showError(ex);
		}
		actualFrameCount = singleFrame_ ? 0 : _numFramesDone;
		if (_numFramesDone > 0 && _numFramesDone % (Math.abs(cs.skipFactorFrame_) + 1) != 0) {
			fillInSkippedFrame(cs, sliceIdx, channelIdx, posIndexNormalized, actualFrameCount);
			return;
		}
		if (!cs.doZStack_.booleanValue() && sliceIdx > 0) {
			fillInSkippedSlice(sliceIdx, channelIdx, posIndexNormalized, actualFrameCount);
			return;
		}
		// exposureMs = cs.exposure_;
		img = null;
		zAbsolutePos = z;
		try {
			if (isFocusStageAvailable() && cs.zOffset_ != 0.0D && cs.config_.length() > 0) {
				_core.waitForDevice(zStage_);
				zAbsolutePos = z + cs.zOffset_;
				_core.setPosition(zStage_, zAbsolutePos);
				_core.waitForDevice(zStage_);
			}
			if (cs.config_.length() > 0) {
				_core.setConfig(_core.getChannelGroup(), cs.config_);
				_core.waitForConfig(_core.getChannelGroup(), cs.config_);
				_core.setExposure(cs.exposure_);
			}
			if (originalAutoShutterSetting_ && !_core.getAutoShutter() && !shutterIsOpen_) {
				if (sliceMode_ == 1 && keepShutterOpenForStack_ && sliceIdx == 0)
					setShutterOpen(true);
				if (sliceMode_ == 0 && keepShutterOpenForChannels_ && channelIdx == 0)
					setShutterOpen(true);
			}
			if (!acqTask_.isRunning())
				return;
		} catch (Exception e) {
			ReportingUtils.logError(e);
			throw new MMException(e.getMessage());
		}
		try {
			img = snapAndRetrieve();
		}
		// Misplaced declaration of an exception variable
		catch (Exception e11) {
			ReportingUtils.logError(e11);
			ReportingUtils.displayNonBlockingMessage((new StringBuilder()).append("acquisition snapAndRetrieve failed: ").append(e11.getMessage()).toString());
		}
		if (originalAutoShutterSetting_ && !_core.getAutoShutter() && shutterIsOpen_) {
			if (sliceMode_ == 1 && !keepShutterOpenForChannels_ && sliceIdx == numSlices - 1 && _core.getShutterOpen())
				setShutterOpen(false);
			if (sliceMode_ == 0 && !keepShutterOpenForStack_ && channelIdx == _channels.size() - 1 && _core.getShutterOpen())
				setShutterOpen(false);
		}
		long width = getImageWidth();
		long height = getImageHeight();
		long depth = _core.getBytesPerPixel();
		if (sliceIdx == 0 && channelIdx == 0)
			haveEnoughMemoryForFrame(numSlices, width, height, depth);
		if (imgDepth_ != depth)
			throw new MMException("Byte depth does not match between channels or slices");
		try {
			insertPixelsIntoSequence(sliceIdx, channelIdx, actualFrameCount, posIndexNormalized, img);
		} catch (Throwable t1) {
			System.out.println("Inserting the image failed : Actual Frame :" + actualFrameCount + "Slice : " + sliceIdx + "Position : " + posIndexNormalized);
		}
	}

	/*
	 * public void generateMetadata(double zCur, int sliceIdx, int channelIdx,
	 * int posIdx, int posIndexNormalized, double exposureMs, Object img) throws
	 * MMAcqDataException { if (acqData_ == null || acqData_.length == 0 ||
	 * acqData_[posIndexNormalized] == null) return; org.json.JSONObject state =
	 * Annotator.generateJSONMetadata(core_ .getSystemStateCache());
	 * acqData_[posIndexNormalized].insertImageMetadata(frameCount_, channelIdx,
	 * sliceIdx); acqData_[posIndexNormalized].setImageValue(frameCount_,
	 * channelIdx, sliceIdx, "Exposure-ms", exposureMs);
	 * acqData_[posIndexNormalized].setImageValue(frameCount_, channelIdx,
	 * sliceIdx, "Z-um", zCur); if (useMultiplePositions_) {
	 * acqData_[posIndexNormalized].setImageValue(frameCount_, channelIdx,
	 * sliceIdx, "X-um", posList_.getPosition(posIdx).getX());
	 * acqData_[posIndexNormalized].setImageValue(frameCount_, channelIdx,
	 * sliceIdx, "Y-um", posList_.getPosition(posIdx).getY()); }
	 * acqData_[posIndexNormalized].setSystemState(frameCount_, channelIdx,
	 * sliceIdx, state); if (saveFiles_) {
	 * acqData_[posIndexNormalized].attachImage(img, frameCount_, channelIdx,
	 * sliceIdx); lastImageFilePath_ = acqData_[posIndexNormalized]
	 * .getLastImageFilePath(); }
	 * 
	 * }
	 */

	private void fillInSkippedFrame(ChannelSpec cs, int sliceIdx, int channelIdx, int posIndexNormalized, int actualFrameCount) {
		/*
		 * if (!singleFrame_) { int offset = frameCount_ %
		 * (Math.abs(cs.skipFactorFrame_) + 1); int index = 0; if
		 * (seq[posIndexNormalized] != null) index = posIndexNormalized; Object
		 * previousImg = seq[index].getPixels(channelIdx + 1, sliceIdx + 1,
		 * (actualFrameCount + 1) - offset); if (previousImg != null)
		 * seq[0].setPixels(previousImg, channelIdx + 1, sliceIdx + 1,
		 * actualFrameCount + 1); }
		 */
		System.out.println("fillSkippedFrame");
	}

	private void fillInSkippedSlice(int sliceIdx, int channelIdx, int posIndexNormalized, int actualFrameCount) {
		/*
		 * if (!singleFrame_) { int index = 0; if (seq[posIndexNormalized] !=
		 * null) index = posIndexNormalized; if (sliceIdx > 0) { Object
		 * previousImg = seq[index].getPixels(channelIdx + 1, sliceIdx,
		 * actualFrameCount + 1); if (previousImg != null)
		 * seq[0].setPixels(previousImg, channelIdx + 1, sliceIdx + 1,
		 * actualFrameCount + 1); } }
		 */
		System.out.println("fillSkippedSlice");
	}

	private void haveEnoughMemoryForFrame(int numSlices, long width, long height, long depth) throws OutOfMemoryError {
		long freeBytes = MemoryUtils.freeMemory();
		long requiredBytes = ((long) numSlices * (long) _channels.size() + 10L) * (width * height * depth);
		_core.logMessage((new StringBuilder()).append("Remaining memory ").append(freeBytes).append(" bytes. Required: ").append(requiredBytes).toString());
		for (int tries = 0; freeBytes < requiredBytes && tries < 5; tries++) {
			System.gc();
			freeBytes = MemoryUtils.freeMemory();
		}

		if (freeBytes < requiredBytes)
			throw new OutOfMemoryError((new StringBuilder()).append("Remaining memory ").append(FMT2.format((double) freeBytes / 1048576D))
					.append(" MB. Required for the next step: ").append(FMT2.format((double) requiredBytes / 1048576D)).append(" MB").toString());
		else
			return;
	}

	/**
	 * Insert an IcyBufferedImage into the Sequence
	 * 
	 * @param sliceIdx
	 *            : slice index
	 * @param channelIdx
	 *            : channel index
	 * @param actualFrameCount
	 *            : frame index
	 * @param posIndexNormalized
	 *            : position index
	 * @param img
	 *            : image to add into the sequence
	 */
	protected void insertPixelsIntoSequence(int sliceIdx, int channelIdx, int actualFrameCount, int posIndexNormalized, IcyBufferedImage img) {
		if (posIndexNormalized >= _list_seq.size())
			try {
				setupSequence(posIndexNormalized);
			} catch (MMException e) {
				e.printStackTrace();
			}
		_actual_seq = _list_seq.get(posIndexNormalized);
		if (_actual_seq == null)
			return;
		try {
			IcyBufferedImage tmp_img = _actual_seq.getImage(actualFrameCount, sliceIdx);
			if (tmp_img == null) {
				tmp_img = new IcyBufferedImage(img.getWidth(), img.getHeight(), _channels.size(), DataType.USHORT);
				_actual_seq.setImage(actualFrameCount, sliceIdx, tmp_img);
			}
			tmp_img.setDataXYAsShort(channelIdx, img.getDataXYAsShort(0));
		} finally {
		}
		++_nbImagesAcquired;
		double progress = 1.0D * _nbImagesAcquired / _totalImages * 100;
		_plugin.notifyProgress((int) progress);

	}

	protected IcyBufferedImage snapAndRetrieve() throws Exception {
		return ImageGetter.snapImage(_core);
	}

	public void goToPosition(int posIndex) throws Exception {
		MultiStagePosition pos = posList_.getPosition(posIndex);
		try {
			MultiStagePosition.goToPosition(pos, _core);
			_core.waitForSystem();
		} catch (Exception e1) {
			throw e1;
		}
	}

	private void performAutofocus(MultiStagePosition pos, int posIdx) throws Exception {
		if (afMgr_ != null && afMgr_.getDevice() != null && autofocusEnabled_ && _numFramesDone % (afSkipInterval_ + 1) == 0) {
			if (pos != null && pos.hasProperty("AUTOFOCUS")) {
				if (pos.getProperty("AUTOFOCUS").equals("incremental"))
					afMgr_.getDevice().incrementalFocus();
				else if (pos.getProperty("AUTOFOCUS").equals("full"))
					attemptFullFocus();
				else if (!pos.getProperty("AUTOFOCUS").equals("none"))
					throw new MMException("Unrecognized Auto-focus property in position list");
			} else {
				attemptFullFocus();
			}
			if (pos != null) {
				double zFocus = _core.getPosition(zStage_);
				StagePosition sp = pos.get(zStage_);
				if (sp != null)
					sp.x = zFocus;
			}
			previousPosIdx_ = posIdx;
		}
	}

	public synchronized void setPause(boolean state) {
		_pause = state;
	}

	public synchronized boolean isPaused() {
		return _pause;
	}

	public void setFinished() {
		acqFinished_ = true;
		new AnnounceFrame("Acquisition is complete");
	}

	public String installAutofocusPlugin(String className) {
		return "Call to installAutofocusPlugin in MMAcquisitionEngine received. This method has been deprecated. Use MMStudioMainFrame.installAutofocusPlugin() instead.";
	}

	private void attemptFullFocus() {
		try {
			this.afMgr_.getDevice().fullFocus();
		} catch (final MMException e) {
			if (!this.autofocusHasFailed_) {
				this.autofocusHasFailed_ = true;
				new Thread() {
					public void run() {
						ReportingUtils.showError(e, "Autofocus has failed during this acquisition.");
					}
				}.start();
			}
		}
	}

	public boolean isFramesSettingEnabled() {
		return useFramesSetting_;
	}

	public void enableFramesSetting(boolean enable) {
		useFramesSetting_ = enable;
	}

	public boolean isChannelsSettingEnabled() {
		return useChannelsSetting_;
	}

	public void enableChannelsSetting(boolean enable) {
		useChannelsSetting_ = enable;
	}

	private void setShutterOpen(boolean b) {
		try {
			_core.setShutterOpen(b);
			_core.waitForDevice(_core.getShutterDevice());
		} catch (Exception ex) {
			ReportingUtils.showError(ex);
		}
		shutterIsOpen_ = b;
	}

	public String getLastImageFilePath() {
		return lastImageFilePath_;
	}

	public boolean abortRequest() {
		abortRequest_ = true;
		return abortRequest_;
	}

	@Override
	public boolean abortRequested() {
		return false;
	}

	@Override
	public boolean addChannel(String arg0, double arg1, Boolean arg2, double arg3, ContrastSettings arg4, ContrastSettings arg5, int arg6, Color arg7, boolean arg8) {
		return false;
	}

	@Override
	public void addImageProcessor(@SuppressWarnings("rawtypes") Class paramClass) {
	}

	@Override
	public void addImageProcessor(DataProcessor<TaggedImage> arg0) {
	}

	@Override
	public long getNextWakeTime() {
		return 0;
	}

	@Override
	public boolean isFinished() {
		return false;
	}

	@Override
	public void removeImageProcessor(@SuppressWarnings("rawtypes") Class paramClass) {
	}

	protected class RefreshI5d implements Runnable {

		public void run() {
			try {
				if (isAcquisitionRunning() && _actual_seq != null)
					_actual_seq.dataChanged();
			} catch (NullPointerException e) {
				if (_actual_seq != null)
					e.printStackTrace();
			}
		}
	}

	private class MultiFieldThread extends Thread {

		public void run() {
			posCount_ = 0;
			do {
				if (posCount_ >= posList_.getNumberOfPositions())
					break;
				if (isError())
					return;
				try {
					goToPosition(posCount_);
				} catch (Exception e1) {
					e1.printStackTrace();
					error_ = true;
					return;
				}
				try {
					startAcquisition();
				} catch (MMException e) {
					ReportingUtils.showError(e);
					error_ = true;
					return;
				}
				waitForAcquisitionToStop();
				if (acqInterrupted_)
					break;
				posCount_++;
			} while (true);
			_nbImagesAcquired = 0;
			if (_numFramesDone >= _numFrames)
				terminate();
		}

		@SuppressWarnings("unused")
		public void setError(boolean error_) {
			this.error_ = error_;
		}

		public boolean isError() {
			return error_;
		}

		private volatile boolean error_;

		private MultiFieldThread() {
			error_ = false;
		}

	}

	private class AcqFrameTask extends TimerTask {

		private boolean running_;
		private boolean active_;
		private boolean coreLogInitialized_;

		private AcqFrameTask() {
			running_ = false;
			active_ = true;
			coreLogInitialized_ = false;
		}

		public void run() {
			setRunning(true);
			_actualVolumeDistance3D = 4;
			for (int i = 0; i < _actual_seq.getViewers().size(); ++i) {
				final Viewer v = _actual_seq.getViewers().get(i);
				v.addListener(MMAcquisitionEngineMT.this);
				if (v.getCanvas() instanceof Canvas3D) {
					ThreadUtil.invokeNow(new Runnable() {

						@Override
						public void run() {
							((Canvas3D) v.getCanvas()).setVolumeDistanceSample(_actualVolumeDistance3D);
						}
					});
				}
			}
			if (!coreLogInitialized_) {
				_core.initializeLogging();
				coreLogInitialized_ = true;
			}
			if (_pause) {
				setRunning(false);
				return;
			}
			if (useMultiplePositions_ && posMode_ == 1) {
				for (int i = 0; i < posList_.getNumberOfPositions() && isRunning(); i++) {
					try {
						_actual_seq.beginUpdate();
						acquireOneFrame(i);
					} finally {
						_actual_seq.endUpdate();
					}
				}

			} else {
				try {
					_actual_seq.beginUpdate();
					acquireOneFrame(posCount_);
				} finally {
					_actual_seq.endUpdate();
				}
			}
			setRunning(false);
			for (int i = 0; i < _actual_seq.getViewers().size(); ++i) {
				_actualVolumeDistance3D = 0;
				final Viewer v = _actual_seq.getViewers().get(i);
				if (v.getCanvas() instanceof Canvas3D) {
					v.removeListener(MMAcquisitionEngineMT.this);
					ThreadUtil.invokeNow(new Runnable() {

						@Override
						public void run() {
							((Canvas3D) v.getCanvas()).setVolumeDistanceSample(_actualVolumeDistance3D);
						}
					});
				}
			}
			++_numFramesDone;
			if (_numFramesDone >= _numFrames)
				terminate();
		}

		public boolean cancel() {
			boolean ret = super.cancel();
			setActive(false);
			return ret;
		}

		public synchronized boolean isRunning() {
			return running_;
		}

		private synchronized void setRunning(boolean running) {
			running_ = running;
		}

		public synchronized boolean isActive() {
			return active_;
		}

		private synchronized void setActive(boolean active) {
			active_ = active;
		}
	}

	@Override
	public ImageCache getImageCache() {
		return null;
	}

	@Override
	public void setCore(CMMCore cmmcore, org.micromanager.utils.AutofocusManager autofocusmanager) {
	}

	@Override
	public void viewerChanged(ViewerEvent event) {
		if (event.getType() != ViewerEventType.CANVAS_CHANGED) {
			IcyCanvas c = event.getSource().getCanvas();
			if (c instanceof Canvas3D) {
				((Canvas3D) c).setVolumeDistanceSample(_actualVolumeDistance3D);
			}
		}
	}

	@Override
	public void viewerClosed(Viewer viewer) {

	}

	@Override
	public boolean customTimeIntervalsEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void enableCustomTimeIntervals(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getAcqOrderMode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double[] getCustomTimeIntervals() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAcqOrderMode(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCustomTimeIntervals(double[] arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean addChannel(String arg0, double arg1, Boolean arg2, double arg3, ContrastSettings arg4, int arg5, Color arg6, boolean arg7) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void attachRunnable(int arg0, int arg1, int arg2, int arg3, Runnable arg4) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearRunnables() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeImageProcessor(DataProcessor<TaggedImage> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setParentGUI(ScriptInterface parent) {
		parentGUI_ = parent;		
	}
}