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
package org.tinymediamanager.scraper.thetvdb;

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.ALL;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_POSTER;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.Certification;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCastMember;
import org.tinymediamanager.scraper.entities.MediaCastMember.CastType;
import org.tinymediamanager.scraper.entities.MediaGenres;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaRating;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.exceptions.UnsupportedMediaTypeException;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.mediaprovider.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.mediaprovider.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.util.ApiKey;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.TvUtils;

import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.entities.Actor;
import com.uwetrottmann.thetvdb.entities.ActorsResponse;
import com.uwetrottmann.thetvdb.entities.Episode;
import com.uwetrottmann.thetvdb.entities.EpisodeResponse;
import com.uwetrottmann.thetvdb.entities.EpisodesResponse;
import com.uwetrottmann.thetvdb.entities.Language;
import com.uwetrottmann.thetvdb.entities.LanguagesResponse;
import com.uwetrottmann.thetvdb.entities.Series;
import com.uwetrottmann.thetvdb.entities.SeriesImageQueryResult;
import com.uwetrottmann.thetvdb.entities.SeriesImageQueryResultResponse;
import com.uwetrottmann.thetvdb.entities.SeriesImagesQueryParam;
import com.uwetrottmann.thetvdb.entities.SeriesImagesQueryParamResponse;
import com.uwetrottmann.thetvdb.entities.SeriesResponse;
import com.uwetrottmann.thetvdb.entities.SeriesResultsResponse;

import okhttp3.OkHttpClient;
import retrofit2.Response;

/**
 * The Class TheTvDbMetadataProvider.
 *
 * @author Manuel Laggner
 */
