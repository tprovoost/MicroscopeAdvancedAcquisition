/*jadclipse*/// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.

package plugins.tprovoost.Microscopy.MicroscopeAdvancedAcquisition.wrapper;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.micromanager.AdvancedOptionsDialog;
import org.micromanager.acquisition.ComponentTitledBorder;
import org.micromanager.api.AcquisitionEngine;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.AcqOrderMode;
import org.micromanager.utils.ChannelSpec;
import org.micromanager.utils.ColorEditor;
import org.micromanager.utils.ColorRenderer;
import org.micromanager.utils.ContrastSettings;
import org.micromanager.utils.DisplayMode;
import org.micromanager.utils.FileDialogs;
import org.micromanager.utils.GUIColors;
import org.micromanager.utils.MMException;
import org.micromanager.utils.NumberUtils;
import org.micromanager.utils.ReportingUtils;
import org.micromanager.utils.TooltipTextMaker;

public class AcqControlDlg extends JPanel implements PropertyChangeListener {
	public class CheckBoxPanel extends ComponentTitledPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void setChildrenEnabled(boolean flag) {
			Component acomponent[] = getComponents();
			for (int i = 0; i < acomponent.length; i++)
				if (acomponent[i] instanceof JPanel) {
					Component acomponent1[] = ((JPanel) acomponent[i]).getComponents();
					for (int j = 0; j < acomponent1.length; j++)
						acomponent1[j].setEnabled(flag);

				} else {
					acomponent[i].setEnabled(flag);
				}

		}

		public boolean isSelected() {
			return checkBox.isSelected();
		}

		public void setSelected(boolean flag) {
			checkBox.setSelected(flag);
			setChildrenEnabled(flag);
		}

		public void addActionListener(ActionListener actionlistener) {
			checkBox.addActionListener(actionlistener);
		}

		public void removeActionListeners() {
			ActionListener aactionlistener[] = checkBox.getActionListeners();
			int i = aactionlistener.length;
			for (int j = 0; j < i; j++) {
				ActionListener actionlistener = aactionlistener[j];
				checkBox.removeActionListener(actionlistener);
			}

		}

		JCheckBox checkBox;

