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
package org.tinymediamanager.thirdparty.trakttv;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;

/**
 * Clear your movie data from trakt.tv
 * 
 * @author Manuel Laggner
 */
public class MovieClearTraktTvTask extends TmmTask {


  public MovieClearTraktTvTask() {
    super(TmmResourceBundle.getString("trakt.clear"), 0, TaskType.BACKGROUND_TASK);
  }

  @Override
  protected void doInBackground() {
    TraktTv traktTV = TraktTv.getInstance();

    publishState(TmmResourceBundle.getString("trakt.clear.movies"), 0);
    traktTV.clearTraktMovies();
  }
}
