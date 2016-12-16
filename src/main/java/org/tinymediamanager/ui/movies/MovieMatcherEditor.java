/*
 * Copyright 2012 - 2016 Manuel Laggner
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
package org.tinymediamanager.ui.movies;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSearchOptions;
import org.tinymediamanager.core.movie.entities.Movie;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * The Class MovieMatcherEditor.
 * 
 * @author Manuel Laggner
 */
public class MovieMatcherEditor extends AbstractMatcherEditor<Movie> {
  private final Set<IMovieUIFilter>    filters;
  private final PropertyChangeListener filterChangeListener;

  /**
   * Instantiates a new movie matcher editor.
   */
  public MovieMatcherEditor() {
    filters = new HashSet<>();
    filterChangeListener = evt -> updateFiltering();
  }

  /**
   * Add a new UI filter to this matcher
   * 
   * @param filter
   *          the new filter to be added
   */
  public void addFilter(IMovieUIFilter filter) {
    filter.addPropertyChangeListener(filterChangeListener);
    filters.add(filter);
  }

  /**
   * set any stored filter values
   *
   * @param values
   *          the values to be set
   */
  public void setFilterValues(Map<String, String> values) {
    boolean fireFilterChanged = false;

    for (Map.Entry<String, String> entry : values.entrySet()) {
      if (StringUtils.isBlank(entry.getKey())) {
        continue;
      }
      for (IMovieUIFilter filter : filters) {
        if (filter.getId().equals(entry.getKey())) {
          filter.setActive(true);
          filter.setFilterValue(entry.getValue());
          fireFilterChanged = true;
        }
      }
    }

    if (fireFilterChanged) {
      updateFiltering();
    }
  }

  /**
   * re-filter the list
   */
  private void updateFiltering() {
    Matcher<Movie> matcher = new MovieMatcher(new HashSet<>(filters));
    fireChanged(matcher);

    if (MovieModuleManager.SETTINGS.isStoreUiFilters()) {
      Map<String, String> filterValues = new HashMap<>();
      for (IMovieUIFilter filter : filters) {
        if (filter.isActive()) {
          filterValues.put(filter.getId(), filter.getFilterValueAsString());
        }
      }
      MovieModuleManager.SETTINGS.setUiFilters(filterValues);
      Globals.settings.saveSettings();
    }
  }

  /**
   * Filter movies.
   * 
   * @param filter
   *          the filter
   */
  @Deprecated
  public void filterMovies(Map<MovieSearchOptions, Object> filter) {
    Matcher<Movie> matcher = new MovieExtendedMatcher(filter);
    fireChanged(matcher);
    // if (MovieModuleManager.SETTINGS.isStoreUiFilters()) {
    // MovieModuleManager.SETTINGS.setUiFilters(filter);
    // Globals.settings.saveSettings();
    // }
  }

  /*
   * helper class for running all filters against the given movie
   */
  private class MovieMatcher implements Matcher<Movie> {
    private final Set<IMovieUIFilter> filters;

    public MovieMatcher(Set<IMovieUIFilter> filters) {
      this.filters = filters;
    }

    @Override
    public boolean matches(Movie movie) {
      for (IMovieUIFilter filter : filters) {
        if (filter.isActive() && !filter.accept(movie)) {
          return false;
        }
      }

      return true;
    }
  }
}
