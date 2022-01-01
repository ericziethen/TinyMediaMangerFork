/*
 * Copyright 2012 - 2022 Manuel Laggner
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The Class TvShowEditAction. To edit TV shows/episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowKodiRefreshNfoAction extends TmmAction {
  private static final long serialVersionUID = -3911290901017607679L;

  public TvShowKodiRefreshNfoAction() {
    putValue(LARGE_ICON_KEY, IconManager.MEDIAINFO);
    putValue(SMALL_ICON, IconManager.MEDIAINFO);
    putValue(NAME, TmmResourceBundle.getString("kodi.rpc.refreshnfo"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Object> selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects();

    if (selectedObjects.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    List<TvShowEpisode> eps = new ArrayList<>();
    Set<TvShow> shows = new HashSet<>();

    // if we specify show, we want it recursive for all episodes
    // so remove all single episode calls to not sent them twice...
    for (Object obj : selectedObjects) {
      if (obj instanceof TvShow) {
        TvShow show = (TvShow) obj;
        shows.add(show);
      }
      if (obj instanceof TvShowEpisode) {
        TvShowEpisode episode = (TvShowEpisode) obj;
        eps.add(episode);
      }
      if (obj instanceof TvShowSeason) {
        TvShowSeason season = (TvShowSeason) obj;
        eps.addAll(season.getEpisodes());
      }
    }

    // remove all EPs, where we already have the show
    for (int i = eps.size() - 1; i >= 0; i--) {
      TvShowEpisode ep = eps.get(i);
      if (shows.contains(ep.getTvShow())) {
        eps.remove(i);
      }
    }

    TmmTaskManager.getInstance()
        .addUnnamedTask(
            new TmmTask(TmmResourceBundle.getString("kodi.rpc.refreshnfo"), shows.size() + eps.size(), TmmTaskHandle.TaskType.BACKGROUND_TASK) {

              @Override
              protected void doInBackground() {
                KodiRPC kodiRPC = KodiRPC.getInstance();
                int i = 0;

                // update show + all EPs
                for (TvShow tvShow : shows) {
                  kodiRPC.refreshFromNfo(tvShow);

                  publishState(++i);
                  if (cancel) {
                    return;
                  }
                }

                // update single EP only
                for (TvShowEpisode episode : eps) {
                  kodiRPC.refreshFromNfo(episode);

                  publishState(++i);
                  if (cancel) {
                    return;
                  }
                }

                // if we have updated at least one movie, we need to re-match the movies
                if (progressDone > 0) {
                  kodiRPC.updateTvShowMappings();
                }
              }
            });
  }
}
