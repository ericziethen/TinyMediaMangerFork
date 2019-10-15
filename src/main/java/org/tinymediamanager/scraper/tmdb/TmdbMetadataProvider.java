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
package org.tinymediamanager.scraper.tmdb;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaGenres;
import org.tinymediamanager.scraper.entities.MediaTrailer;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.exceptions.UnsupportedMediaTypeException;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.mediaprovider.IMovieArtworkProvider;
import org.tinymediamanager.scraper.mediaprovider.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.mediaprovider.IMovieMetadataProvider;
import org.tinymediamanager.scraper.mediaprovider.IMovieSetMetadataProvider;
import org.tinymediamanager.scraper.mediaprovider.IMovieTmdbMetadataProvider;
import org.tinymediamanager.scraper.mediaprovider.IMovieTrailerProvider;
import org.tinymediamanager.scraper.mediaprovider.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.mediaprovider.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.util.ApiKey;

import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.TmdbInterceptor;
import com.uwetrottmann.tmdb2.entities.Configuration;
import com.uwetrottmann.tmdb2.entities.FindResults;
import com.uwetrottmann.tmdb2.entities.Genre;
import com.uwetrottmann.tmdb2.entities.Translations;
import com.uwetrottmann.tmdb2.entities.Translations.Translation;
import com.uwetrottmann.tmdb2.enumerations.ExternalSource;

import okhttp3.OkHttpClient;

/**
 * The Class TmdbMetadataProvider. A meta data, artwork and trailer provider for the site themoviedb.org
 *
 * @author Manuel Laggner
 */
