package plugins.tprovoost.Microscopy.MicroscopeAdvancedAcquisition;

import icy.gui.component.IcyLogo;
import icy.gui.frame.IcyFrame;

import java.awt.BorderLayout;
import java.awt.Dimension;

import mmcorej.CMMCore;

import org.micromanager.api.AcquisitionEngine;

import plugins.tprovoost.Microscopy.MicroManagerForIcy.MMMainFrame;
import plugins.tprovoost.Microscopy.MicroscopeAdvancedAcquisition.wrapper.AcqControlDlg;

public class MicroscopeAdvancedAcquisitionFrame extends IcyFrame {

	private static MicroscopeAdvancedAcquisitionFrame _singleton = null;
	
	/** Acquisition Panel for multi-D acquisition */
	private AcqControlDlg _acqdialog;
	private MicroscopeAdvancedAcquisitionFrame(AcquisitionEngine engine,CMMCore core, MMMainFrame gui) {
		super("Control Dialog",false, true,false, true);
		
		IcyLogo _logo_remote = new IcyLogo("Microscope Advanced Acquisition");
		_logo_remote.setPreferredSize(new Dimension(0, 80));
		add(_logo_remote, BorderLayout.NORTH);
		
		_acqdialog = new AcqControlDlg(engine,null ,gui);
		_acqdialog.setPreferredSize(new Dimension(515, 620));
		add(_acqdialog);
		setVisible(true);
		addToMainDesktopPane();
		requestFocus();
		center();
		refresh();
	}
	
	public void refresh() {
		pack();
	}
	
	/**
	 * Updates the list of groups
	 */
	public void updateGroups() {
		_acqdialog.updateGroupsCombo();
	}
	
	/**
	 * Singleton Pattern.<br/>This will not allocate a new Object. 
	 * In order to do that, please use newInstance().
	 * @return Returns the actual instance of the MMMainFrame or null if not allocated.
	 * @see dispose(), newInstance()
	 */
	public static MicroscopeAdvancedAcquisitionFrame getInstance() {
		return _singleton;
	}
	
	/**
	 * Singleton Pattern. Allocates and returns the new object. if already initialized, returns null. 
	 * @return instance or new instance of the MMMainFrame
	 * @see dispose(), getInstance()
	 */
	public static MicroscopeAdvancedAcquisitionFrame getInstance(AcquisitionEngine engine, CMMCore core, MMMainFrame gui) {
		if (_singleton == null) {
			_singleton = new MicroscopeAdvancedAcquisitionFrame(engine,core, gui);
			return _singleton;
		}
		return _singleton;
	}

	/**
	 * Destroys the actual singleton.
	 * @see  getInstance(), newInstance()
	 */
	public static void dispose() {
		_singleton = null;
	}
	
}
