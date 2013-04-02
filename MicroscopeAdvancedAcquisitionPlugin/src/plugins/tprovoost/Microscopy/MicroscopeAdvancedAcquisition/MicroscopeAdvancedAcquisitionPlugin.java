package plugins.tprovoost.Microscopy.MicroscopeAdvancedAcquisition;

import icy.gui.frame.IcyFrameAdapter;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.progress.AnnounceFrame;

import org.micromanager.utils.StateItem;

import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePluginAcquisition;
import plugins.tprovoost.Microscopy.MicroscopeAdvancedAcquisition.wrapper.MMAcquisitionEngineMT;

public class MicroscopeAdvancedAcquisitionPlugin extends MicroscopePluginAcquisition
{

    private MicroscopeAdvancedAcquisitionFrame _frame;
    MMAcquisitionEngineMT engine;

    @Override
    public void start()
    {
        engine = new MMAcquisitionEngineMT(this, mainGui);
        initEngine();
        _frame = MicroscopeAdvancedAcquisitionFrame.getInstance(engine, mCore, mainGui);
        if (_frame == null)
        {
            new AnnounceFrame("Only one Advanced Acquisition plugin at a time.");
            return;
        }
        _frame.addFrameListener(new IcyFrameAdapter()
        {
            @Override
            public void icyFrameClosed(IcyFrameEvent e)
            {
                super.icyFrameClosed(e);
                MicroscopeAdvancedAcquisitionFrame.delete();
                mainGui.removePlugin(MicroscopeAdvancedAcquisitionPlugin.this);
            }
        });
        _frame.setVisible(true);
        mainGui.addPlugin(this);
    }

    @Override
    public void notifyConfigAboutToChange(StateItem item)
    {
        if (engine != null && engine.isAcquisitionRunning())
            engine.setPause(true);
    }

    @Override
    public void notifyConfigChanged(StateItem item)
    {
        if (engine != null && engine.isPaused())
            engine.setPause(false);
        _frame.updateGroups();
    }

    @Override
    public void MainGUIClosed()
    {
        if (engine != null)
        {
            engine.abortRequest();
            engine.stop(true);
        }
    }

    @Override
    public String getRenderedName()
    {
        return "AdvancedAcquisition";
    }

    /**
     * Initiate the engine with the current configuration.
     */
    public void initEngine()
    {
        engine.setParentGUI(mainGui);
        engine.setCore(mCore, mainGui.getAutofocusManager());
        engine.setPositionList(mainGui.getPositionList());
        engine.setZStageDevice(mCore.getFocusDevice());
    }

}
