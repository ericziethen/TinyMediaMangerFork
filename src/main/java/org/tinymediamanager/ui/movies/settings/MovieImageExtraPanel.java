/*
 * Copyright 2012 - 2020 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.Dimension;
import java.util.ResourceBundle;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieImageSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class MovieImageExtraPanel extends JPanel {
  private static final long           serialVersionUID = 7312645402037806284L;
  /** @wbp.nls.resourceBundle messages */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages");

  private MovieSettings               settings         = MovieModuleManager.SETTINGS;
  private JCheckBox                   cbActorImages;
  private JCheckBox                   chckbxEnableExtrathumbs;
  private JCheckBox                   chckbxEnableExtrafanart;
  private JCheckBox                   chckbxResizeExtrathumbsTo;
  private JSpinner                    spExtrathumbWidth;
  private JSpinner                    spDownloadCountExtrathumbs;
  private JSpinner                    spDownloadCountExtrafanart;

  /**
   * Instantiates a new movie image settings panel.
   */
  MovieImageExtraPanel() {
    // UI init
    initComponents();
    initDataBindings();
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][]"));
    {
      JPanel panelExtra = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", ""));

      JLabel lblExtra = new TmmLabel(BUNDLE.getString("Settings.extraartwork"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelExtra, lblExtra, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#enable-extra-artwork"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        chckbxEnableExtrathumbs = new JCheckBox(BUNDLE.getString("Settings.enable.extrathumbs"));
        panelExtra.add(chckbxEnableExtrathumbs, "cell 1 0 2 1");

        chckbxResizeExtrathumbsTo = new JCheckBox(BUNDLE.getString("Settings.resize.extrathumbs"));
        panelExtra.add(chckbxResizeExtrathumbsTo, "cell 2 1");

        spExtrathumbWidth = new JSpinner();
        spExtrathumbWidth.setMinimumSize(new Dimension(60, 20));
        panelExtra.add(spExtrathumbWidth, "cell 2 1");

        JLabel lblDownload = new JLabel(BUNDLE.getString("Settings.amount.autodownload"));
        panelExtra.add(lblDownload, "cell 2 2");

        spDownloadCountExtrathumbs = new JSpinner();
        spDownloadCountExtrathumbs.setMinimumSize(new Dimension(60, 20));
        panelExtra.add(spDownloadCountExtrathumbs, "cell 2 2");

        chckbxEnableExtrafanart = new JCheckBox(BUNDLE.getString("Settings.enable.extrafanart"));
        panelExtra.add(chckbxEnableExtrafanart, "cell 1 3 2 1");

        JLabel lblDownloadCount = new JLabel(BUNDLE.getString("Settings.amount.autodownload"));
        panelExtra.add(lblDownloadCount, "cell 2 4");

        spDownloadCountExtrafanart = new JSpinner();
        spDownloadCountExtrafanart.setMinimumSize(new Dimension(60, 20));
        panelExtra.add(spDownloadCountExtrafanart, "cell 2 4");

        cbActorImages = new JCheckBox(BUNDLE.getString("Settings.actor.download"));
        panelExtra.add(cbActorImages, "cell 1 5 2 1");
      }
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_2 = BeanProperty.create("writeActorImages");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_2, cbActorImages, jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_3 = BeanProperty.create("imageExtraFanart");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_3, chckbxEnableExtrafanart, jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_4 = BeanProperty.create("imageExtraThumbs");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_4, chckbxEnableExtrathumbs, jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    BeanProperty<MovieSettings, Integer> settingsBeanProperty_8 = BeanProperty.create("imageExtraThumbsSize");
    BeanProperty<JSpinner, Object> jSpinnerBeanProperty_1 = BeanProperty.create("value");
    AutoBinding<MovieSettings, Integer, JSpinner, Object> autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_8, spExtrathumbWidth, jSpinnerBeanProperty_1);
    autoBinding_10.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_9 = BeanProperty.create("imageExtraThumbsResize");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_9, chckbxResizeExtrathumbsTo, jCheckBoxBeanProperty);
    autoBinding_11.bind();
    //
    BeanProperty<MovieSettings, Integer> settingsBeanProperty_10 = BeanProperty.create("imageExtraThumbsCount");
    AutoBinding<MovieSettings, Integer, JSpinner, Object> autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_10, spDownloadCountExtrathumbs, jSpinnerBeanProperty_1);
    autoBinding_12.bind();
    //
    BeanProperty<MovieSettings, Integer> settingsBeanProperty_11 = BeanProperty.create("imageExtraFanartCount");
    AutoBinding<MovieSettings, Integer, JSpinner, Object> autoBinding_13 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_11, spDownloadCountExtrafanart, jSpinnerBeanProperty_1);
    autoBinding_13.bind();
    //
    BeanProperty<JSpinner, Boolean> jSpinnerBeanProperty = BeanProperty.create("enabled");
    AutoBinding<JCheckBox, Boolean, JSpinner, Boolean> autoBinding_14 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, chckbxEnableExtrafanart,
        jCheckBoxBeanProperty, spDownloadCountExtrafanart, jSpinnerBeanProperty);
    autoBinding_14.bind();
    //
    AutoBinding<JCheckBox, Boolean, JSpinner, Boolean> autoBinding_15 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, chckbxEnableExtrathumbs,
        jCheckBoxBeanProperty, spDownloadCountExtrathumbs, jSpinnerBeanProperty);
    autoBinding_15.bind();
    //
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty_1 = BeanProperty.create("enabled");
    AutoBinding<JCheckBox, Boolean, JCheckBox, Boolean> autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, chckbxEnableExtrathumbs,
        jCheckBoxBeanProperty, chckbxResizeExtrathumbsTo, jCheckBoxBeanProperty_1);
    autoBinding_8.bind();
    //
    AutoBinding<JCheckBox, Boolean, JSpinner, Boolean> autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, chckbxEnableExtrathumbs,
        jCheckBoxBeanProperty, spExtrathumbWidth, jSpinnerBeanProperty);
    autoBinding_9.bind();
  }
}
