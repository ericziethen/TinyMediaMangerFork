/*
 * Copyright 2012 - 2019 Manuel Laggner
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
import static org.tinymediamanager.ui.TmmFontHelper.L1;
import static org.tinymediamanager.ui.TmmFontHelper.L2;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TableColumnResizer;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.EnhancedTextField;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.SettingsPanelFactory;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The class MovieRenamerSettingsPanel.
 */
public class MovieRenamerSettingsPanel extends JPanel implements HierarchyListener {
  private static final long              serialVersionUID = 5039498266207230875L;
  /** @wbp.nls.resourceBundle messages */
  private static final ResourceBundle    BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$
  private static final Logger            LOGGER           = LoggerFactory.getLogger(MovieRenamerSettingsPanel.class);

  private MovieSettings                  settings         = MovieModuleManager.SETTINGS;
  private List<String>                   spaceReplacement = new ArrayList<>(Arrays.asList("_", ".", "-"));
  private List<String>                   colonReplacement = new ArrayList<>(Arrays.asList(" ", "-"));
  private EventList<MovieRenamerExample> exampleEventList;

  /**
   * UI components
   */
  private EnhancedTextField              tfMoviePath;
  private EnhancedTextField              tfMovieFilename;
  private JLabel                         lblExample;
  private JCheckBox                      chckbxAsciiReplacement;

  private JCheckBox                      chckbxSpaceReplacement;
  private JComboBox                      cbSpaceReplacement;
  private JComboBox                      cbMovieForPreview;
  private JCheckBox                      chckbxRemoveOtherNfos;
  private JCheckBox                      chckbxMoviesetSingleMovie;

  private TmmTable                       tableExamples;
  private ReadOnlyTextArea               taMMDWarning;
  private JComboBox                      cbColonReplacement;

