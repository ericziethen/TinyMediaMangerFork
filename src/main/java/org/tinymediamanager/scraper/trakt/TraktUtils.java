package org.tinymediamanager.scraper.trakt;

import com.uwetrottmann.trakt5.entities.CastMember;
import com.uwetrottmann.trakt5.entities.CrewMember;
import com.uwetrottmann.trakt5.entities.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaCastMember;

import java.util.Date;

import static org.tinymediamanager.scraper.MediaMetadata.IMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TMDB;

public class TraktUtils {

  /**
   * converts a LocalDate to Date
   *
   * @param date
   * @return Date or NULL
   */
  public static Date toDate(LocalDate date) {
    try {
      // we need to add time and zone to be able to convert :|
      LocalTime time = LocalTime.of(0, 0);
      Instant instant = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant();
      Date d = DateTimeUtils.toDate(instant);
      return d;
    } catch (Exception e) {
    }
    return null;
  }

  /**
   * converts a OffsetDateTime to Date
   *
   * @param date
   * @return Date or NULL
   */
  public static Date toDate(OffsetDateTime date) {
    try {
      Date d = DateTimeUtils.toDate(date.toInstant());
      return d;
    } catch (Exception e) {
    }
    return null;
  }

  /**
   * search result object is for all searches the same
   *
   * @param options
   * @param traktResult
   * @return
   */
  public static MediaSearchResult morphTraktResultToTmmResult(MediaSearchOptions options, SearchResult traktResult) {
    MediaSearchResult msr = new MediaSearchResult(TraktMetadataProvider.providerInfo.getId(), options.getMediaType());

    // ok, some duplicate code
    // but it is more maintainable, than some reflection/casting lookup
    if (traktResult.movie != null) {
      msr.setTitle(traktResult.movie.title);
      msr.setOverview(traktResult.movie.overview);
      msr.setYear(traktResult.movie.year);

      msr.setId(TraktMetadataProvider.providerInfo.getId(), String.valueOf(traktResult.movie.ids.trakt));
      if (traktResult.movie.ids.tmdb != null && traktResult.movie.ids.tmdb > 0) {
        msr.setId(TMDB, String.valueOf(traktResult.movie.ids.tmdb));
      }
      if (StringUtils.isNotBlank(traktResult.movie.ids.imdb)) {
        msr.setId(IMDB, traktResult.movie.ids.imdb);
      }
    }

    if (traktResult.show != null) {
      msr.setTitle(traktResult.show.title);
      msr.setOverview(traktResult.show.overview);
      msr.setYear(traktResult.show.year);

      msr.setId(TraktMetadataProvider.providerInfo.getId(), String.valueOf(traktResult.show.ids.trakt));
      if (traktResult.show.ids.tmdb != null && traktResult.show.ids.tmdb > 0) {
        msr.setId(TMDB, String.valueOf(traktResult.show.ids.tmdb));
      }
      if (StringUtils.isNotBlank(traktResult.show.ids.imdb)) {
        msr.setId(IMDB, traktResult.show.ids.imdb);
      }
    }

    if (traktResult.episode != null) {
      msr.setTitle(traktResult.episode.title);
      msr.setOverview(traktResult.episode.overview);
      msr.setYear(traktResult.episode.first_aired.getYear());

      msr.setId(TraktMetadataProvider.providerInfo.getId(), String.valueOf(traktResult.episode.ids.trakt));
      if (traktResult.episode.ids.tmdb != null && traktResult.episode.ids.tmdb > 0) {
        msr.setId(TMDB, String.valueOf(traktResult.episode.ids.tmdb));
      }
      if (StringUtils.isNotBlank(traktResult.episode.ids.imdb)) {
        msr.setId(IMDB, traktResult.episode.ids.imdb);
      }
    }

    return msr;
  }

  public static MediaCastMember toTmmCast(CrewMember crew, MediaCastMember.CastType type) {
    MediaCastMember cm = new MediaCastMember(type);
    cm.setName(crew.person.name);
    cm.setPart(crew.job);
    cm.setId(TraktMetadataProvider.providerInfo.getId(), crew.person.ids.trakt);
    cm.setId(MediaMetadata.IMDB, crew.person.ids.imdb);
    cm.setId(MediaMetadata.TMDB, crew.person.ids.tmdb);
    if (StringUtils.isNotBlank(crew.person.ids.slug)) {
      cm.setProfileUrl("https://trakt.tv/people/" + crew.person.ids.slug);
    }
    return cm;
  }

  public static MediaCastMember toTmmCast(CastMember crew, MediaCastMember.CastType type) {
    MediaCastMember cm = new MediaCastMember(type);
    cm.setName(crew.person.name);
    cm.setCharacter(crew.character);
    cm.setId(TraktMetadataProvider.providerInfo.getId(), crew.person.ids.trakt);
    cm.setId(MediaMetadata.IMDB, crew.person.ids.imdb);
    cm.setId(MediaMetadata.TMDB, crew.person.ids.tmdb);
    if (StringUtils.isNotBlank(crew.person.ids.slug)) {
      cm.setProfileUrl("https://trakt.tv/people/" + crew.person.ids.slug);
    }
    return cm;
  }

}