public class TmdbMetadataProvider implements IMovieMetadataProvider, IMovieSetMetadataProvider, ITvShowMetadataProvider, IMovieArtworkProvider,
        ITvShowArtworkProvider, IMovieTrailerProvider, IMovieTmdbMetadataProvider, IMovieImdbMetadataProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TmdbMetadataProvider.class);
  private static final String TMM_API_KEY = ApiKey.decryptApikey("dj5KmN0AO0eFDMF1tybX3H+zxGpfm4pUQAlEhM3iah/g2kuCzUQVZiiJ+ceCP2DO");

  // Use primary translations, not just our internal MediaLanguages (we need the country!)
  // https://api.themoviedb.org/3/configuration/primary_translations?api_key=XXXX
  // And keep on duplicate languages the main country on first position!
  private static final String[] PT = new String[]{"ar-AE", "ar-SA", "be-BY", "bg-BG", "bn-BD", "ca-ES", "ch-GU", "cs-CZ", "da-DK",
          "de-DE", "el-GR", "en-US", "en-AU", "en-CA", "en-GB", "eo-EO", "es-ES", "es-MX", "eu-ES", "fr-FR", "fa-IR", "fi-FI", "fr-CA", "gl-ES", "he-IL",
          "hi-IN", "hu-HU", "id-ID", "it-IT", "ja-JP", "ka-GE", "kn-IN", "ko-KR", "lt-LT", "ml-IN", "nb-NO", "nl-NL", "no-NO", "pl-PL", "pt-BR", "pt-PT",
          "ro-RO", "ru-RU", "si-LK", "sk-SK", "sl-SI", "sr-RS", "sv-SE", "ta-IN", "te-IN", "th-TH", "tr-TR", "uk-UA", "vi-VN", "zh-CN", "zh-HK",
          "zh-TW"};

  static Tmdb api;
  static MediaProviderInfo providerInfo = createMediaProviderInfo();
  static Configuration configuration;

  public TmdbMetadataProvider() {
  }

  private static MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo providerInfo = new MediaProviderInfo("tmdb", "themoviedb.org",
            "<html><h3>The Movie Database (TMDb)</h3><br />The largest free movie database maintained by the community. It provides metadata and artwork<br />in many different languages. Thus it is the first choice for non english users<br /><br />Available languages: multiple</html>",
        TmdbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/themoviedb_org.png"));
    providerInfo.setVersion(TmdbMetadataProvider.class);

    providerInfo.getConfig().addText("apiKey", "", true);
    providerInfo.getConfig().addBoolean("includeAdult", false);
    providerInfo.getConfig().addBoolean("scrapeLanguageNames", true);
    providerInfo.getConfig().addBoolean("titleFallback", false);
    providerInfo.getConfig().addSelect("titleFallbackLanguage", PT, "en-US");
    providerInfo.getConfig().load();

    return providerInfo;
  }

  // thread safe initialization of the API
  private static synchronized void initAPI() throws ScrapeException {
    String apiKey = TMM_API_KEY;
    String userApiKey = providerInfo.getConfig().getValue("apiKey");

    // check if the API should change from current key to user key
    if (StringUtils.isNotBlank(userApiKey) && api != null && !userApiKey.equals(api.apiKey())) {
      api = null;
      apiKey = userApiKey;
    }

    // check if the API should change from current key to tmm key
    if (StringUtils.isBlank(userApiKey) && api != null && !TMM_API_KEY.equals(api.apiKey())) {
      api = null;
      apiKey = TMM_API_KEY;
    }

    // create a new instance of the tmdb api
    if (api == null) {
      try {
        api = new Tmdb(apiKey) {
          // tell the tmdb api to use our OkHttp client

          @Override
          protected synchronized OkHttpClient okHttpClient() {
            OkHttpClient.Builder builder = TmmHttpClient.newBuilder(true);
            builder.addInterceptor(new TmdbInterceptor(this));
            return builder.build();
          }
        };

        configuration = api.configurationService().configuration().execute().body();
        if (configuration == null) {
          throw new Exception("Invalid TMDB API key");
        }
      } catch (Exception e) {
        LOGGER.error("could not initialize the API: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public List<MediaSearchResult> search(MediaSearchOptions query) throws ScrapeException, UnsupportedMediaTypeException {
    // lazy initialization of the api
    initAPI();

    List<MediaSearchResult> searchResults;

    switch (query.getMediaType()) {
      case MOVIE:
        searchResults = new TmdbMovieMetadataProvider(api).search(query);
        break;

      case MOVIE_SET:
        searchResults = new TmdbMovieSetMetadataProvider(api).search(query);
        break;

      case TV_SHOW:
        searchResults = new TmdbTvShowMetadataProvider(api).search(query);
        break;

      default:
        throw new UnsupportedMediaTypeException(query.getMediaType());
    }

    Collections.sort(searchResults);
    Collections.reverse(searchResults);

    return searchResults;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(MediaScrapeOptions options) throws ScrapeException, UnsupportedMediaTypeException, MissingIdException {
    // lazy initialization of the api
    initAPI();

    switch (options.getType()) {
      case TV_SHOW:
      case TV_EPISODE:
        return new TmdbTvShowMetadataProvider(api).getEpisodeList(options);

      default:
        throw new UnsupportedMediaTypeException(options.getType());
    }
  }

  @Override
  public MediaMetadata getMetadata(MediaScrapeOptions options)
          throws ScrapeException, UnsupportedMediaTypeException, MissingIdException, NothingFoundException {
    // lazy initialization of the api
    initAPI();

    switch (options.getType()) {
      case MOVIE:
        return new TmdbMovieMetadataProvider(api).getMetadata(options);

      case MOVIE_SET:
        return new TmdbMovieSetMetadataProvider(api).getMetadata(options);

      case TV_SHOW:
      case TV_EPISODE:
        return new TmdbTvShowMetadataProvider(api).getMetadata(options);

      default:
        throw new UnsupportedMediaTypeException(options.getType());
    }
  }

  @Override
  public List<MediaArtwork> getArtwork(MediaScrapeOptions options) throws ScrapeException, MissingIdException {
    // lazy initialization of the api
    initAPI();

    return new TmdbArtworkProvider(api).getArtwork(options);
  }

  @Override
  public List<MediaTrailer> getTrailers(MediaScrapeOptions options) throws ScrapeException, UnsupportedMediaTypeException, MissingIdException {
    // lazy initialization of the api
    initAPI();

    switch (options.getType()) {
      case MOVIE:
        return new TmdbTrailerProvider(api).getTrailers(options);

      default:
        throw new UnsupportedMediaTypeException(options.getType());
    }
  }

  /**
   * get the tmdbId via the imdbId
   *
   * @param imdbId the imdbId
   * @param type   the MediaType to look for (we cannot search for movie, and take the TV entry!
   * @return the tmdbId or 0 if nothing has been found
   */
  public int getTmdbIdFromImdbId(String imdbId, MediaType type) {
    try {
      // lazy initialization of the api
      initAPI();

      FindResults findResults = api.findService().find(imdbId, ExternalSource.IMDB_ID, null).execute().body();
      // movie
      if (findResults != null && findResults.movie_results != null && !findResults.movie_results.isEmpty()
              && (type == MediaType.MOVIE || type == MediaType.MOVIE_SET)) {
        return findResults.movie_results.get(0).id;
      }

      // tv show
      if (findResults != null && findResults.tv_results != null && !findResults.tv_results.isEmpty()
              && (type == MediaType.TV_SHOW || type == MediaType.TV_EPISODE)) {
        return findResults.tv_results.get(0).id;
      }

    } catch (Exception e) {
      LOGGER.debug("failed to get tmdb id: " + e.getMessage());
    }

    return 0;
  }

  /**
   * tries to find correct title & overview from all the translations<br>
   * everything can be null/empty
   *
   * @param translations
   * @param locale
   * @return
   */
  private static Translation getTranslationForLocale(Translations translations, Locale locale) {
    Translation ret = null;

    if (translations != null && translations.translations != null && !translations.translations.isEmpty()) {
      for (Translation tr : translations.translations) {
        // check with language AND country
        if (tr.iso_639_1.equals(locale.getLanguage()) && tr.iso_3166_1.equals(locale.getCountry())) {
          ret = tr;
          break;
        }
      }

      if (ret == null) {
        // did not find exact translation, check again with language OR country
        for (Translation tr : translations.translations) {
          if (tr.iso_639_1.equals(locale.getLanguage()) || tr.iso_3166_1.equals(locale.getCountry())) {
            ret = tr;
            break;
          }
        }
      }
    }

    return ret;
  }

  /**
   * 0 is title(movie) or name(show)<br>
   * 1 is overview<br>
   * both may be empty, but never null
   *
   * @param translations
   * @param locale
   * @return
   */
  public static String[] getValuesFromTranslation(Translations translations, Locale locale) {
    String[] ret = new String[]{"", ""};

    Translation tr = getTranslationForLocale(translations, locale);
    if (tr == null || tr.data == null) {
      return ret;
    }

    if (!StringUtils.isEmpty(tr.data.title)) {
      ret[0] = tr.data.title; // movie
    }
    if (!StringUtils.isEmpty(tr.data.name)) {
      ret[0] = tr.data.name; // show
    }

    if (!StringUtils.isEmpty(tr.data.overview)) {
      ret[1] = tr.data.overview;
    }

    return ret;
  }

  /**
   * Maps scraper Genres to internal TMM genres
   */
  static MediaGenres getTmmGenre(Genre genre) {
    MediaGenres g = null;
    switch (genre.id) {
      case 28:
        g = MediaGenres.ACTION;
        break;
      case 12:
        g = MediaGenres.ADVENTURE;
        break;
      case 16:
        g = MediaGenres.ANIMATION;
        break;
      case 35:
        g = MediaGenres.COMEDY;
        break;
      case 80:
        g = MediaGenres.CRIME;
        break;
      case 105:
        g = MediaGenres.DISASTER;
        break;
      case 99:
        g = MediaGenres.DOCUMENTARY;
        break;
      case 18:
        g = MediaGenres.DRAMA;
        break;
      case 82:
        g = MediaGenres.EASTERN;
        break;
      case 2916:
        g = MediaGenres.EROTIC;
        break;
      case 10751:
        g = MediaGenres.FAMILY;
        break;
      case 10750:
        g = MediaGenres.FAN_FILM;
        break;
      case 14:
        g = MediaGenres.FANTASY;
        break;
      case 10753:
        g = MediaGenres.FILM_NOIR;
        break;
      case 10769:
        g = MediaGenres.FOREIGN;
        break;
      case 36:
        g = MediaGenres.HISTORY;
        break;
      case 10595:
        g = MediaGenres.HOLIDAY;
        break;
      case 27:
        g = MediaGenres.HORROR;
        break;
      case 10756:
        g = MediaGenres.INDIE;
        break;
      case 10402:
        g = MediaGenres.MUSIC;
        break;
      case 22:
        g = MediaGenres.MUSICAL;
        break;
      case 9648:
        g = MediaGenres.MYSTERY;
        break;
      case 10754:
        g = MediaGenres.NEO_NOIR;
        break;
      case 1115:
        g = MediaGenres.ROAD_MOVIE;
        break;
      case 10749:
        g = MediaGenres.ROMANCE;
        break;
      case 878:
        g = MediaGenres.SCIENCE_FICTION;
        break;
      case 10755:
        g = MediaGenres.SHORT;
        break;
      case 9805:
        g = MediaGenres.SPORT;
        break;
      case 10758:
        g = MediaGenres.SPORTING_EVENT;
        break;
      case 10757:
        g = MediaGenres.SPORTS_FILM;
        break;
      case 10748:
        g = MediaGenres.SUSPENSE;
        break;
      case 10770:
        g = MediaGenres.TV_MOVIE;
        break;
      case 53:
        g = MediaGenres.THRILLER;
        break;
      case 10752:
        g = MediaGenres.WAR;
        break;
      case 37:
        g = MediaGenres.WESTERN;
        break;
    }
    if (g == null) {
      g = MediaGenres.getGenre(genre.name);
    }
    return g;
  }
}