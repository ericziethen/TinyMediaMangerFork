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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The Class TvShowDownloadActorImagesAction To download images from actors / producers for selected TvShows
 *
 * @author wjanes
 */
public class TvShowDownloadActorImagesAction extends TmmAction {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

  public TvShowDownloadActorImagesAction() {
    putValue(NAME, BUNDLE.getString("tvshow.downloadactorimages"));
    putValue(SMALL_ICON, IconManager.IMAGE);
    putValue(LARGE_ICON_KEY, IconManager.IMAGE);
  }

  @Override
  protected void processAction(ActionEvent e) {

    List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShows();

    if (selectedTvShows.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), BUNDLE.getString("tmm.nothingselected"));
      return;
    }

    for (TvShow tvShow : selectedTvShows) {
      tvShow.writeActorImages();
    }
  }
}