public class TheTvDbMetadataProvider implements ITvShowMetadataProvider, ITvShowArtworkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TheTvDbMetadataProvider.class);
  private static String artworkUrl = "http://thetvdb.com/banners/";
  private static final String TMM_API_KEY = ApiKey.decryptApikey("7bHHg4k0XhRERM8xd3l+ElhMUXOA5Ou4vQUEzYLGHt8=");
  private static final String FALLBACK_LANGUAGE = "fallbackLanguage";

  private final CacheMap<Integer, List<MediaMetadata>> episodeListCacheMap;

  private final MediaProviderInfo providerInfo;
  private TheTvdb tvdb;
  private List<Language> tvdbLanguages;

  public TheTvDbMetadataProvider() {
    // create the providerinfo
    providerInfo = createMediaProviderInfo();
    episodeListCacheMap = new CacheMap<>(600, 5);
  }

  private synchronized void initAPI() throws ScrapeException {
    String apiKey = TMM_API_KEY;
    String userApiKey = providerInfo.getConfig().getValue("apiKey");

    // check if the API should change from current key to user key
    if (StringUtils.isNotBlank(userApiKey) && tvdb != null && !userApiKey.equals(tvdb.apiKey())) {
      tvdb = null;
      apiKey = userApiKey;
    }

    // check if the API should change from current key to tmm key
    if (StringUtils.isBlank(userApiKey) && tvdb != null && !TMM_API_KEY.equals(tvdb.apiKey())) {
      tvdb = null;
      apiKey = TMM_API_KEY;
    }

    if (tvdb == null) {
      try {
        tvdb = new TheTvdb(apiKey) {
          // tell the tvdb api to use our OkHttp client
          private OkHttpClient okHttpClient;

          @Override
          protected synchronized OkHttpClient okHttpClient() {
            if (this.okHttpClient == null) {
              OkHttpClient.Builder builder = TmmHttpClient.newBuilder(true); // with cache
              this.setOkHttpClientDefaults(builder);
              this.okHttpClient = builder.build();
            }

            return this.okHttpClient;
          }
        };
        Response<LanguagesResponse> httpResponse = tvdb.languages().allAvailable().execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        LanguagesResponse response = httpResponse.body();
        if (response == null) {
          throw new Exception("Could not connect to TVDB");
        }
        tvdbLanguages = response.data;
      } catch (Exception e) {
        LOGGER.warn("could not initialize API: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }
  }

  private MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo providerInfo = new MediaProviderInfo("tvdb", "thetvdb.com",
            "<html><h3>The TV DB</h3><br />An open database for television fans. This scraper is able to scrape TV series metadata and artwork</html>",
        TheTvDbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/thetvdb_com.png"));
    providerInfo.setVersion(TheTvDbMetadataProvider.class);

    providerInfo.getConfig().addText("apiKey", "", true);

    ArrayList<String> fallbackLanguages = new ArrayList<>();
    for (MediaLanguages mediaLanguages : MediaLanguages.values()) {
      fallbackLanguages.add(mediaLanguages.toString());
    }
    providerInfo.getConfig().addSelect(FALLBACK_LANGUAGE, fallbackLanguages.toArray(new String[0]), MediaLanguages.en.toString());
    providerInfo.getConfig().load();

    return providerInfo;
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public MediaMetadata getMetadata(MediaScrapeOptions mediaScrapeOptions)
          throws ScrapeException, MissingIdException, UnsupportedMediaTypeException, NothingFoundException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("getting metadata: {}", mediaScrapeOptions);
    switch (mediaScrapeOptions.getType()) {
      case TV_SHOW:
        return getTvShowMetadata(mediaScrapeOptions);

      case TV_EPISODE:
        return getEpisodeMetadata(mediaScrapeOptions);

      default:
        throw new UnsupportedMediaTypeException(mediaScrapeOptions.getType());
    }
  }

  @Override
  public List<MediaSearchResult> search(MediaSearchOptions options) throws ScrapeException, UnsupportedMediaTypeException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("search() {}", options);
    List<MediaSearchResult> results = new ArrayList<>();

    if (options.getMediaType() != MediaType.TV_SHOW) {
      throw new UnsupportedMediaTypeException(options.getMediaType());
    }

    // detect the string to search
    String searchString = "";
    if (StringUtils.isNotEmpty(options.getQuery())) {
      searchString = options.getQuery();
    }
    // searchString = cleanString(searchString);

    String imdbId = options.getImdbId().isEmpty() ? null : options.getImdbId(); // do not submit empty string!
    if (MetadataUtil.isValidImdbId(searchString)) {
      imdbId = searchString; // search via IMDBid only
      searchString = null; // by setting empty searchterm
    }
    if (!StringUtils.isEmpty(imdbId)) {
      searchString = null; // null-out search string, when searching with IMDB, else 405
    }

    int tvdbId = options.getIdAsInt(providerInfo.getId());
    String language = options.getLanguage().getLanguage();
    String fallbackLanguage = MediaLanguages.get(providerInfo.getConfig().getValue(FALLBACK_LANGUAGE)).getLanguage(); // just 2 char

    List<Series> series = new ArrayList<>();

    // FALLBACK:
    // Accept-Language:
    // Records are returned with the Episode name and Overview in the desired language, if it exists.
    // If there is no translation for the given language, then the record is still returned but with empty values for the translated fields.

    // if we have an TVDB id, use that!
    if (tvdbId != 0) {
      LOGGER.debug("found TvDb ID {} - getting direct", tvdbId);
      try {

        // check with submitted language
        Response<SeriesResponse> httpResponse = tvdb.series().series(tvdbId, language).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        Series res = httpResponse.body().data;
        fillFallbackLanguages(language, fallbackLanguage, res);
        series.add(res);
      } catch (Exception e) {
        LOGGER.error("problem getting data vom tvdb via ID: {}", e.getMessage());
      }
    }

    // only search when we did not find something by ID (and search string or IMDB is present)
    if (series.isEmpty() && !(StringUtils.isEmpty(searchString) && StringUtils.isEmpty(imdbId))) {
      try {
        Response<SeriesResultsResponse> httpResponse = tvdb.search().series(searchString, imdbId, null, null, language).execute();
        if (!httpResponse.isSuccessful()) {
          // when not found in language -> 404
          if (!fallbackLanguage.equals(language)) {
            LOGGER.debug("not found - trying with fallback language {}", fallbackLanguage);
            httpResponse = tvdb.search().series(searchString, imdbId, null, null, fallbackLanguage).execute();
          }
          if (!httpResponse.isSuccessful()) {
            if (!fallbackLanguage.equals("en") && !language.equals("en")) {
              LOGGER.debug("not found - trying with EN language");
              httpResponse = tvdb.search().series(searchString, imdbId, null, null, "en").execute();
            }
            if (!httpResponse.isSuccessful()) {
              throw new HttpException(httpResponse.code(), httpResponse.message());
            }
          }
        }
        List<Series> res = httpResponse.body().data;
        for (Series s : res) {
          fillFallbackLanguages(language, fallbackLanguage, s);
        }
        series.addAll(res);
      } catch (Exception e) {
        LOGGER.error("problem getting data vom tvdb: {}", e.getMessage());
      }
    }

    if (series.isEmpty()) {
      return results;
    }

    // make sure there are no duplicates (e.g. if a show has been found in both languages)
    Map<Integer, MediaSearchResult> resultMap = new HashMap<>();

    for (Series show : series) {
      // check if that show has already a result
      if (resultMap.containsKey(show.id)) {
        continue;
      }

      // build up a new result
      MediaSearchResult result = new MediaSearchResult(providerInfo.getId(), options.getMediaType());
      result.setId(show.id.toString());
      result.setTitle(show.seriesName);
      result.setOverview(show.overview);
      try {
        result.setYear(Integer.parseInt(show.firstAired.substring(0, 4)));
      } catch (Exception ignored) {
        // ignore
      }

      // do not get banners instead of posters
      // if (StringUtils.isNotBlank(show.banner)) {
      // result.setPosterUrl(artworkUrl + show.banner);
      // }

      // for how the api responds only a banner - we would like to have a poster here;
      // just try to fetch the poster url
      try {
        Response<SeriesImageQueryResultResponse> httpResponse = tvdb.series().imagesQuery(show.id, "poster", null, null, null).execute();
        if (httpResponse.isSuccessful()) {
          SeriesImageQueryResultResponse response = httpResponse.body();
          if (response != null && !response.data.isEmpty()) {
            result.setPosterUrl(artworkUrl + response.data.get(0).fileName);
          }
        }
      } catch (Exception e) {
        LOGGER.warn("could not get poster for search result: {}", e.getMessage());
      }

      float score = MetadataUtil.calculateScore(searchString, show.seriesName);
      if (yearDiffers(options.getYear(), result.getYear())) {
        float diff = (float) Math.abs(options.getYear() - result.getYear()) / 100;
        LOGGER.debug("parsed year does not match search result year - downgrading score by {}", diff);
        score -= diff;
      }
      result.setScore(score);

      // results.add(result);
      resultMap.put(show.id, result);
    }

    // and convert all entries from the map to a list
    results.addAll(resultMap.values());

    // sort
    Collections.sort(results);
    Collections.reverse(results);

    return results;
  }

  private void fillFallbackLanguages(String language, String fallbackLanguage, Series serie) throws IOException {
    // check with fallback language
    Response<SeriesResponse> httpResponse;
    if ((StringUtils.isEmpty(serie.seriesName) || StringUtils.isEmpty(serie.overview)) && !fallbackLanguage.equals(language)) {
      LOGGER.trace("Getting show in fallback language {}", fallbackLanguage);
      httpResponse = tvdb.series().series(serie.id, fallbackLanguage).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      serie.seriesName = StringUtils.isEmpty(serie.seriesName) ? httpResponse.body().data.seriesName : serie.seriesName;
      serie.overview = StringUtils.isEmpty(serie.overview) ? httpResponse.body().data.overview : serie.overview;
    }

    // STILL empty? check with EN language...
    if ((StringUtils.isEmpty(serie.seriesName) || StringUtils.isEmpty(serie.overview)) && !fallbackLanguage.equals("en") && !language.equals("en")) {
      LOGGER.trace("Getting show in fallback language {}", "en");
      httpResponse = tvdb.series().series(serie.id, "en").execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      serie.seriesName = StringUtils.isEmpty(serie.seriesName) ? httpResponse.body().data.seriesName : serie.seriesName;
      serie.overview = StringUtils.isEmpty(serie.overview) ? httpResponse.body().data.overview : serie.overview;
    }
  }

  private void fillFallbackLanguages(String language, String fallbackLanguage, Episode episode) throws IOException {
    // check with fallback language
    Response<EpisodeResponse> httpResponse;
    if ((StringUtils.isEmpty(episode.episodeName) || StringUtils.isEmpty(episode.overview)) && !fallbackLanguage.equals(language)) {
      LOGGER.trace("Getting episode S{}E{} in fallback language {}", episode.airedSeason, episode.airedEpisodeNumber, fallbackLanguage);
      httpResponse = tvdb.episodes().get(episode.id, fallbackLanguage).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      episode.episodeName = StringUtils.isEmpty(episode.episodeName) ? httpResponse.body().data.episodeName : episode.episodeName;
      episode.overview = StringUtils.isEmpty(episode.overview) ? httpResponse.body().data.overview : episode.overview;
    }

    // STILL empty? check with EN language...
    if ((StringUtils.isEmpty(episode.episodeName) || StringUtils.isEmpty(episode.overview)) && !fallbackLanguage.equals("en")
            && !language.equals("en")) {
      LOGGER.trace("Getting episode S{}E{} in fallback language {}", episode.airedSeason, episode.airedEpisodeNumber, "en");
      httpResponse = tvdb.episodes().get(episode.id, "en").execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      episode.episodeName = StringUtils.isEmpty(episode.episodeName) ? httpResponse.body().data.episodeName : episode.episodeName;
      episode.overview = StringUtils.isEmpty(episode.overview) ? httpResponse.body().data.overview : episode.overview;
    }
  }

  private MediaMetadata getTvShowMetadata(MediaScrapeOptions options) throws ScrapeException, MissingIdException, NothingFoundException {
    MediaMetadata md = new MediaMetadata(providerInfo.getId());

    // do we have an id from the options?
    Integer id = options.getIdAsInteger(providerInfo.getId());
    if (id == null || id == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(providerInfo.getId());
    }
    String language = options.getLanguage().getLanguage();
    String fallbackLanguage = MediaLanguages.get(providerInfo.getConfig().getValue(FALLBACK_LANGUAGE)).getLanguage(); // just 2 char

    Series show = null;
    try {
      Response<SeriesResponse> httpResponse = tvdb.series().series(id, options.getLanguage().getLanguage()).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      show = httpResponse.body().data;
      fillFallbackLanguages(language, fallbackLanguage, show);
    } catch (Exception e) {
      LOGGER.error("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (show == null) {
      throw new NothingFoundException();
    }

    // populate metadata
    md.setId(providerInfo.getId(), show.id);
    md.setTitle(show.seriesName);
    if (StringUtils.isNotBlank(show.imdbId)) {
      md.setId(MediaMetadata.IMDB, show.imdbId);
    }
    if (StringUtils.isNotBlank(show.zap2itId)) {
      md.setId("zap2it", show.zap2itId);
    }
    md.setPlot(show.overview);

    try {
      md.setRuntime(Integer.valueOf(show.runtime));
    } catch (NumberFormatException e) {
      md.setRuntime(0);
    }

    MediaRating rating = new MediaRating(getProviderInfo().getId());
    rating.setRating(show.siteRating);
    rating.setVoteCount(TvUtils.parseInt(show.siteRatingCount));
    rating.setMaxValue(10);
    md.addRating(rating);

    try {
      md.setReleaseDate(StrgUtils.parseDate(show.firstAired));
    } catch (ParseException ignored) {
    }

    try {
      Date date = StrgUtils.parseDate(show.firstAired);
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      int y = calendar.get(Calendar.YEAR);
      md.setYear(y);
      if (y != 0 && md.getTitle().contains(String.valueOf(y))) {
        LOGGER.debug("Weird TVDB entry - removing date {} from title", y);
        String t = show.seriesName.replaceAll(String.valueOf(y), "").replaceAll("\\(\\)", "").trim();
        md.setTitle(t);
      }
    } catch (Exception ignored) {
    }

    md.setStatus(show.status);
    md.addProductionCompany(show.network);

    List<Actor> actors = new ArrayList<>();
    try {
      Response<ActorsResponse> httpResponse = tvdb.series().actors(id).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      actors.addAll(httpResponse.body().data);
    } catch (Exception e) {
      LOGGER.error("failed to get actors: {}", e.getMessage());
    }

    for (Actor actor : actors) {
      MediaCastMember member = new MediaCastMember(CastType.ACTOR);
      member.setId(providerInfo.getId(), actor.id);
      member.setName(actor.name);
      member.setCharacter(actor.role);
      if (StringUtils.isNotBlank(actor.image)) {
        member.setImageUrl(artworkUrl + actor.image);
      }

      md.addCastMember(member);
    }

    md.addCertification(Certification.findCertification(show.rating));

    // genres
    for (String genreAsString : show.genre) {
      md.addGenre(MediaGenres.getGenre(genreAsString));
    }

    return md;
  }

  private MediaMetadata getEpisodeMetadata(MediaScrapeOptions options) throws ScrapeException, NothingFoundException, MissingIdException {
    boolean useDvdOrder = false;

    // do we have an id from the options?
    Integer showId = options.getIdAsInteger(providerInfo.getId());

    if (showId == null || showId == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(providerInfo.getId());
    }

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    if (seasonNr == -1 || episodeNr == -1) {
      seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR_DVD, -1);
      episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR_DVD, -1);
      useDvdOrder = true;
    }

    Date releaseDate = null;
    if (options.getMetadata() != null && options.getMetadata().getReleaseDate() != null) {
      releaseDate = options.getMetadata().getReleaseDate();
    }
    if (releaseDate == null && (seasonNr == -1 || episodeNr == -1)) {
      LOGGER.warn("no aired date/season number/episode number found");
      throw new MissingIdException(MediaMetadata.EPISODE_NR, MediaMetadata.SEASON_NR);
    }

    // get the episode via the episodesList() (is cached and contains all data with 1 call per 100 eps)
    List<MediaMetadata> episodes = episodeListCacheMap.get(showId);
    if (episodes == null) {
      episodes = getEpisodeList(options);
    }

    // now search for the right episode in this list
    MediaMetadata foundEpisode = null;
    // first run - search with EP number
    for (MediaMetadata episode : episodes) {
      if (useDvdOrder && episode.getDvdSeasonNumber() == seasonNr && episode.getDvdEpisodeNumber() == episodeNr) {
        foundEpisode = episode;
        break;
      } else if (!useDvdOrder && episode.getSeasonNumber() == seasonNr && episode.getEpisodeNumber() == episodeNr) {
        foundEpisode = episode;
        break;
      }
    }
    if (foundEpisode == null && releaseDate != null) {
      // we did not find the episode via season/episode number - search via release date
      for (MediaMetadata episode : episodes) {
        if (episode.getReleaseDate() == releaseDate) {
          foundEpisode = episode;
          break;
        }
      }
    }

    if (foundEpisode == null) {
      throw new NothingFoundException();
    }

    return foundEpisode;
  }

  @Override
  public List<MediaArtwork> getArtwork(MediaScrapeOptions options) throws ScrapeException, MissingIdException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("getting artwork: {}", options);
    List<MediaArtwork> artwork = new ArrayList<>();

    // do we have an id from the options?
    Integer id = options.getIdAsInteger(providerInfo.getId());

    if (id == null || id == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(providerInfo.getId());
    }

    // get artwork from thetvdb
    List<SeriesImageQueryResult> images = new ArrayList<>();
    try {
      // get all types of artwork we can get
      SeriesImagesQueryParamResponse response = tvdb.series().imagesQueryParams(id).execute().body();
      for (SeriesImagesQueryParam param : response.data) {
        if (options.getArtworkType() == ALL || ("fanart".equals(param.keyType) && options.getArtworkType() == BACKGROUND)
                || ("poster".equals(param.keyType) && options.getArtworkType() == POSTER)
                || ("season".equals(param.keyType) && options.getArtworkType() == SEASON_POSTER)
                || ("seasonwide".equals(param.keyType) && options.getArtworkType() == SEASON_BANNER)
                || ("series".equals(param.keyType) && options.getArtworkType() == BANNER)) {

          try {
            Response<SeriesImageQueryResultResponse> httpResponse = tvdb.series().imagesQuery(id, param.keyType, null, null, null).execute();
            if (!httpResponse.isSuccessful()) {
              throw new HttpException(httpResponse.code(), httpResponse.message());
            }
            images.addAll(httpResponse.body().data);
          } catch (Exception e) {
            LOGGER.error("could not get artwork from tvdb: {}", e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("failed to get artwork: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (images.isEmpty()) {
      return artwork;
    }

    // sort it
    images.sort(new ImageComparator(options.getLanguage().getLanguage()));

    // build output
    for (SeriesImageQueryResult image : images) {
      MediaArtwork ma = null;

      // set artwork type
      switch (image.keyType) {
        case "fanart":
          ma = new MediaArtwork(providerInfo.getId(), BACKGROUND);
          break;

        case "poster":
          ma = new MediaArtwork(providerInfo.getId(), POSTER);
          break;

        case "season":
          ma = new MediaArtwork(providerInfo.getId(), SEASON_POSTER);
          try {
            ma.setSeason(Integer.parseInt(image.subKey));
          } catch (Exception e) {
            LOGGER.warn("could not parse season: {}", image.subKey);
          }
          break;

        case "seasonwide":
          ma = new MediaArtwork(providerInfo.getId(), SEASON_BANNER);
          try {
            ma.setSeason(Integer.parseInt(image.subKey));
          } catch (Exception e) {
            LOGGER.warn("could not parse season: {}", image.subKey);
          }
          break;

        case "series":
          ma = new MediaArtwork(providerInfo.getId(), BANNER);
          break;

        default:
          continue;
      }

      // extract image sizes
      if (StringUtils.isNotBlank(image.resolution)) {
        try {
          Pattern pattern = Pattern.compile("([0-9]{3,4})x([0-9]{3,4})");
          Matcher matcher = pattern.matcher(image.resolution);
          if (matcher.matches() && matcher.groupCount() > 1) {
            int width = Integer.parseInt(matcher.group(1));
            int height = Integer.parseInt(matcher.group(2));
            ma.addImageSize(width, height, artworkUrl + image.fileName);

            // set image size
            switch (ma.getType()) {
              case POSTER:
                if (width >= 1000) {
                  ma.setSizeOrder(MediaArtwork.PosterSizes.LARGE.getOrder());
                } else if (width >= 500) {
                  ma.setSizeOrder(MediaArtwork.PosterSizes.BIG.getOrder());
                } else if (width >= 342) {
                  ma.setSizeOrder(MediaArtwork.PosterSizes.MEDIUM.getOrder());
                } else {
                  ma.setSizeOrder(MediaArtwork.PosterSizes.SMALL.getOrder());
                }
                break;

              case BACKGROUND:
                if (width >= 3840) {
                  ma.setSizeOrder(MediaArtwork.FanartSizes.XLARGE.getOrder());
                }
                if (width >= 1920) {
                  ma.setSizeOrder(MediaArtwork.FanartSizes.LARGE.getOrder());
                } else if (width >= 1280) {
                  ma.setSizeOrder(MediaArtwork.FanartSizes.MEDIUM.getOrder());
                } else {
                  ma.setSizeOrder(MediaArtwork.FanartSizes.SMALL.getOrder());
                }
                break;

              default:
                break;
            }
          }
        } catch (Exception e) {
          LOGGER.debug("could not extract size from artwork: {}", image.resolution);
        }
      }

      // set size for banner & season poster (resolution not in api)
      if (ma.getType() == SEASON_BANNER || ma.getType() == SEASON_POSTER) {
        ma.setSizeOrder(MediaArtwork.FanartSizes.LARGE.getOrder());
      } else if (ma.getType() == BANNER) {
        ma.setSizeOrder(MediaArtwork.FanartSizes.MEDIUM.getOrder());
      }

      ma.setDefaultUrl(artworkUrl + image.fileName);
      if (StringUtils.isNotBlank(image.thumbnail)) {
        ma.setPreviewUrl(artworkUrl + image.thumbnail);
      } else {
        ma.setPreviewUrl(ma.getDefaultUrl());
      }

      // ma.setLanguage(banner.getLanguage());

      artwork.add(ma);
    }

    return artwork;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(MediaScrapeOptions options) throws ScrapeException, MissingIdException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("getting episode list: {}", options);
    List<MediaMetadata> episodes = new ArrayList<>();

    // do we have an show id from the options?
    Integer showId = options.getIdAsInteger(providerInfo.getId());
    if (showId == null || showId == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(providerInfo.getId());
    }

    List<Episode> eps = new ArrayList<>();
    try {
      String language = options.getLanguage().getLanguage();
      String fallbackLanguage = MediaLanguages.get(providerInfo.getConfig().getValue(FALLBACK_LANGUAGE)).getLanguage();

      // 100 results per page
      int counter = 1;
      while (true) {
        Response<EpisodesResponse> httpResponse = tvdb.series().episodes(showId, counter, language).execute();
        if (!httpResponse.isSuccessful() && counter == 1) {
          // error at the first fetch will result in an exception
          throw new HttpException(httpResponse.code(), httpResponse.message());
        } else if (!httpResponse.isSuccessful() && counter > 1) {
          // we got at least one page with results - maybe the episode count is the same as the pagination count
          break;
        }
        EpisodesResponse response = httpResponse.body();

        // fallback language
        for (Episode ep : response.data) {
          fillFallbackLanguages(language, fallbackLanguage, ep);
        }

        eps.addAll(response.data);
        if (response.data.size() < 100) {
          break;
        }

        counter++;
      }
    } catch (Exception e) {
      LOGGER.error("failed to get episode list: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    for (Episode ep : eps) {
      MediaMetadata episode = new MediaMetadata(providerInfo.getId());

      episode.setId(providerInfo.getId(), ep.id);
      episode.setSeasonNumber(TvUtils.getSeasonNumber(ep.airedSeason));
      episode.setEpisodeNumber(TvUtils.getEpisodeNumber(ep.airedEpisodeNumber));
      episode.setDvdSeasonNumber(TvUtils.getSeasonNumber(ep.dvdSeason));
      episode.setDvdEpisodeNumber(TvUtils.getEpisodeNumber(ep.dvdEpisodeNumber));
      episode.setTitle(ep.episodeName);
      episode.setPlot(ep.overview);

      try {
        episode.setReleaseDate(StrgUtils.parseDate(ep.firstAired));
      } catch (Exception ignored) {
        LOGGER.warn("Could not parse date: {}", ep.firstAired);
      }

      MediaRating rating = new MediaRating(getProviderInfo().getId());
      rating.setRating(ep.siteRating);
      rating.setVoteCount(TvUtils.parseInt(ep.siteRatingCount));
      rating.setMaxValue(10);
      episode.addRating(rating);

      // directors
      if (ep.directors != null && !ep.directors.isEmpty()) {
        for (String director : ep.directors) {
          String[] multiple = director.split(",");
          for (String g2 : multiple) {
            MediaCastMember cm = new MediaCastMember(CastType.DIRECTOR);
            cm.setName(g2.trim());
            episode.addCastMember(cm);
          }
        }
      }

      // writers
      if (ep.writers != null && !ep.writers.isEmpty()) {
        for (String writer : ep.writers) {
          String[] multiple = writer.split(",");
          for (String g2 : multiple) {
            MediaCastMember cm = new MediaCastMember(CastType.WRITER);
            cm.setName(g2.trim());
            episode.addCastMember(cm);
          }
        }
      }

      // actors (guests?)
      if (ep.guestStars != null && !ep.guestStars.isEmpty()) {
        for (String guest : ep.guestStars) {
          String[] multiple = guest.split(",");
          for (String g2 : multiple) {
            MediaCastMember cm = new MediaCastMember(CastType.ACTOR);
            cm.setName(g2.trim());
            episode.addCastMember(cm);
          }
        }
      }

      // Thumb
      if (StringUtils.isNotBlank(ep.filename) && (options.getArtworkType() == ALL || options.getArtworkType() == MediaArtworkType.THUMB)) {
        MediaArtwork ma = new MediaArtwork(providerInfo.getId(), MediaArtworkType.THUMB);
        ma.setPreviewUrl(artworkUrl + ep.filename);
        ma.setDefaultUrl(artworkUrl + ep.filename);
        episode.addMediaArt(ma);
      }

      episodes.add(episode);
    }

    // cache for further fast access
    episodeListCacheMap.put(showId, episodes);

    return episodes;
  }

  /**
   * Is i1 != i2 (when >0)
   */
  private boolean yearDiffers(int i1, int i2) {
    return i1 != 0 && i2 != 0 && i1 != i2;
  }

  // unneeded, tested it, and (semi)colons in query work well
  @Deprecated
  private String cleanString(String oldString) {
    if (StringUtils.isEmpty(oldString)) {
      return "";
    }
    // remove semicolons (for searies like "ChäoS;Child" or "Steins;Gate")
    String newString = oldString.replaceAll(";", "");

    return newString.trim();
  }

  /**********************************************************************
   * local helper classes
   **********************************************************************/
  private class ImageComparator implements Comparator<SeriesImageQueryResult> {
    private int preferredLangu = 0;
    private int english = 0;

    private ImageComparator(String language) {
      for (Language lang : tvdbLanguages) {
        if (language.equals(lang.abbreviation)) {
          preferredLangu = lang.id;
        }
        if ("en".equals(lang.abbreviation)) {
          english = lang.id;
        }
      }
    }

    /*
     * sort artwork: primary by language: preferred lang (ie de), en, others; then: score
     */
    @Override
    public int compare(SeriesImageQueryResult arg0, SeriesImageQueryResult arg1) {
      // check if first image is preferred langu

      if (arg0.languageId == preferredLangu && arg1.languageId != preferredLangu) {
        return -1;
      }

      // check if second image is preferred langu
      if (arg0.languageId != preferredLangu && arg1.languageId == preferredLangu) {
        return 1;
      }

      // check if the first image is en
      if (arg0.languageId == english && arg1.languageId != english) {
        return -1;
      }

      // check if the second image is en
      if (arg0.languageId != english && arg1.languageId == english) {
        return 1;
      }

      // swap arg0 and arg1 to sort reverse
      int result = Double.compare(arg1.ratingsInfo.average, arg0.ratingsInfo.average);

      // equal rating; sort by votes
      if (result == 0) {
        result = Integer.compare(arg1.ratingsInfo.count, arg0.ratingsInfo.count);
      }

      // if the result is still 0, we need to compare by ID (returning a zero here will treat it as a duplicate and remove the previous one)
      if (result == 0) {
        result = Integer.compare(arg1.id, arg0.id);
      }

      return result;
    }
  }
}