		CheckBoxPanel(String s) {
			titleComponent = new JCheckBox(s);
			checkBox = (JCheckBox) titleComponent;
			compTitledBorder = new ComponentTitledBorder(checkBox, this, BorderFactory.createEtchedBorder());
			setBorder(compTitledBorder);
			borderSet_ = true;
			final CheckBoxPanel thisPanel = this;
			checkBox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent actionevent) {
					boolean flag = checkBox.isSelected();
					thisPanel.setChildrenEnabled(flag);
				}

			});
		}
	}

	public class LabelPanel extends ComponentTitledPanel {

		/** */
		private static final long serialVersionUID = 6646933053538284682L;

		LabelPanel(String s) {
			titleComponent = new JLabel(s);
			JLabel jlabel = (JLabel) titleComponent;
			jlabel.setOpaque(true);
			jlabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			compTitledBorder = new ComponentTitledBorder(jlabel, this, BorderFactory.createEtchedBorder());
			setBorder(compTitledBorder);
			borderSet_ = true;
		}
	}

	public class ComponentTitledPanel extends JPanel {

		/**  */
		private static final long serialVersionUID = 1L;

		public void setBorder(Border border) {
			if (compTitledBorder != null && borderSet_)
				compTitledBorder.setBorder(border);
			else
				super.setBorder(border);
		}

		public Border getBorder() {
			return compTitledBorder;
		}

		public void setTitleFont(Font font) {
			titleComponent.setFont(font);
		}

		public ComponentTitledBorder compTitledBorder;
		public boolean borderSet_;
		public Component titleComponent;

		public ComponentTitledPanel() {
			borderSet_ = false;
		}
	}

	public class ChannelCellRenderer extends JLabel implements TableCellRenderer {

		public Component getTableCellRendererComponent(JTable jtable, Object obj, boolean flag, boolean flag1, int i, int j) {
			ChannelTableModel channeltablemodel = (ChannelTableModel) jtable.getModel();
			ArrayList<ChannelSpec> arraylist = channeltablemodel.getChannels();
			ChannelSpec channelspec = arraylist.get(i);
			setEnabled(jtable.isEnabled());
			if (!flag1)
				;
			j = jtable.convertColumnIndexToModel(j);
			setOpaque(false);
			if (j == 0) {
				JCheckBox jcheckbox = new JCheckBox("", channelspec.useChannel_);
				jcheckbox.setEnabled(jtable.isEnabled());
				jcheckbox.setOpaque(true);
				if (flag) {
					jcheckbox.setBackground(jtable.getSelectionBackground());
					jcheckbox.setOpaque(true);
				} else {
					jcheckbox.setOpaque(false);
					jcheckbox.setBackground(jtable.getBackground());
				}
				return jcheckbox;
			}
			if (j == 1)
				setText(channelspec.config_);
			else if (j == 2)
				setText(NumberUtils.doubleToDisplayString(channelspec.exposure_));
			else if (j == 3) {
				setText(NumberUtils.doubleToDisplayString(channelspec.zOffset_));
			} else {
				if (j == 4) {
					JCheckBox jcheckbox1 = new JCheckBox("", channelspec.doZStack_.booleanValue());
					jcheckbox1.setEnabled(acqEng_.isZSliceSettingEnabled() && jtable.isEnabled());
					if (flag) {
						jcheckbox1.setBackground(jtable.getSelectionBackground());
						jcheckbox1.setOpaque(true);
					} else {
						jcheckbox1.setOpaque(false);
						jcheckbox1.setBackground(jtable.getBackground());
					}
					return jcheckbox1;
				}
				if (j == 5)
					setText(Integer.toString(channelspec.skipFactorFrame_));
				else if (j == 6) {
					setText("");
					setBackground(channelspec.color_);
					setOpaque(true);
				}
			}
			if (flag) {
				setBackground(jtable.getSelectionBackground());
				setOpaque(true);
			} else {
				setOpaque(false);
				setBackground(jtable.getBackground());
			}
			return this;
		}

		public void validate() {
		}

		public void revalidate() {
		}

		protected void firePropertyChange(String s, Object obj, Object obj1) {
		}

		public void firePropertyChange(String s, boolean flag, boolean flag1) {
		}

		private static final long serialVersionUID = 1438894697L;
		private AcquisitionEngine acqEng_;

		public ChannelCellRenderer(AcquisitionEngine acquisitionengine) {
			acqEng_ = acquisitionengine;
		}
	}

	public class ChannelCellEditor extends AbstractCellEditor implements TableCellEditor {

		public Component getTableCellEditorComponent(JTable jtable, Object obj, boolean flag, int i, int j) {
			if (!flag)
				;
			ChannelTableModel channeltablemodel = (ChannelTableModel) jtable.getModel();
			ArrayList<ChannelSpec> arraylist = channeltablemodel.getChannels();
			final ChannelSpec channel = arraylist.get(i);
			channel_ = channel;
			j = jtable.convertColumnIndexToModel(j);
			editRow_ = i;
			editCol_ = j;
			if (j == 0) {
				checkBox_.setSelected(((Boolean) obj).booleanValue());
				return checkBox_;
			}
			if (j == 2 || j == 3) {
				text_.setText(((Double) obj).toString());
				return text_;
			}
			if (j == 4) {
				checkBox_.setSelected(((Boolean) obj).booleanValue());
				return checkBox_;
			}
			if (j == 5) {
				text_.setText(((Integer) obj).toString());
				return text_;
			}
			if (j == 1) {
				combo_.removeAllItems();
				ActionListener aactionlistener[] = combo_.getActionListeners();
				for (int k = 0; k < aactionlistener.length; k++)
					combo_.removeActionListener(aactionlistener[k]);

				combo_.removeAllItems();
				String as[] = channeltablemodel.getAvailableChannels();
				for (int l = 0; l < as.length; l++)
					combo_.addItem(as[l]);

				combo_.setSelectedItem(channel.config_);
				channel.color_ = new Color(colorPrefs_.getInt((new StringBuilder()).append("Color_").append(acqEng_.getChannelGroup()).append("_").append(channel.config_).toString(),
						Color.white.getRGB()));
				combo_.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent actionevent) {
						channel.color_ = new Color(colorPrefs_.getInt((new StringBuilder()).append("Color_").append(acqEng_.getChannelGroup()).append("_").append(channel.config_).toString(),
								Color.white.getRGB()));
						fireEditingStopped();
					}

				});
				return combo_;
			} else {
				return colorLabel_;
			}
		}

		public Object getCellEditorValue() {
			if (editCol_ == 0)
				return Boolean.valueOf(checkBox_.isSelected());
			if (editCol_ == 1) {
				channel_.color_ = new Color(colorPrefs_.getInt((new StringBuilder()).append("Color_").append(acqEng_.getChannelGroup()).append("_").append(combo_.getSelectedItem()).toString(),
						Color.white.getRGB()));
				return combo_.getSelectedItem();
			}
			if (editCol_ == 2 || editCol_ == 3)
				try {
					return new Double(NumberUtils.displayStringToDouble(text_.getText()));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			if (editCol_ == 4)
				return new Boolean(checkBox_.isSelected());
			if (editCol_ == 5)
				try {
					return new Integer(NumberUtils.displayStringToInt(text_.getText()));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			if (editCol_ == 6) {
				Color color = colorLabel_.getBackground();
				return color;
			}
			String s1 = new String("Internal error: unknown column");
			return s1;
		}

		private static final long serialVersionUID = 1903549075L;
		JTextField text_;
		JComboBox combo_;
		JCheckBox checkBox_;
		JLabel colorLabel_;
		int editCol_;
		int editRow_;
		ChannelSpec channel_;

		public ChannelCellEditor() {
			text_ = new JTextField();
			combo_ = new JComboBox();
			checkBox_ = new JCheckBox();
			colorLabel_ = new JLabel();
			editCol_ = -1;
			editRow_ = -1;
			channel_ = null;
		}
	}

	public class ChannelTableModel extends AbstractTableModel implements TableModelListener {

		public String getToolTipText(int i) {
			return TOOLTIPS[i];
		}

		public int getRowCount() {
			if (channels_ == null)
				return 0;
			else
				return channels_.size();
		}

		public int getColumnCount() {
			return COLUMN_NAMES.length;
		}

		public String getColumnName(int i) {
			return COLUMN_NAMES[i];
		}

		public Object getValueAt(int i, int j) {
			if (channels_ != null && i < channels_.size()) {
				if (j == 0)
					return new Boolean(channels_.get(i).useChannel_);
				if (j == 1)
					return channels_.get(i).config_;
				if (j == 2)
					return new Double(channels_.get(i).exposure_);
				if (j == 3)
					return new Double(channels_.get(i).zOffset_);
				if (j == 4)
					return new Boolean(channels_.get(i).doZStack_.booleanValue());
				if (j == 5)
					return new Integer(channels_.get(i).skipFactorFrame_);
				if (j == 6)
					return channels_.get(i).color_;
			}
			return null;
		}

		public Class<? extends Object> getColumnClass(int i) {
			return getValueAt(0, i).getClass();
		}

		public void setValueAt(Object obj, int i, int j) {
			if (i >= channels_.size() || obj == null)
				return;
			ChannelSpec channelspec = channels_.get(i);
			if (j == 0)
				channelspec.useChannel_ = ((Boolean) obj).booleanValue();
			else if (j == 1)
				channelspec.config_ = obj.toString();
			else if (j == 2)
				channelspec.exposure_ = ((Double) obj).doubleValue();
			else if (j == 3)
				channelspec.zOffset_ = ((Double) obj).doubleValue();
			else if (j == 4)
				channelspec.doZStack_ = (Boolean) obj;
			else if (j == 5)
				channelspec.skipFactorFrame_ = ((Integer) obj).intValue();
			else if (j == 6)
				channelspec.color_ = (Color) obj;
			acqEng_.setChannel(i, channelspec);
			repaint();
		}

		public boolean isCellEditable(int i, int j) {
			return j != 4 || acqEng_.isZSliceSettingEnabled();
		}

		public void tableChanged(TableModelEvent tablemodelevent) {
			int i = tablemodelevent.getFirstRow();
			if (i < 0)
				return;
			int j = tablemodelevent.getColumn();
			if (j < 0)
				return;
			ChannelSpec channelspec = channels_.get(i);
			TableModel tablemodel = (TableModel) tablemodelevent.getSource();
			if (j == 6) {
				Color color = (Color) tablemodel.getValueAt(i, j);
				colorPrefs_.putInt((new StringBuilder()).append("Color_").append(acqEng_.getChannelGroup()).append("_").append(channelspec.config_).toString(), color.getRGB());
			}
		}

		public void setChannels(ArrayList<ChannelSpec> arraylist) {
			channels_ = arraylist;
		}

		public ArrayList<ChannelSpec> getChannels() {
			return channels_;
		}

		public void addNewChannel() {
			ChannelSpec channelspec = new ChannelSpec();
			channelspec.config_ = "";
			if (acqEng_.getChannelConfigs().length > 0) {
				String as[] = acqEng_.getChannelConfigs();
				int i = as.length;
				int j = 0;
				do {
					if (j >= i)
						break;
					String s = as[j];
					boolean flag = true;
					Iterator<ChannelSpec> iterator = channels_.iterator();
					do {
						if (!iterator.hasNext())
							break;
						ChannelSpec channelspec1 = iterator.next();
						if (s.contentEquals(channelspec1.config_))
							flag = false;
					} while (true);
					if (flag) {
						channelspec.config_ = s;
						break;
					}
					j++;
				} while (true);
				if (channelspec.config_.length() == 0) {
					ReportingUtils.showMessage("No more channels are available\nin this channel group.");
				} else {
					channelspec.color_ = new Color(colorPrefs_.getInt((new StringBuilder()).append("Color_").append(acqEng_.getChannelGroup()).append("_").append(channelspec.config_).toString(),
							Color.white.getRGB()));
					channels_.add(channelspec);
				}
			}
		}

		public void removeChannel(int i) {
			if (i >= 0 && i < channels_.size())
				channels_.remove(i);
		}

		public int rowDown(int i) {
			if (i >= 0 && i < channels_.size() - 1) {
				ChannelSpec channelspec = channels_.get(i);
				channels_.remove(i);
				channels_.add(i + 1, channelspec);
				return i + 1;
			} else {
				return i;
			}
		}

		public int rowUp(int i) {
			if (i >= 1 && i < channels_.size()) {
				ChannelSpec channelspec = channels_.get(i);
				channels_.remove(i);
				channels_.add(i - 1, channelspec);
				return i - 1;
			} else {
				return i;
			}
		}

		public String[] getAvailableChannels() {
			return acqEng_.getChannelConfigs();
		}

		public void cleanUpConfigurationList() {
			Iterator<ChannelSpec> iterator = channels_.iterator();
			do {
				if (!iterator.hasNext())
					break;
				String s = iterator.next().config_;
				if (!s.contentEquals("") && !acqEng_.isConfigAvailable(s))
					iterator.remove();
			} while (true);
			fireTableStructureChanged();
		}

		public boolean duplicateChannels() {
			for (int i = 0; i < channels_.size() - 1; i++) {
				for (int j = i + 1; j < channels_.size(); j++)
					if (channels_.get(i).config_.equals(channels_.get(j).config_))
						return true;

			}

			return false;
		}

		private static final long serialVersionUID = 508170627L;
		private ArrayList<ChannelSpec> channels_;
		private AcquisitionEngine acqEng_;
		public final String COLUMN_NAMES[] = { "Use?", "Configuration", "Exposure", "Z-offset", "Z-stack", "Skip Fr.", "Color" };
		private final String TOOLTIPS[] = {
				"Toggle channel/group on/off",
				"Choose preset property values for channel or group",
				"Set exposure time in ms",
				TooltipTextMaker
						.addHTMLBreaksForTooltip("Set a Z offset specific to this channel/group (the main object in one of the channels/groups is in a different focal plane from the other channels/groups"),
				"Collect images in multiple Z planes?",
				TooltipTextMaker
						.addHTMLBreaksForTooltip("Setting 'Skip Frame' to a number other than 0 will cause the acquisition to 'skip' taking images in that channel (after taking the first image) for the indicated number of time intervals. The 5D-Image Viewer will 'fill in' these skipped frames with the previous image. In some situations it may be desirable to acquire certain channels at lower sampling rates, to reduce photo-toxicity and to save disk space. "),
				"Select channel/group color for display in viewer" };

		public ChannelTableModel(AcquisitionEngine acquisitionengine) {
			acqEng_ = acquisitionengine;
			addTableModelListener(this);
		}
	}

	public void createChannelTable() {
		model_ = new ChannelTableModel(acqEng_);
		channelTable_ = new JTable() {

			private static final long serialVersionUID = 1L;

			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {

					private static final long serialVersionUID = 1L;

					public String getToolTipText(MouseEvent mouseevent) {
						Point point = mouseevent.getPoint();
						int k = columnModel.getColumnIndexAtX(point.x);
						int l = columnModel.getColumn(k).getModelIndex();
						return model_.getToolTipText(l);
					}
				};
			}
		};
		channelTable_.setFont(new Font("Dialog", 0, 10));
		channelTable_.setAutoCreateColumnsFromModel(false);
		channelTable_.setModel(model_);
		model_.setChannels(acqEng_.getChannels());
		ChannelCellEditor channelcelleditor = new ChannelCellEditor();
		ChannelCellRenderer channelcellrenderer = new ChannelCellRenderer(acqEng_);
		channelTable_.setAutoResizeMode(0);
		for (int i = 0; i < model_.getColumnCount(); i++) {
			int j = search(columnOrder_, i);
			if (j < 0)
				j = i;
			if (j == model_.getColumnCount() - 1) {
				ColorRenderer colorrenderer = new ColorRenderer(true);
				ColorEditor coloreditor = new ColorEditor(model_, model_.getColumnCount() - 1);
				TableColumn tablecolumn1 = new TableColumn(model_.getColumnCount() - 1, 200, colorrenderer, coloreditor);
				tablecolumn1.setPreferredWidth(columnWidth_[model_.getColumnCount() - 1]);
				channelTable_.addColumn(tablecolumn1);
			} else {
				TableColumn tablecolumn = new TableColumn(j, 200, channelcellrenderer, channelcelleditor);
				tablecolumn.setPreferredWidth(columnWidth_[j]);
				channelTable_.addColumn(tablecolumn);
			}
		}

		channelTablePane_.setViewportView(channelTable_);
	}

	public JPanel createPanel(String s, int i, int j, int k, int l) {
		return createPanel(s, i, j, k, l, false);
	}

	public JPanel createPanel(String s, int i, int j, int k, int l, boolean flag) {
		ComponentTitledPanel panel;
		if (flag)
			panel = new CheckBoxPanel(s);
		else
			panel = new LabelPanel(s);
		panel.setTitleFont(new Font("Dialog", 1, 12));
		panelList_.add(panel);
		panel.setBounds(i, j, k - i, l - j);
		panel.setLayout(null);
		add(panel);
		return panel;
	}

	public void updatePanelBorder(JPanel jpanel) {
	}

	public void createEmptyPanels() {
		panelList_ = new Vector<JPanel>();
		framesPanel_ = (CheckBoxPanel) createPanel("Time points", 5, 5, 220, 91, true);
		positionsPanel_ = (CheckBoxPanel) createPanel("Multiple positions (XY)", 5, 93, 220, 154, true);
		slicesPanel_ = (CheckBoxPanel) createPanel("Z-stacks (slices)", 5, 156, 220, 306, true);
		acquisitionOrderPanel_ = createPanel("Acquisition order", 226, 5, 427, 63);
		summaryPanel_ = createPanel("Summary", 226, 152, 427, 306);
		afPanel_ = (CheckBoxPanel) createPanel("Autofocus", 226, 65, 427, 150, true);
		channelsPanel_ = (CheckBoxPanel) createPanel("Channels", 5, 308, 510, 451, true);
		savePanel_ = (CheckBoxPanel) createPanel("Save images", 5, 453, 510, 620, true);
	}

	private void createToolTips() {
		framesPanel_.setToolTipText("Acquire images over a repeating time interval");
		positionsPanel_.setToolTipText("Acquire images from a series of positions in the XY plane");
		slicesPanel_.setToolTipText("Acquire images from a series of Z positions");
		// String s =
		// getClass().getResource("icons/acq_order_figure.png").toString();
		String s1 = "<html>Lets you select the order of image acquisition when some combination of multiple " + "dimensions<br>(i.e. time points, XY positions, Z-slices, or Channels)  is selected.  "
				+ "During image acquisition, the<br>values of each dimension are iterated in the reverse order"
				+ " of their listing here.  \"Time\" and \"Position\" <br>always precede \"Slice\" and \"Channel\" "
				+ "<br><br>For example, suppose there are are two time points, two XY positions, and two Z slices, "
				+ "and Acquisition<br>order is set to \"Time, Position, Slice\".  The microscope will acquire images "
				+ "in the following order: <br> Time point 1, XY position 1, Z-slice 1 <br>Time point 1, XY position 1, "
				+ "Z-slice 2 <br>Time point 1, XY position 2, Z-slice 1 <br>Time point 1, XY position 2, Z-slice 2 <br>Time point 2, XY position 1, Z-slice 1 <br>etc. " + "<br><br></html>";
		acquisitionOrderPanel_.setToolTipText(s1);
		acqOrderBox_.setToolTipText(s1);
		afPanel_.setToolTipText("Toggle autofocus on/off");
		channelsPanel_.setToolTipText("Lets you acquire images in multiple channels (groups of properties with multiple preset values");
		savePanel_
				.setToolTipText(TooltipTextMaker
						.addHTMLBreaksForTooltip("If the Save images option is selected, images will be saved to disk continuously during the acquisition. If this option is not selected, images are accumulated only in the 5D-Image window, and once the acquisition is finished, image data can be saved to disk. However, saving files automatically during acquisition secures the acquired data against an unexpected computer failure or accidental closing of image window. Even when saving to disk, some of the acquired images are still kept in memory, facilitating fast playback. If such behavior is not desired, check the 'Conserve RAM' option (Tools | Options)"));
	}

	public AcqControlDlg(AcquisitionEngine acquisitionengine, Preferences preferences, ScriptInterface devicecontrolgui) {
		zVals_ = 0;
		disableGUItoSettings_ = false;
		prefs_ = preferences;
		gui_ = devicecontrolgui;
		guiColors_ = new GUIColors();
		Preferences preferences1 = Preferences.userNodeForPackage(getClass());
		acqPrefs_ = preferences1.node((new StringBuilder()).append(preferences1.absolutePath()).append("/").append("AcquistionSettings").toString());
		colorPrefs_ = preferences1.node((new StringBuilder()).append(preferences1.absolutePath()).append("/").append("ColorSettings").toString());
		numberFormat_ = NumberFormat.getNumberInstance();

		acqEng_ = acquisitionengine;
		setLayout(null);
		createEmptyPanels();
		JPanel jpanel = new JPanel();
		JPanel jpanel1 = new JPanel();
		jpanel.setLayout(null);
		JLabel jlabel = new JLabel("Custom time intervals enabled");
		jlabel.setFont(new Font("Arial", 1, 12));
		jlabel.setForeground(Color.red);
		JButton jbutton = new JButton("Disable custom intervals");
		jbutton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				acqEng_.enableCustomTimeIntervals(false);
				updateGUIContents();
			}

		});
		jbutton.setFont(new Font("Arial", 0, 10));
		jpanel1.add(jlabel, "First");
		jpanel1.add(jbutton, "Last");
		framesPanel_.setLayout(new BorderLayout());
		framesSubPanelLayout_ = new CardLayout();
		framesSubPanel_ = new JPanel(framesSubPanelLayout_);
		framesPanel_.add(framesSubPanel_);
		framesSubPanel_.add(jpanel, "Default frames panel");
		framesSubPanel_.add(jpanel1, "Override frames panel");
		framesSubPanelLayout_.show(framesSubPanel_, "Default frames panel");
		framesPanel_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				applySettings();
			}

		});
		JLabel jlabel1 = new JLabel();
		jlabel1.setFont(new Font("Arial", 0, 10));
		jlabel1.setText("Number");
		jpanel.add(jlabel1);
		jlabel1.setBounds(15, 0, 54, 24);
		SpinnerNumberModel spinnernumbermodel = new SpinnerNumberModel(new Integer(1), new Integer(1), null, new Integer(1));
		numFrames_ = new JSpinner(spinnernumbermodel);
		((javax.swing.JSpinner.DefaultEditor) numFrames_.getEditor()).getTextField().setFont(new Font("Arial", 0, 10));
		jpanel.add(numFrames_);
		numFrames_.setBounds(60, 0, 70, 24);
		numFrames_.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent changeevent) {
				applySettings();
			}

		});
		JLabel jlabel2 = new JLabel();
		jlabel2.setFont(new Font("Arial", 0, 10));
		jlabel2.setText("Interval");
		jlabel2.setToolTipText("Interval between successive time points.  Setting an intervalof 0 will cause micromanager to acquire 'burts' of images as fast as possible");
		jpanel.add(jlabel2);
		jlabel2.setBounds(15, 27, 43, 24);
		interval_ = new JFormattedTextField(numberFormat_);
		interval_.setFont(new Font("Arial", 0, 10));
		interval_.setValue(new Double(1.0D));
		interval_.addPropertyChangeListener("value", this);
		jpanel.add(interval_);
		interval_.setBounds(60, 27, 55, 24);
		timeUnitCombo_ = new JComboBox();
		timeUnitCombo_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
			}

		});
		timeUnitCombo_.setModel(new DefaultComboBoxModel(new String[] { "ms", "s", "min" }));
		timeUnitCombo_.setFont(new Font("Arial", 0, 10));
		timeUnitCombo_.setBounds(120, 27, 67, 24);
		jpanel.add(timeUnitCombo_);
		listButton_ = new JButton();
		listButton_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				gui_.showXYPositionList();
			}
		});
		listButton_.setToolTipText("Open XY list dialog");
		// listButton_.setIcon(SwingResourceManager.getIcon(org / micromanager /
		// AcqControlDlg, "icons/application_view_list.png"));
		listButton_.setText("Edit position list...");
		listButton_.setMargin(new Insets(2, 5, 2, 5));
		listButton_.setFont(new Font("Dialog", 0, 10));
		listButton_.setBounds(42, 25, 136, 26);
		positionsPanel_.add(listButton_);
		slicesPanel_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				applySettings();
			}

		});
		JLabel jlabel3 = new JLabel();
		jlabel3.setFont(new Font("Arial", 0, 10));
		jlabel3.setText("Z-start [um]");
		jlabel3.setBounds(30, 30, 69, 15);
		slicesPanel_.add(jlabel3);
		zBottom_ = new JFormattedTextField(numberFormat_);
		zBottom_.setFont(new Font("Arial", 0, 10));
		zBottom_.setBounds(95, 27, 54, 21);
		zBottom_.setValue(new Double(1.0D));
		zBottom_.addPropertyChangeListener("value", this);
		slicesPanel_.add(zBottom_);
		setBottomButton_ = new JButton();
		setBottomButton_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				setBottomPosition();
			}

		});
		setBottomButton_.setMargin(new Insets(-5, -5, -5, -5));
		setBottomButton_.setFont(new Font("", 0, 10));
		setBottomButton_.setText("Set");
		setBottomButton_.setToolTipText("Set value as microscope's current Z position");
		setBottomButton_.setBounds(150, 27, 50, 22);
		slicesPanel_.add(setBottomButton_);
		JLabel jlabel4 = new JLabel();
		jlabel4.setFont(new Font("Arial", 0, 10));
		jlabel4.setText("Z-end [um]");
		jlabel4.setBounds(30, 53, 69, 15);
		slicesPanel_.add(jlabel4);
		zTop_ = new JFormattedTextField(numberFormat_);
		zTop_.setFont(new Font("Arial", 0, 10));
		zTop_.setBounds(95, 50, 54, 21);
		zTop_.setValue(new Double(1.0D));
		zTop_.addPropertyChangeListener("value", this);
		slicesPanel_.add(zTop_);
		setTopButton_ = new JButton();
		setTopButton_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				setTopPosition();
			}

		});
		setTopButton_.setMargin(new Insets(-5, -5, -5, -5));
		setTopButton_.setFont(new Font("Dialog", 0, 10));
		setTopButton_.setText("Set");
		setTopButton_.setToolTipText("Set value as microscope's current Z position");
		setTopButton_.setBounds(150, 50, 50, 22);
		slicesPanel_.add(setTopButton_);
		JLabel jlabel5 = new JLabel();
		jlabel5.setFont(new Font("Arial", 0, 10));
		jlabel5.setText("Z-step [um]");
		jlabel5.setBounds(30, 76, 69, 15);
		slicesPanel_.add(jlabel5);
		zStep_ = new JFormattedTextField(numberFormat_);
		zStep_.setFont(new Font("Arial", 0, 10));
		zStep_.setBounds(95, 73, 54, 21);
		zStep_.setValue(new Double(1.0D));
		zStep_.addPropertyChangeListener("value", this);
		slicesPanel_.add(zStep_);
		zValCombo_ = new JComboBox();
		zValCombo_.setFont(new Font("Arial", 0, 10));
		zValCombo_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				zValCalcChanged();
			}

		});
		zValCombo_.setModel(new DefaultComboBoxModel(new String[] { "relative Z", "absolute Z" }));
		zValCombo_.setBounds(30, 97, 110, 22);
		slicesPanel_.add(zValCombo_);
		stackKeepShutterOpenCheckBox_ = new JCheckBox();
		stackKeepShutterOpenCheckBox_.setText("Keep shutter open");
		stackKeepShutterOpenCheckBox_.setFont(new Font("Arial", 0, 10));
		stackKeepShutterOpenCheckBox_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				applySettings();
			}

		});
		stackKeepShutterOpenCheckBox_.setSelected(false);
		stackKeepShutterOpenCheckBox_.setBounds(60, 121, 150, 22);
		slicesPanel_.add(stackKeepShutterOpenCheckBox_);
		acqOrderBox_ = new JComboBox();
		acqOrderBox_.setFont(new Font("", 0, 10));
		acqOrderBox_.setBounds(2, 26, 195, 22);
		acquisitionOrderPanel_.add(acqOrderBox_);
		acqOrderModes_ = new AcqOrderMode[4];
		acqOrderModes_[0] = new AcqOrderMode(0);
		acqOrderModes_[1] = new AcqOrderMode(1);
		acqOrderModes_[2] = new AcqOrderMode(2);
		acqOrderModes_[3] = new AcqOrderMode(3);
		acqOrderBox_.addItem(acqOrderModes_[0]);
		acqOrderBox_.addItem(acqOrderModes_[1]);
		acqOrderBox_.addItem(acqOrderModes_[2]);
		acqOrderBox_.addItem(acqOrderModes_[3]);
		summaryTextArea_ = new JTextArea();
		summaryTextArea_.setFont(new Font("Arial", 0, 11));
		summaryTextArea_.setEditable(false);
		summaryTextArea_.setBounds(4, 19, 350, 120);
		summaryTextArea_.setMargin(new Insets(2, 2, 2, 2));
		summaryTextArea_.setOpaque(false);
		summaryPanel_.add(summaryTextArea_);
		afPanel_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				applySettings();
			}

		});
		afButton_ = new JButton();
		afButton_.setToolTipText("Set autofocus options");
		afButton_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				afOptions();
			}

		});
		afButton_.setText("Options...");
		// afButton_.setIcon(SwingResourceManager.getIcon(org / micromanager /
		// AcqControlDlg, "icons/wrench_orange.png"));
		afButton_.setMargin(new Insets(2, 5, 2, 5));
		afButton_.setFont(new Font("Dialog", 0, 10));
		afButton_.setBounds(50, 21, 100, 28);
		afPanel_.add(afButton_);
		JLabel jlabel6 = new JLabel();
		jlabel6.setFont(new Font("Dialog", 0, 10));
		jlabel6.setText("Skip frame(s): ");
		jlabel6.setToolTipText(TooltipTextMaker
				.addHTMLBreaksForTooltip("The number of 'frames skipped' correspondsto the number of time intervals of image acquisition that pass before micromanager autofocuses again.  Micromanager will always autofocus when moving to a new position regardless of this value"));
		jlabel6.setBounds(35, 54, 70, 21);
		afPanel_.add(jlabel6);
		afSkipInterval_ = new JSpinner(new SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
		((javax.swing.JSpinner.DefaultEditor) afSkipInterval_.getEditor()).getTextField().setFont(new Font("Arial", 0, 10));
		afSkipInterval_.setBounds(105, 54, 55, 22);
		afSkipInterval_.setValue(new Integer(acqEng_.getAfSkipInterval()));
		afSkipInterval_.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent changeevent) {
				applySettings();
				afSkipInterval_.setValue(new Integer(acqEng_.getAfSkipInterval()));
			}

		});
		afPanel_.add(afSkipInterval_);
		channelsPanel_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				applySettings();
			}

		});
		JLabel jlabel7 = new JLabel();
		jlabel7.setFont(new Font("Arial", 0, 10));
		jlabel7.setBounds(90, 19, 80, 24);
		jlabel7.setText("Channel group:");
		channelsPanel_.add(jlabel7);
		channelGroupCombo_ = new JComboBox();
		channelGroupCombo_.setFont(new Font("", 0, 10));
		updateGroupsCombo();
		channelGroupCombo_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				String s = (String) channelGroupCombo_.getSelectedItem();
				if (acqEng_.setChannelGroup(s)) {
					model_.cleanUpConfigurationList();
					if (gui_.getAutofocusManager() != null)
						try {
							gui_.getAutofocusManager().refresh();
						} catch (MMException mmexception) {
							ReportingUtils.showError(mmexception);
						}
				} else {
					updateGroupsCombo();
				}
			}

		});
		channelGroupCombo_.setBounds(165, 20, 150, 22);
		channelsPanel_.add(channelGroupCombo_);
		channelTablePane_ = new JScrollPane();
		channelTablePane_.setFont(new Font("Arial", 0, 10));
		channelTablePane_.setBounds(10, 45, 414, 90);
		channelsPanel_.add(channelTablePane_);
		JButton jbutton1 = new JButton();
		jbutton1.setFont(new Font("Arial", 0, 10));
		jbutton1.setMargin(new Insets(0, 0, 0, 0));
		jbutton1.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				applySettings();
				model_.addNewChannel();
				model_.fireTableStructureChanged();
			}

		});
		jbutton1.setText("New");
		jbutton1.setToolTipText("Create new channel for currently selected channel group");
		jbutton1.setBounds(430, 45, 68, 22);
		channelsPanel_.add(jbutton1);
		JButton jbutton2 = new JButton();
		jbutton2.setFont(new Font("Arial", 0, 10));
		jbutton2.setMargin(new Insets(-5, -5, -5, -5));
		jbutton2.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				int k = channelTable_.getSelectedRow();
				if (k > -1) {
					applySettings();
					model_.removeChannel(k);
					model_.fireTableStructureChanged();
					if (channelTable_.getRowCount() > k)
						channelTable_.setRowSelectionInterval(k, k);
				}
			}

		});
		jbutton2.setText("Remove");
		jbutton2.setToolTipText("Remove currently selected channel");
		jbutton2.setBounds(430, 69, 68, 22);
		channelsPanel_.add(jbutton2);
		JButton jbutton3 = new JButton();
		jbutton3.setFont(new Font("Arial", 0, 10));
		jbutton3.setMargin(new Insets(0, 0, 0, 0));
		jbutton3.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				int k = channelTable_.getSelectedRow();
				if (k > -1) {
					applySettings();
					int l = model_.rowUp(k);
					model_.fireTableStructureChanged();
					channelTable_.setRowSelectionInterval(l, l);
				}
			}

		});
		jbutton3.setText("Up");
		jbutton3.setToolTipText(TooltipTextMaker.addHTMLBreaksForTooltip("Move currently selected channel up (Channels higher on list are acquired first)"));
		jbutton3.setBounds(430, 93, 68, 22);
		channelsPanel_.add(jbutton3);
		JButton jbutton4 = new JButton();
		jbutton4.setFont(new Font("Arial", 0, 10));
		jbutton4.setMargin(new Insets(0, 0, 0, 0));
		jbutton4.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				int k = channelTable_.getSelectedRow();
				if (k > -1) {
					applySettings();
					int l = model_.rowDown(k);
					model_.fireTableStructureChanged();
					channelTable_.setRowSelectionInterval(l, l);
				}
			}

		});
		jbutton4.setText("Down");
		jbutton4.setToolTipText(TooltipTextMaker.addHTMLBreaksForTooltip("Move currently selected channel down (Channels lower on list are acquired later)"));
		jbutton4.setBounds(430, 117, 68, 22);
		channelsPanel_.add(jbutton4);
		chanKeepShutterOpenCheckBox_ = new JCheckBox();
		chanKeepShutterOpenCheckBox_.setText("Keep shutter open");
		chanKeepShutterOpenCheckBox_.setFont(new Font("Arial", 0, 10));
		chanKeepShutterOpenCheckBox_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				applySettings();
			}

		});
		chanKeepShutterOpenCheckBox_.setSelected(false);
		chanKeepShutterOpenCheckBox_.setBounds(330, 20, 150, 22);
		channelsPanel_.add(chanKeepShutterOpenCheckBox_);
		savePanel_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				if (!savePanel_.isSelected())
					displayModeCombo_.setSelectedIndex(0);
				commentTextArea_.setEnabled(savePanel_.isSelected());
				applySettings();
			}

		});
		displayMode_ = new JLabel();
		displayMode_.setFont(new Font("Arial", 0, 10));
		displayMode_.setText("Display");
		displayMode_.setBounds(150, 15, 49, 21);
		displayModeCombo_ = new JComboBox();
		displayModeCombo_.setFont(new Font("", 0, 10));
		displayModeCombo_.setBounds(188, 14, 150, 24);
		displayModeCombo_.addItem(new DisplayMode(0));
		displayModeCombo_.addItem(new DisplayMode(1));
		displayModeCombo_.addItem(new DisplayMode(2));
		displayModeCombo_.setEnabled(false);
		rootLabel_ = new JLabel();
		rootLabel_.setFont(new Font("Arial", 0, 10));
		rootLabel_.setText("Directory root");
		rootLabel_.setBounds(10, 30, 72, 22);
		savePanel_.add(rootLabel_);
		rootField_ = new JTextField();
		rootField_.setFont(new Font("Arial", 0, 10));
		rootField_.setBounds(90, 30, 354, 22);
		savePanel_.add(rootField_);
		browseRootButton_ = new JButton();
		browseRootButton_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				setRootDirectory();
			}

		});
		browseRootButton_.setMargin(new Insets(2, 5, 2, 5));
		browseRootButton_.setFont(new Font("Dialog", 0, 10));
		browseRootButton_.setText("...");
		browseRootButton_.setBounds(445, 30, 47, 24);
		savePanel_.add(browseRootButton_);
		browseRootButton_.setToolTipText("Browse");
		namePrefixLabel_ = new JLabel();
		namePrefixLabel_.setFont(new Font("Arial", 0, 10));
		namePrefixLabel_.setText("Name prefix");
		namePrefixLabel_.setBounds(10, 55, 76, 22);
		savePanel_.add(namePrefixLabel_);
		nameField_ = new JTextField();
		nameField_.setFont(new Font("Arial", 0, 10));
		nameField_.setBounds(90, 55, 354, 22);
		savePanel_.add(nameField_);
		commentLabel_ = new JLabel();
		commentLabel_.setFont(new Font("Arial", 0, 10));
		commentLabel_.setText("Comments");
		commentLabel_.setBounds(10, 80, 76, 22);
		savePanel_.add(commentLabel_);
		JScrollPane jscrollpane = new JScrollPane();
		jscrollpane.setBounds(90, 80, 354, 72);
		savePanel_.add(jscrollpane);
		commentTextArea_ = new JTextArea();
		jscrollpane.setViewportView(commentTextArea_);
		commentTextArea_.setFont(new Font("", 0, 10));
		commentTextArea_.setToolTipText("Comment for the current acquistion");
		commentTextArea_.setWrapStyleWord(true);
		commentTextArea_.setLineWrap(true);
		commentTextArea_.setBorder(new EtchedBorder(1));
		JButton jbutton5 = new JButton();
		jbutton5.setFont(new Font("Arial", 0, 10));
		jbutton5.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				saveSettings();
				saveAcqSettings();
				gui_.makeActive();
			}

		});
		jbutton5.setText("Close");
		jbutton5.setBounds(432, 10, 80, 22);
		add(jbutton5);
		JButton btnAcquire = new JButton();
		btnAcquire.setMargin(new Insets(-9, -9, -9, -9));
		btnAcquire.setFont(new Font("Arial", 1, 12));
		btnAcquire.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				AbstractCellEditor abstractcelleditor = (AbstractCellEditor) channelTable_.getCellEditor();
				if (abstractcelleditor != null)
					abstractcelleditor.stopCellEditing();
				runAcquisition();
			}

		});
		btnAcquire.setText("Acquire!");
		btnAcquire.setBounds(432, 44, 80, 22);
		add(btnAcquire);
		
		JButton btnStop = new JButton();
		btnStop.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				acqEng_.abortRequest();
				acqEng_.stop(true);
			}

		});
		btnStop.setText("Stop");
		btnStop.setFont(new Font("Arial", 1, 12));
		btnStop.setBounds(432, 68, 80, 22);
		add(btnStop);
		JButton jbutton8 = new JButton();
		jbutton8.setFont(new Font("Arial", 0, 10));
		jbutton8.setMargin(new Insets(-5, -5, -5, -5));
		jbutton8.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				loadAcqSettingsFromFile();
			}

		});
		jbutton8.setText("Load...");
		jbutton8.setBounds(432, 102, 80, 22);
		add(jbutton8);
		jbutton8.setToolTipText("Load acquisition settings");
		JButton jbutton9 = new JButton();
		jbutton9.setFont(new Font("Arial", 0, 10));
		jbutton9.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				saveAsAcqSettingsToFile();
			}

		});
		jbutton9.setToolTipText("Save current acquisition settings as");
		jbutton9.setText("Save as...");
		jbutton9.setBounds(432, 126, 80, 22);
		jbutton9.setMargin(new Insets(-5, -5, -5, -5));
		add(jbutton9);
		JButton jbutton10 = new JButton();
		jbutton10.setFont(new Font("Arial", 0, 10));
		jbutton10.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				showAdvancedDialog();
				updateGUIContents();
			}

		});
		jbutton10.setText("Advanced");
		jbutton10.setBounds(432, 170, 80, 22);
		add(jbutton10);
		int i = 100;
		int j = 100;
		setBounds(i, j, 521, 645);
		if (prefs_ != null) {
			i = prefs_.getInt("acq_x", i);
			j = prefs_.getInt("acq_y", j);
			setLocation(i, j);
		}
		positionsPanel_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				applySettings();
			}

		});
		displayModeCombo_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				applySettings();
			}

		});
		acqOrderBox_.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent actionevent) {
				applySettings();
			}

		});
		loadAcqSettings();
		createChannelTable();
		updateGUIContents();
		applySettings();
		createToolTips();
	}

	public void propertyChange(PropertyChangeEvent propertychangeevent) {
		applySettings();
		summaryTextArea_.setText(acqEng_.getVerboseSummary());
	}

	protected void afOptions() {
		if (gui_.getAutofocusManager() != null && gui_.getAutofocusManager().getDevice() != null)
			gui_.getAutofocusManager().showOptionsDialog();
	}

	public boolean inArray(String s, String as[]) {
		for (int i = 0; i < as.length; i++)
			if (s.equals(as[i]))
				return true;

		return false;
	}

	public void close() {
		try {
			saveSettings();
		} catch (Throwable throwable) {
			ReportingUtils.logError(throwable, "in saveSettings");
		}
		try {
			saveAcqSettings();
		} catch (Throwable throwable1) {
			ReportingUtils.logError(throwable1, "in saveAcqSettings");
		}
		try {
			
		} catch (Throwable throwable2) {
			ReportingUtils.logError(throwable2, "in dispose");
		}
		if (null != gui_)
			try {
				gui_.makeActive();
			} catch (Throwable throwable3) {
				ReportingUtils.logError(throwable3, "in makeActive");
			}
	}

	public void updateGroupsCombo() {
		String as[] = acqEng_.getAvailableGroups();
		if (as.length != 0) {
			channelGroupCombo_.setModel(new DefaultComboBoxModel(as));
			if (!inArray(acqEng_.getChannelGroup(), as))
				acqEng_.setChannelGroup(acqEng_.getFirstConfigGroup());
			channelGroupCombo_.setSelectedItem(acqEng_.getChannelGroup());
		}
	}

	public void updateChannelAndGroupCombo() {
		updateGroupsCombo();
		model_.cleanUpConfigurationList();
	}

	public synchronized void loadAcqSettings() {
		disableGUItoSettings_ = true;
		acqEng_.clear();
		int i = acqPrefs_.getInt("acqNumframes", 1);
		double d = acqPrefs_.getDouble("acqInterval", 0.0D);
		acqEng_.setFrames(i, d);
		acqEng_.enableFramesSetting(acqPrefs_.getBoolean("enableMultiFrame", false));
		framesPanel_.setSelected(acqEng_.isFramesSettingEnabled());
		numFrames_.setValue(Integer.valueOf(acqEng_.getNumFrames()));
		int j = acqPrefs_.getInt("acqTimeInit", 0);
		timeUnitCombo_.setSelectedIndex(j);
		double d1 = acqPrefs_.getDouble("acqZbottom", 0.0D);
		double d2 = acqPrefs_.getDouble("acqZtop", 0.0D);
		double d3 = acqPrefs_.getDouble("acqZstep", 1.0D);
		if (Math.abs(d3) < Math.abs(acqEng_.getMinZStepUm()))
			d3 = acqEng_.getMinZStepUm();
		zVals_ = acqPrefs_.getInt("acqZValues", 0);
		acqEng_.setSlices(d1, d2, d3, zVals_ != 0);
		acqEng_.enableZSliceSetting(acqPrefs_.getBoolean("enableSliceSettings", acqEng_.isZSliceSettingEnabled()));
		acqEng_.enableMultiPosition(acqPrefs_.getBoolean("enableMultiPosition", acqEng_.isMultiPositionEnabled()));
		positionsPanel_.setSelected(acqEng_.isMultiPositionEnabled());
		slicesPanel_.setSelected(acqEng_.isZSliceSettingEnabled());
		acqEng_.enableChannelsSetting(acqPrefs_.getBoolean("enableMultiChannels", false));
		channelsPanel_.setSelected(acqEng_.isChannelsSettingEnabled());
		savePanel_.setSelected(acqPrefs_.getBoolean("acqSaveFiles", false));
		nameField_.setText(acqPrefs_.get("acqDirName", "Untitled"));
		rootField_.setText(acqPrefs_.get("acqRootName", (new StringBuilder()).append(System.getProperty("user.home")).append("/AcquisitionData").toString()));
		acqEng_.setAcqOrderMode(acqPrefs_.getInt("acqOrderMode", acqEng_.getAcqOrderMode()));
		acqEng_.setDisplayMode(acqPrefs_.getInt("acqDisplayMode", acqEng_.getDisplayMode()));
		acqEng_.enableAutoFocus(acqPrefs_.getBoolean("autofocus_enabled", acqEng_.isAutoFocusEnabled()));
		acqEng_.setAfSkipInterval(acqPrefs_.getInt("autofocusSkipInterval", acqEng_.getAfSkipInterval()));
		acqEng_.setChannelGroup(acqPrefs_.get("acqChannelGroup", acqEng_.getFirstConfigGroup()));
		afPanel_.setSelected(acqEng_.isAutoFocusEnabled());
		acqEng_.keepShutterOpenForChannels(acqPrefs_.getBoolean("acqChannelsKeepShutterOpen", false));
		acqEng_.keepShutterOpenForStack(acqPrefs_.getBoolean("acqStackKeepShutterOpen", false));
		ArrayList<Double> arraylist = new ArrayList<Double>();
		for (int k = 0; acqPrefs_.getDouble((new StringBuilder()).append("customInterval").append(k).toString(), -1D) >= 0.0D; k++)
			arraylist.add(Double.valueOf(acqPrefs_.getDouble((new StringBuilder()).append("customInterval").append(k).toString(), -1D)));

		double ad[] = new double[arraylist.size()];
		for (int l = 0; l < ad.length; l++)
			ad[l] = arraylist.get(l).doubleValue();

		acqEng_.setCustomTimeIntervals(ad);
		acqEng_.enableCustomTimeIntervals(acqPrefs_.getBoolean("enableCustomIntervals", false));
		int i1 = acqPrefs_.getInt("acqNumchannels", 0);
		ChannelSpec channelspec = new ChannelSpec();
		acqEng_.getChannels().clear();
		for (int j1 = 0; j1 < i1; j1++) {
			String s1 = acqPrefs_.get((new StringBuilder()).append("acqChannelName").append(j1).toString(), "Undefined");
			boolean flag = acqPrefs_.getBoolean((new StringBuilder()).append("acqChannelUse").append(j1).toString(), true);
			double d4 = acqPrefs_.getDouble((new StringBuilder()).append("acqChannelExp").append(j1).toString(), 0.0D);
			Boolean boolean1 = Boolean.valueOf(acqPrefs_.getBoolean((new StringBuilder()).append("acqChannelDoZStack").append(j1).toString(), true));
			double d5 = acqPrefs_.getDouble((new StringBuilder()).append("acqChannelZOffset").append(j1).toString(), 0.0D);
			ContrastSettings contrastsettings = new ContrastSettings();
			contrastsettings.min = acqPrefs_.getInt((new StringBuilder()).append("acqChannel8ContrastMin").append(j1).toString(), channelspec.contrast_.min);
			contrastsettings.max = acqPrefs_.getInt((new StringBuilder()).append("acqChannel8ContrastMax").append(j1).toString(), channelspec.contrast_.max);
			ContrastSettings contrastsettings1 = new ContrastSettings();
			contrastsettings1.min = acqPrefs_.getInt((new StringBuilder()).append("acqChannel16ContrastMin").append(j1).toString(), channelspec.contrast_.min);
			contrastsettings1.max = acqPrefs_.getInt((new StringBuilder()).append("acqChannel16ContrastMax").append(j1).toString(), channelspec.contrast_.max);
			int l1 = acqPrefs_.getInt((new StringBuilder()).append("acqChannelColorR").append(j1).toString(), channelspec.color_.getRed());
			int i2 = acqPrefs_.getInt((new StringBuilder()).append("acqChannelColorG").append(j1).toString(), channelspec.color_.getGreen());
			int j2 = acqPrefs_.getInt((new StringBuilder()).append("acqChannelColorB").append(j1).toString(), channelspec.color_.getBlue());
			int k2 = acqPrefs_.getInt((new StringBuilder()).append("acqSkip").append(j1).toString(), channelspec.skipFactorFrame_);
			Color color = new Color(l1, i2, j2);
			acqEng_.addChannel(s1, d4, boolean1, d5, contrastsettings, contrastsettings1, k2, color, flag);
		}

		byte byte0 = 7;
		columnWidth_ = new int[byte0];
		columnOrder_ = new int[byte0];
		for (int k1 = 0; k1 < byte0; k1++) {
			columnWidth_[k1] = acqPrefs_.getInt((new StringBuilder()).append("column_width").append(k1).toString(), 77);
			columnOrder_[k1] = acqPrefs_.getInt((new StringBuilder()).append("column_order").append(k1).toString(), k1);
		}

		disableGUItoSettings_ = false;
	}

	public synchronized void saveAcqSettings() {
		try {
			acqPrefs_.clear();
		} catch (BackingStoreException backingstoreexception) {
			ReportingUtils.showError(backingstoreexception);
		}
		applySettings();
		acqPrefs_.putBoolean("enableMultiFrame", acqEng_.isFramesSettingEnabled());
		acqPrefs_.putBoolean("enableMultiChannels", acqEng_.isChannelsSettingEnabled());
		acqPrefs_.putInt("acqNumframes", acqEng_.getNumFrames());
		acqPrefs_.putDouble("acqInterval", acqEng_.getFrameIntervalMs());
		acqPrefs_.putInt("acqTimeInit", timeUnitCombo_.getSelectedIndex());
		acqPrefs_.putDouble("acqZbottom", acqEng_.getSliceZBottomUm());
		acqPrefs_.putDouble("acqZtop", acqEng_.getZTopUm());
		acqPrefs_.putDouble("acqZstep", acqEng_.getSliceZStepUm());
		acqPrefs_.putBoolean("enableSliceSettings", acqEng_.isZSliceSettingEnabled());
		acqPrefs_.putBoolean("enableMultiPosition", acqEng_.isMultiPositionEnabled());
		acqPrefs_.putInt("acqZValues", zVals_);
		acqPrefs_.putBoolean("acqSaveFiles", savePanel_.isSelected());
		acqPrefs_.put("acqDirName", nameField_.getText());
		acqPrefs_.put("acqRootName", rootField_.getText());
		acqPrefs_.putInt("acqOrderMode", acqEng_.getAcqOrderMode());
		acqPrefs_.putInt("acqDisplayMode", acqEng_.getDisplayMode());
		acqPrefs_.putBoolean("autofocus_enabled", acqEng_.isAutoFocusEnabled());
		acqPrefs_.putInt("autofocusSkipInterval", acqEng_.getAfSkipInterval());
		acqPrefs_.putBoolean("acqChannelsKeepShutterOpen", acqEng_.isShutterOpenForChannels());
		acqPrefs_.putBoolean("acqStackKeepShutterOpen", acqEng_.isShutterOpenForStack());
		acqPrefs_.put("acqChannelGroup", acqEng_.getChannelGroup());
		ArrayList<ChannelSpec> arraylist = acqEng_.getChannels();
		acqPrefs_.putInt("acqNumchannels", arraylist.size());
		for (int i = 0; i < arraylist.size(); i++) {
			ChannelSpec channelspec = arraylist.get(i);
			acqPrefs_.put((new StringBuilder()).append("acqChannelName").append(i).toString(), channelspec.config_);
			acqPrefs_.putBoolean((new StringBuilder()).append("acqChannelUse").append(i).toString(), channelspec.useChannel_);
			acqPrefs_.putDouble((new StringBuilder()).append("acqChannelExp").append(i).toString(), channelspec.exposure_);
			acqPrefs_.putBoolean((new StringBuilder()).append("acqChannelDoZStack").append(i).toString(), channelspec.doZStack_.booleanValue());
			acqPrefs_.putDouble((new StringBuilder()).append("acqChannelZOffset").append(i).toString(), channelspec.zOffset_);
			acqPrefs_.putDouble((new StringBuilder()).append("acqChannel8ContrastMin").append(i).toString(), channelspec.contrast_.min);
			acqPrefs_.putDouble((new StringBuilder()).append("acqChannel8ContrastMax").append(i).toString(), channelspec.contrast_.max);
			acqPrefs_.putDouble((new StringBuilder()).append("acqChannel16ContrastMin").append(i).toString(), channelspec.contrast_.min);
			acqPrefs_.putDouble((new StringBuilder()).append("acqChannel16ContrastMax").append(i).toString(), channelspec.contrast_.max);
			acqPrefs_.putInt((new StringBuilder()).append("acqChannelColorR").append(i).toString(), channelspec.color_.getRed());
			acqPrefs_.putInt((new StringBuilder()).append("acqChannelColorG").append(i).toString(), channelspec.color_.getGreen());
			acqPrefs_.putInt((new StringBuilder()).append("acqChannelColorB").append(i).toString(), channelspec.color_.getBlue());
			acqPrefs_.putInt((new StringBuilder()).append("acqSkip").append(i).toString(), channelspec.skipFactorFrame_);
		}

		double ad[] = acqEng_.getCustomTimeIntervals();
		if (ad != null && ad.length > 0) {
			for (int j = 0; j < ad.length; j++)
				acqPrefs_.putDouble((new StringBuilder()).append("customInterval").append(j).toString(), ad[j]);

		}
		acqPrefs_.putBoolean("enableCustomIntervals", acqEng_.customTimeIntervalsEnabled());
		for (int k = 0; k < model_.getColumnCount(); k++) {
			acqPrefs_.putInt((new StringBuilder()).append("column_width").append(k).toString(), findTableColumn(channelTable_, k).getWidth());
			acqPrefs_.putInt((new StringBuilder()).append("column_order").append(k).toString(), channelTable_.convertColumnIndexToView(k));
		}

		try {
			acqPrefs_.flush();
		} catch (BackingStoreException backingstoreexception1) {
			ReportingUtils.logError(backingstoreexception1);
		}
	}

	public TableColumn findTableColumn(JTable jtable, int i) {
		for (Enumeration<TableColumn> enumeration = jtable.getColumnModel().getColumns(); enumeration.hasMoreElements();) {
			TableColumn tablecolumn = (TableColumn) enumeration.nextElement();
			if (tablecolumn.getModelIndex() == i)
				return tablecolumn;
		}

		return null;
	}

	protected void enableZSliceControls(boolean flag) {
		zBottom_.setEnabled(flag);
		zTop_.setEnabled(flag);
		zStep_.setEnabled(flag);
		zValCombo_.setEnabled(flag);
	}

	protected void setRootDirectory() {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(1);
		fc.setCurrentDirectory(new File(acqEng_.getRootName()));
		int retVal = fc.showOpenDialog(this);
		if (retVal == 0) {
			rootField_.setText(fc.getSelectedFile().getAbsolutePath());
			acqEng_.setRootName(fc.getSelectedFile().getAbsolutePath());
		}
	}

	protected void setTopPosition() {
		double d = acqEng_.getCurrentZPos();
		zTop_.setText(NumberUtils.doubleToDisplayString(d));
		applySettings();
		summaryTextArea_.setText(acqEng_.getVerboseSummary());
	}

	protected void setBottomPosition() {
		double d = acqEng_.getCurrentZPos();
		zBottom_.setText(NumberUtils.doubleToDisplayString(d));
		applySettings();
		summaryTextArea_.setText(acqEng_.getVerboseSummary());
	}

	protected void loadAcqSettingsFromFile() {
		File file = FileDialogs.openFile(null, "Load acquisition settings", ACQ_SETTINGS_FILE);
		if (file != null)
			loadAcqSettingsFromFile(file.getAbsolutePath());
	}

	public void loadAcqSettingsFromFile(String s) {
		acqFile_ = new File(s);
		try {
			FileInputStream fileinputstream = new FileInputStream(acqFile_);
			acqPrefs_.clear();
			Preferences.importPreferences(fileinputstream);
			loadAcqSettings();
			updateGUIContents();
			fileinputstream.close();
			acqDir_ = acqFile_.getParent();
			if (acqDir_ != null)
				prefs_.put("dir", acqDir_);
		} catch (Exception exception) {
			ReportingUtils.showError(exception);
			return;
		}
	}

	protected boolean saveAsAcqSettingsToFile() {
		saveAcqSettings();
		File file = FileDialogs.save(null, "Save the acquisition settings file", ACQ_SETTINGS_FILE);
		if (file != null) {
			try {
				FileOutputStream fileoutputstream = new FileOutputStream(file);
				acqPrefs_.exportNode(fileoutputstream);
			} catch (FileNotFoundException filenotfoundexception) {
				ReportingUtils.showError(filenotfoundexception);
				return false;
			} catch (IOException ioexception) {
				ReportingUtils.showError(ioexception);
				return false;
			} catch (BackingStoreException backingstoreexception) {
				ReportingUtils.showError(backingstoreexception);
				return false;
			}
			return true;
		} else {
			return false;
		}
	}

	public void runAcquisition() {
		if (acqEng_.isAcquisitionRunning()) {
			JOptionPane.showMessageDialog(this, "Cannot start acquisition: previous acquisition still in progress.");
			return;
		}
		applySettings();
		ChannelTableModel channeltablemodel = (ChannelTableModel) channelTable_.getModel();
		if (acqEng_.isChannelsSettingEnabled() && channeltablemodel.duplicateChannels()) {
			JOptionPane.showMessageDialog(this, "Cannot start acquisition using the same channel twice");
			return;
		}
		try {
			acqEng_.acquire();
		} catch (MMException mmexception) {
			ReportingUtils.showError(mmexception);
			return;
		}
	}

	public void runAcquisition(String s, String s1) {
		if (acqEng_.isAcquisitionRunning()) {
			JOptionPane.showMessageDialog(this, "Unable to start the new acquisition task: previous acquisition still in progress.");
			return;
		}
		try {
			applySettings();
			acqEng_.setDirName(s);
			acqEng_.setRootName(s1);
			acqEng_.setSaveFiles(true);
			acqEng_.acquire();
		} catch (MMException mmexception) {
			ReportingUtils.showError(mmexception);
			return;
		}
	}

	public boolean isAcquisitionRunning() {
		return acqEng_.isAcquisitionRunning();
	}

	public static int search(int ai[], int i) {
		for (int j = 0; j < ai.length; j++)
			if (ai[j] == i)
				return j;

		return -1;
	}

	private void checkForCustomTimeIntervals() {
		if (acqEng_.customTimeIntervalsEnabled())
			framesSubPanelLayout_.show(framesSubPanel_, "Override frames panel");
		else
			framesSubPanelLayout_.show(framesSubPanel_, "Default frames panel");
	}

	public void updateGUIContents() {
		if (disableGUItoSettings_)
			return;
		disableGUItoSettings_ = true;
		model_.setChannels(acqEng_.getChannels());
		double d = acqEng_.getFrameIntervalMs();
		interval_.setText(numberFormat_.format(convertMsToTime(d, timeUnitCombo_.getSelectedIndex())));
		zBottom_.setText(NumberUtils.doubleToDisplayString(acqEng_.getSliceZBottomUm()));
		zTop_.setText(NumberUtils.doubleToDisplayString(acqEng_.getZTopUm()));
		zStep_.setText(NumberUtils.doubleToDisplayString(acqEng_.getSliceZStepUm()));
		framesPanel_.setSelected(acqEng_.isFramesSettingEnabled());
		checkForCustomTimeIntervals();
		slicesPanel_.setSelected(acqEng_.isZSliceSettingEnabled());
		positionsPanel_.setSelected(acqEng_.isMultiPositionEnabled());
		afPanel_.setSelected(acqEng_.isAutoFocusEnabled());
		acqOrderBox_.setEnabled(positionsPanel_.isSelected() || framesPanel_.isSelected() || slicesPanel_.isSelected() || channelsPanel_.isSelected());
		afSkipInterval_.setEnabled(acqEng_.isAutoFocusEnabled());
		Integer integer = new Integer(acqEng_.getNumFrames());
		Integer integer1 = new Integer(acqEng_.getAfSkipInterval());
		if (acqEng_.isFramesSettingEnabled())
			numFrames_.setValue(integer);
		afSkipInterval_.setValue(integer1);
		enableZSliceControls(acqEng_.isZSliceSettingEnabled());
		model_.fireTableStructureChanged();
		channelGroupCombo_.setSelectedItem(acqEng_.getChannelGroup());
		try {
			displayModeCombo_.setSelectedIndex(acqEng_.getDisplayMode());
		} catch (IllegalArgumentException illegalargumentexception) {
			displayModeCombo_.setSelectedIndex(0);
		}
		AcqOrderMode aacqordermode[] = acqOrderModes_;
		int j = aacqordermode.length;
		for (int k = 0; k < j; k++) {
			AcqOrderMode acqordermode = aacqordermode[k];
			acqordermode.setEnabled(framesPanel_.isSelected(), positionsPanel_.isSelected(), slicesPanel_.isSelected(), channelsPanel_.isSelected());
		}

		int i = acqEng_.getAcqOrderMode();
		acqOrderBox_.removeAllItems();
		if (framesPanel_.isSelected() && positionsPanel_.isSelected() && slicesPanel_.isSelected() && channelsPanel_.isSelected()) {
			acqOrderBox_.addItem(acqOrderModes_[0]);
			acqOrderBox_.addItem(acqOrderModes_[1]);
			acqOrderBox_.addItem(acqOrderModes_[2]);
			acqOrderBox_.addItem(acqOrderModes_[3]);
		} else if (framesPanel_.isSelected() && positionsPanel_.isSelected()) {
			if (i == 0 || i == 2) {
				acqOrderBox_.addItem(acqOrderModes_[0]);
				acqOrderBox_.addItem(acqOrderModes_[2]);
			} else {
				acqOrderBox_.addItem(acqOrderModes_[1]);
				acqOrderBox_.addItem(acqOrderModes_[3]);
			}
		} else if (channelsPanel_.isSelected() && slicesPanel_.isSelected()) {
			if (i == 0 || i == 1) {
				acqOrderBox_.addItem(acqOrderModes_[0]);
				acqOrderBox_.addItem(acqOrderModes_[1]);
			} else {
				acqOrderBox_.addItem(acqOrderModes_[2]);
				acqOrderBox_.addItem(acqOrderModes_[3]);
			}
		} else {
			acqOrderBox_.addItem(acqOrderModes_[i]);
		}
		acqOrderBox_.setSelectedItem(acqOrderModes_[acqEng_.getAcqOrderMode()]);
		zValCombo_.setSelectedIndex(zVals_);
		stackKeepShutterOpenCheckBox_.setSelected(acqEng_.isShutterOpenForStack());
		chanKeepShutterOpenCheckBox_.setSelected(acqEng_.isShutterOpenForChannels());
		channelTable_.setAutoResizeMode(4);
		boolean flag = channelsPanel_.isSelected();
		channelTable_.setEnabled(flag);
		channelTable_.getTableHeader().setForeground(flag ? Color.black : Color.gray);
		summaryTextArea_.setText(acqEng_.getVerboseSummary());
		disableGUItoSettings_ = false;
	}

	private void applySettings() {
		if (disableGUItoSettings_)
			return;
		disableGUItoSettings_ = true;
		AbstractCellEditor abstractcelleditor = (AbstractCellEditor) channelTable_.getCellEditor();
		if (abstractcelleditor != null)
			abstractcelleditor.stopCellEditing();
		try {
			double d = NumberUtils.displayStringToDouble(zStep_.getText());
			if (Math.abs(d) < acqEng_.getMinZStepUm())
				d = acqEng_.getMinZStepUm();
			acqEng_.setSlices(NumberUtils.displayStringToDouble(zBottom_.getText()), NumberUtils.displayStringToDouble(zTop_.getText()), d, zVals_ != 0);
			acqEng_.enableZSliceSetting(slicesPanel_.isSelected());
			acqEng_.enableMultiPosition(positionsPanel_.isSelected());
			acqEng_.setDisplayMode(((DisplayMode) displayModeCombo_.getSelectedItem()).getID());
			acqEng_.setAcqOrderMode(((AcqOrderMode) acqOrderBox_.getSelectedItem()).getID());
			acqEng_.enableChannelsSetting(channelsPanel_.isSelected());
			acqEng_.setChannels(((ChannelTableModel) channelTable_.getModel()).getChannels());
			acqEng_.enableFramesSetting(framesPanel_.isSelected());
			acqEng_.setFrames(((Integer) numFrames_.getValue()).intValue(), convertTimeToMs(NumberUtils.displayStringToDouble(interval_.getText()), timeUnitCombo_.getSelectedIndex()));
			acqEng_.setAfSkipInterval(NumberUtils.displayStringToInt(afSkipInterval_.getValue().toString()));
			acqEng_.keepShutterOpenForChannels(chanKeepShutterOpenCheckBox_.isSelected());
			acqEng_.keepShutterOpenForStack(stackKeepShutterOpenCheckBox_.isSelected());
		} catch (ParseException parseexception) {
			ReportingUtils.showError(parseexception);
		}
		acqEng_.setSaveFiles(savePanel_.isSelected());
		acqEng_.setDirName(nameField_.getText());
		acqEng_.setRootName(rootField_.getText());
		acqEng_.setComment(commentTextArea_.getText());
		acqEng_.enableAutoFocus(afPanel_.isSelected());
		acqEng_.setParameterPreferences(acqPrefs_);
		disableGUItoSettings_ = false;
		updateGUIContents();
	}

	private void saveSettings() {
		Rectangle rectangle = getBounds();
		if (prefs_ != null) {
			prefs_.putInt("acq_x", rectangle.x);
			prefs_.putInt("acq_y", rectangle.y);
		}
	}

	private double convertTimeToMs(double d, int i) {
		if (i == 1)
			return d * 1000D;
		if (i == 2)
			return d * 60D * 1000D;
		if (i == 0) {
			return d;
		} else {
			ReportingUtils.showError("Unknown units supplied for acquisition interval!");
			return d;
		}
	}

	private double convertMsToTime(double d, int i) {
		if (i == 1)
			return d / 1000D;
		if (i == 2)
			return d / 60000D;
		if (i == 0) {
			return d;
		} else {
			ReportingUtils.showError("Unknown units supplied for acquisition interval!");
			return d;
		}
	}

	private void zValCalcChanged() {
		if (zValCombo_.getSelectedIndex() == 0) {
			setTopButton_.setEnabled(false);
			setBottomButton_.setEnabled(false);
		} else {
			setTopButton_.setEnabled(true);
			setBottomButton_.setEnabled(true);
		}
		if (zVals_ == zValCombo_.getSelectedIndex())
			return;
		zVals_ = zValCombo_.getSelectedIndex();
		double d;
		double d1;
		try {
			d = NumberUtils.displayStringToDouble(zBottom_.getText());
			d1 = NumberUtils.displayStringToDouble(zTop_.getText());
		} catch (ParseException parseexception) {
			ReportingUtils.logError(parseexception);
			return;
		}
		double d2 = acqEng_.getCurrentZPos();
		double d3;
		double d4;
		if (zVals_ == 0) {
			setTopButton_.setEnabled(false);
			setBottomButton_.setEnabled(false);
			d3 = d1 - d2;
			d4 = d - d2;
		} else {
			setTopButton_.setEnabled(true);
			setBottomButton_.setEnabled(true);
			d3 = d1 + d2;
			d4 = d + d2;
		}
		zBottom_.setText(NumberUtils.doubleToDisplayString(d4));
		zTop_.setText(NumberUtils.doubleToDisplayString(d3));
	}

	public void setBackgroundStyle(String s) {
		setBackground((Color) guiColors_.background.get(s));
		repaint();
	}

	private void showAdvancedDialog() {
		if (advancedOptionsWindow_ == null)
			advancedOptionsWindow_ = new AdvancedOptionsDialog(acqEng_);
		advancedOptionsWindow_.setVisible(true);
	}

	private static final long serialVersionUID = 1L;
	protected JButton listButton_;
	private JButton afButton_;
	private JSpinner afSkipInterval_;
	private JComboBox acqOrderBox_;
	public static final String NEW_ACQFILE_NAME = "MMAcquistion.xml";
	public static final String ACQ_SETTINGS_NODE = "AcquistionSettings";
	public static final String COLOR_SETTINGS_NODE = "ColorSettings";
	private JComboBox channelGroupCombo_;
	private JTextArea commentTextArea_;
	private JComboBox zValCombo_;
	private JTextField nameField_;
	private JTextField rootField_;
	private JTextArea summaryTextArea_;
	private JComboBox timeUnitCombo_;
	private JFormattedTextField interval_;
	private JFormattedTextField zStep_;
	private JFormattedTextField zTop_;
	private JFormattedTextField zBottom_;
	private AcquisitionEngine acqEng_;
	private JScrollPane channelTablePane_;
	private JTable channelTable_;
	private JSpinner numFrames_;
	private ChannelTableModel model_;
	private Preferences prefs_;
	private Preferences acqPrefs_;
	private Preferences colorPrefs_;
	private File acqFile_;
	private String acqDir_;
	private int zVals_;
	private JButton setBottomButton_;
	private JButton setTopButton_;
	protected JComboBox displayModeCombo_;
	private ScriptInterface gui_;
	private GUIColors guiColors_;
	private NumberFormat numberFormat_;
	private JLabel namePrefixLabel_;
	private JLabel rootLabel_;
	private JLabel commentLabel_;
	private JButton browseRootButton_;
	private JLabel displayMode_;
	private JCheckBox stackKeepShutterOpenCheckBox_;
	private JCheckBox chanKeepShutterOpenCheckBox_;
	private AcqOrderMode acqOrderModes_[];
	private AdvancedOptionsDialog advancedOptionsWindow_;
	private static final org.micromanager.utils.FileDialogs.FileType ACQ_SETTINGS_FILE = new org.micromanager.utils.FileDialogs.FileType("ACQ_SETTINGS_FILE", "Acquisition settings",
			(new StringBuilder()).append(System.getProperty("user.home")).append("/AcqSettings.xml").toString(), true, new String[] { "xml" });
	private int columnWidth_[];
	private int columnOrder_[];
	private CheckBoxPanel framesPanel_;
	private JPanel framesSubPanel_;
	private CardLayout framesSubPanelLayout_;
	private CheckBoxPanel channelsPanel_;
	private CheckBoxPanel slicesPanel_;
	protected CheckBoxPanel positionsPanel_;
	private JPanel acquisitionOrderPanel_;
	private CheckBoxPanel afPanel_;
	private JPanel summaryPanel_;
	private CheckBoxPanel savePanel_;
	private Vector<JPanel> panelList_;
	private boolean disableGUItoSettings_;

}