  public MovieRenamerSettingsPanel() {
    exampleEventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(MovieRenamerExample.class)));

    // UI initializations
    initComponents();
    initDataBindings();

    // data init
    tfMoviePath.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }

      @Override
      public void changedUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }
    });

    tfMovieFilename.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }

      @Override
      public void changedUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }
    });

    // space replacement
    String replacement = settings.getRenamerSpaceReplacement();
    int index = spaceReplacement.indexOf(replacement);
    if (index >= 0) {
      cbSpaceReplacement.setSelectedIndex(index);
    }

    // colon replacement
    replacement = settings.getRenamerColonReplacement();
    index = colonReplacement.indexOf(replacement);
    if (index >= 0) {
      cbColonReplacement.setSelectedIndex(index);
    }

    ActionListener actionCreateRenamerExample = e -> createRenamerExample();
    cbMovieForPreview.addActionListener(actionCreateRenamerExample);
    cbSpaceReplacement.addActionListener(arg0 -> {
      checkChanges();
      createRenamerExample();
    });
    cbColonReplacement.addActionListener(arg0 -> {
      checkChanges();
      createRenamerExample();
    });

    chckbxMoviesetSingleMovie.addActionListener(actionCreateRenamerExample);
    chckbxAsciiReplacement.addActionListener(actionCreateRenamerExample);
    chckbxSpaceReplacement.addActionListener(actionCreateRenamerExample);

    lblExample.putClientProperty("clipPosition", SwingConstants.LEFT);

    // examples
    exampleEventList.add(new MovieRenamerExample("${title}"));
    exampleEventList.add(new MovieRenamerExample("${originalTitle}"));
    exampleEventList.add(new MovieRenamerExample("${title[0]}"));
    exampleEventList.add(new MovieRenamerExample("${title;first}"));
    exampleEventList.add(new MovieRenamerExample("${title[0,2]}"));
    exampleEventList.add(new MovieRenamerExample("${titleSortable}"));
    exampleEventList.add(new MovieRenamerExample("${year}"));
    exampleEventList.add(new MovieRenamerExample("${movieSet.title}"));
    exampleEventList.add(new MovieRenamerExample("${movieSet.titleSortable}"));
    exampleEventList.add(new MovieRenamerExample("${rating}"));
    exampleEventList.add(new MovieRenamerExample("${imdb}"));
    exampleEventList.add(new MovieRenamerExample("${certification}"));
    exampleEventList.add(new MovieRenamerExample("${directors[0].name}"));
    exampleEventList.add(new MovieRenamerExample("${genres[0]}"));
    exampleEventList.add(new MovieRenamerExample("${genres[0].name}"));
    exampleEventList.add(new MovieRenamerExample("${genresAsString}"));
    exampleEventList.add(new MovieRenamerExample("${tags[0]}"));
    exampleEventList.add(new MovieRenamerExample("${language}"));
    exampleEventList.add(new MovieRenamerExample("${videoResolution}"));
    exampleEventList.add(new MovieRenamerExample("${videoCodec}"));
    exampleEventList.add(new MovieRenamerExample("${videoFormat}"));
    exampleEventList.add(new MovieRenamerExample("${videoBitDepth}"));
    exampleEventList.add(new MovieRenamerExample("${audioCodec}"));
    exampleEventList.add(new MovieRenamerExample("${audioCodecList}"));
    exampleEventList.add(new MovieRenamerExample("${audioCodecsAsString}"));
    exampleEventList.add(new MovieRenamerExample("${audioChannels}"));
    exampleEventList.add(new MovieRenamerExample("${audioChannelList}"));
    exampleEventList.add(new MovieRenamerExample("${audioChannelsAsString}"));
    exampleEventList.add(new MovieRenamerExample("${audioLanguage}"));
    exampleEventList.add(new MovieRenamerExample("${audioLanguageList}"));
    exampleEventList.add(new MovieRenamerExample("${audioLanguagesAsString}"));
    exampleEventList.add(new MovieRenamerExample("${mediaSource}"));
    exampleEventList.add(new MovieRenamerExample("${3Dformat}"));
    exampleEventList.add(new MovieRenamerExample("${hdr}"));
    exampleEventList.add(new MovieRenamerExample("${edition}"));
    exampleEventList.add(new MovieRenamerExample("${parent}"));
  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 1", "[grow]", "[][15lp!][][15lp!][]"));
    {
      JPanel panelPatterns = new JPanel(new MigLayout("insets 0, hidemode 1", "[20lp!][15lp][][300lp,grow]", "[][][][][][]"));

      JLabel lblPatternsT = new TmmLabel(BUNDLE.getString("Settings.movie.renamer.title"), H3); //$NON-NLS-1$
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelPatterns, lblPatternsT, true);
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        JLabel lblMoviePath = new JLabel(BUNDLE.getString("Settings.renamer.folder")); //$NON-NLS-1$
        panelPatterns.add(lblMoviePath, "cell 1 0 2 1,alignx right");

        tfMoviePath = new EnhancedTextField(IconManager.UNDO_GREY);
        tfMoviePath.setIconToolTipText(BUNDLE.getString("Settings.renamer.reverttodefault")); //$NON-NLS-1$
        tfMoviePath.addIconMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            tfMoviePath.setText(MovieSettings.DEFAULT_RENAMER_FOLDER_PATTERN);
          }
        });
        panelPatterns.add(tfMoviePath, "cell 3 0,growx");

        JLabel lblDefault = new JLabel(BUNDLE.getString("Settings.default")); //$NON-NLS-1$
        panelPatterns.add(lblDefault, "cell 1 1 2 1,alignx right");
        TmmFontHelper.changeFont(lblDefault, L2);

        JTextArea tpDefaultFolderPattern = new ReadOnlyTextArea(MovieSettings.DEFAULT_RENAMER_FOLDER_PATTERN);
        panelPatterns.add(tpDefaultFolderPattern, "cell 3 1,growx,wmin 0");
        TmmFontHelper.changeFont(tpDefaultFolderPattern, L2);
      }
      {
        JLabel lblMovieFilename = new JLabel(BUNDLE.getString("Settings.renamer.file")); //$NON-NLS-1$
        panelPatterns.add(lblMovieFilename, "cell 1 2 2 1,alignx right");

        tfMovieFilename = new EnhancedTextField(IconManager.UNDO_GREY);
        tfMovieFilename.setIconToolTipText(BUNDLE.getString("Settings.renamer.reverttodefault")); //$NON-NLS-1$
        tfMovieFilename.addIconMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            tfMovieFilename.setText(MovieSettings.DEFAULT_RENAMER_FILE_PATTERN);
          }
        });
        panelPatterns.add(tfMovieFilename, "cell 3 2,growx");

        JLabel lblDefault = new JLabel(BUNDLE.getString("Settings.default")); //$NON-NLS-1$
        panelPatterns.add(lblDefault, "cell 1 3 2 1,alignx right");
        TmmFontHelper.changeFont(lblDefault, L2);

        JTextArea tpDefaultFilePattern = new ReadOnlyTextArea(MovieSettings.DEFAULT_RENAMER_FILE_PATTERN);
        panelPatterns.add(tpDefaultFilePattern, "cell 3 3,growx,wmin 0");
        TmmFontHelper.changeFont(tpDefaultFilePattern, L2);
      }
      {

        JLabel lblRenamerHintT = new JLabel(BUNDLE.getString("Settings.movie.renamer.example")); //$NON-NLS-1$
        panelPatterns.add(lblRenamerHintT, "cell 1 4 3 1");

        JButton btnHelp = new JButton(BUNDLE.getString("tmm.help")); //$NON-NLS-1$
        btnHelp.addActionListener(e -> {
          String url = StringEscapeUtils.unescapeHtml4("https://gitlab.com/tinyMediaManager/tinyMediaManager/wikis/Movie-Settings#renamer");
          try {
            TmmUIHelper.browseUrl(url);
          }
          catch (Exception e1) {
            LOGGER.error("Wiki", e1);
            MessageManager.instance
                .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e1.getLocalizedMessage() }));
          }
        });
        panelPatterns.add(btnHelp, "cell 1 4 3 1");
      }
      {
        taMMDWarning = new ReadOnlyTextArea(BUNDLE.getString("Settings.renamer.folder.warning"));
        taMMDWarning.setForeground(Color.red);
        panelPatterns.add(taMMDWarning, "cell 3 5,growx,wmin 0");
      }
    }
    {
      JPanel panelAdvancedOptions = SettingsPanelFactory.createSettingsPanel();

      JLabel lblAdvancedOptions = new TmmLabel(BUNDLE.getString("Settings.advancedoptions"), H3); //$NON-NLS-1$
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelAdvancedOptions, lblAdvancedOptions, true);
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");
      {
        chckbxSpaceReplacement = new JCheckBox(BUNDLE.getString("Settings.renamer.spacereplacement")); //$NON-NLS-1$
        chckbxSpaceReplacement.setToolTipText(BUNDLE.getString("Settings.renamer.spacereplacement.hint")); //$NON-NLS-1$
        panelAdvancedOptions.add(chckbxSpaceReplacement, "cell 1 0 2 1");

        cbSpaceReplacement = new JComboBox<>(spaceReplacement.toArray());
        panelAdvancedOptions.add(cbSpaceReplacement, "cell 1 0");
      }
      {
        JLabel lblColonReplacement = new JLabel(BUNDLE.getString("Settings.renamer.colonreplacement")); //$NON-NLS-1$
        panelAdvancedOptions.add(lblColonReplacement, "cell 2 1");
        lblColonReplacement.setToolTipText(BUNDLE.getString("Settings.renamer.colonreplacement.hint"));

        cbColonReplacement = new JComboBox<>(colonReplacement.toArray());
        panelAdvancedOptions.add(cbColonReplacement, "cell 2 1");
      }
      {
        chckbxAsciiReplacement = new JCheckBox(BUNDLE.getString("Settings.renamer.asciireplacement"));
        panelAdvancedOptions.add(chckbxAsciiReplacement, "cell 1 2 2 1");

        JLabel lblAsciiHint = new JLabel(BUNDLE.getString("Settings.renamer.asciireplacement.hint"));
        panelAdvancedOptions.add(lblAsciiHint, "cell 2 3");
        TmmFontHelper.changeFont(lblAsciiHint, L2);
      }
      {
        chckbxMoviesetSingleMovie = new JCheckBox(BUNDLE.getString("Settings.renamer.moviesetsinglemovie"));
        panelAdvancedOptions.add(chckbxMoviesetSingleMovie, "cell 1 4 2 1");
      }
      {
        chckbxRemoveOtherNfos = new JCheckBox(BUNDLE.getString("Settings.renamer.removenfo"));
        panelAdvancedOptions.add(chckbxRemoveOtherNfos, "cell 1 5 2 1");
      }
    }
    {
      JPanel panelExample = new JPanel();
      panelExample.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][300lp,grow]", ""));

      JLabel lblExampleHeader = new TmmLabel(BUNDLE.getString("Settings.example"), H3); //$NON-NLS-1$
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelExample, lblExampleHeader, true);
      add(collapsiblePanel, "cell 0 4, growx, wmin 0");
      {
        JLabel lblExampleT = new JLabel(BUNDLE.getString("tmm.movie")); //$NON-NLS-1$
        panelExample.add(lblExampleT, "cell 1 0");

        cbMovieForPreview = new JComboBox();
        panelExample.add(cbMovieForPreview, "cell 1 0");

        lblExample = new TmmLabel("", L1);
        panelExample.add(lblExample, "cell 1 1, wmin 0");

        DefaultEventTableModel<MovieRenamerExample> exampleTableModel = new DefaultEventTableModel<>(
            GlazedListsSwing.swingThreadProxyList(exampleEventList), new MovieRenamerExampleTableFormat());
        tableExamples = new TmmTable(exampleTableModel);
        JScrollPane scrollPaneExamples = new JScrollPane(tableExamples);
        tableExamples.configureScrollPane(scrollPaneExamples);
        panelExample.add(scrollPaneExamples, "cell 1 2,grow");
      }
    }
  }

  private void buildAndInstallMovieArray() {
    cbMovieForPreview.removeAllItems();
    List<Movie> allMovies = new ArrayList<>(MovieList.getInstance().getMovies());
    allMovies.sort(new MovieComparator());
    for (Movie movie : allMovies) {
      MoviePreviewContainer container = new MoviePreviewContainer();
      container.movie = movie;
      cbMovieForPreview.addItem(container);
    }
  }

  private void createRenamerExample() {
    Movie movie = null;

    // empty is valid (although not unique)
    if (!tfMoviePath.getText().isEmpty() && !MovieRenamer.isFolderPatternUnique(tfMoviePath.getText())) {
      taMMDWarning.setVisible(true);
    }
    else {
      taMMDWarning.setVisible(false);
    }

    if (cbMovieForPreview.getSelectedItem() instanceof MoviePreviewContainer) {
      MoviePreviewContainer container = (MoviePreviewContainer) cbMovieForPreview.getSelectedItem();
      movie = container.movie;
    }

    if (movie != null) {
      String path = "";
      String filename = "";
      if (StringUtils.isNotBlank(tfMoviePath.getText())) {
        path = MovieRenamer.createDestinationForFoldername(tfMoviePath.getText(), movie);
      }
      else {
        // the old folder name
        path = movie.getPathNIO().getFileName().toString();
      }

      if (StringUtils.isNotBlank(tfMovieFilename.getText())) {
        List<MediaFile> mediaFiles = movie.getMediaFiles(MediaFileType.VIDEO);
        if (mediaFiles.size() > 0) {
          String extension = FilenameUtils.getExtension(mediaFiles.get(0).getFilename());
          filename = MovieRenamer.createDestinationForFilename(tfMovieFilename.getText(), movie) + "." + extension;
        }
      }
      else {
        filename = movie.getMediaFiles(MediaFileType.VIDEO).get(0).getFilename();
      }

      lblExample.setText(movie.getDataSource() + File.separator + path + File.separator + filename);

      // create examples
      for (MovieRenamerExample example : exampleEventList) {
        example.createExample(movie);
      }
      try {
        TableColumnResizer.adjustColumnPreferredWidths(tableExamples, 7);
      }
      catch (Exception ignored) {
      }
    }
    else {
      lblExample.setText(BUNDLE.getString("Settings.movie.renamer.nomovie")); //$NON-NLS-1$
    }
  }

  private void checkChanges() {
    // space replacement
    String replacement = (String) cbSpaceReplacement.getSelectedItem();
    settings.setRenamerSpaceReplacement(replacement);

    // colon replacement
    replacement = (String) cbColonReplacement.getSelectedItem();
    settings.setRenamerColonReplacement(replacement);
  }

  @Override
  public void hierarchyChanged(HierarchyEvent arg0) {
    if (isShowing()) {
      buildAndInstallMovieArray();
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    addHierarchyListener(this);
  }

  @Override
  public void removeNotify() {
    removeHierarchyListener(this);
    super.removeNotify();
  }

  /*****************************************************************************
   * helper classes
   *****************************************************************************/
  private class MoviePreviewContainer {
    Movie movie;

    @Override
    public String toString() {
      return movie.getTitle();
    }
  }

  private class MovieComparator implements Comparator<Movie> {
    @Override
    public int compare(Movie arg0, Movie arg1) {
      return arg0.getTitle().compareTo(arg1.getTitle());
    }
  }

  @SuppressWarnings("unused")
  private class MovieRenamerExample extends AbstractModelObject {
    private String token;
    private String description;
    private String example = "";

    private MovieRenamerExample(String token) {
      this.token = token;
      try {
        this.description = BUNDLE.getString("Settings.movie.renamer." + token); //$NON-NLS-1$
      }
      catch (Exception e) {
        this.description = "";
      }
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getExample() {
      return example;
    }

    public void setExample(String example) {
      this.example = example;
    }

    private void createExample(Movie movie) {
      String oldValue = example;
      if (movie == null) {
        example = "";
      }
      else {
        example = MovieRenamer.createDestination(token, movie, true);
      }
      firePropertyChange("example", oldValue, example);
    }
  }

  private class MovieRenamerExampleTableFormat implements TableFormat<MovieRenamerExample> {
    @Override
    public int getColumnCount() {
      return 3;
    }

    @Override
    public String getColumnName(int column) {
      switch (column) {
        case 0:
          return BUNDLE.getString("Settings.renamer.token.name"); //$NON-NLS-1$

        case 1:
          return BUNDLE.getString("Settings.renamer.token"); //$NON-NLS-1$

        case 2:
          return BUNDLE.getString("Settings.renamer.value"); //$NON-NLS-1$

      }
      return null;
    }

    @Override
    public Object getColumnValue(MovieRenamerExample baseObject, int column) {
      switch (column) {
        case 0:
          return baseObject.token;

        case 1:
          return baseObject.description;

        case 2:
          return baseObject.example;

        default:
          break;
      }
      return null;
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSettings, String> settingsBeanProperty_11 = BeanProperty.create("renamerPathname");
    BeanProperty<JTextField, String> jTextFieldBeanProperty_3 = BeanProperty.create("text");
    AutoBinding<MovieSettings, String, JTextField, String> autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_11, tfMoviePath, jTextFieldBeanProperty_3);
    autoBinding_10.bind();
    //
    BeanProperty<MovieSettings, String> settingsBeanProperty_12 = BeanProperty.create("renamerFilename");
    BeanProperty<JTextField, String> jTextFieldBeanProperty_4 = BeanProperty.create("text");
    AutoBinding<MovieSettings, String, JTextField, String> autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_12, tfMovieFilename, jTextFieldBeanProperty_4);
    autoBinding_11.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty = BeanProperty.create("renamerSpaceSubstitution");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty, chckbxSpaceReplacement, jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_1 = BeanProperty.create("renamerNfoCleanup");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_1, chckbxRemoveOtherNfos, jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_5 = BeanProperty.create("renamerCreateMoviesetForSingleMovie");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_5, chckbxMoviesetSingleMovie, jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_7 = BeanProperty.create("asciiReplacement");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_7, chckbxAsciiReplacement, jCheckBoxBeanProperty);
    autoBinding_5.bind();
  }
}
