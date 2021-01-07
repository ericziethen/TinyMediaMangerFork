package org.tinymediamanager.scraper.omdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.BasicTest;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.omdb.entities.SeasonEntity;

/**
 * @author Wolfgang Janes
 */
public class ITOmdbMetadataProviderTest extends BasicTest {

  @Before
  public void setUpBeforeTest() throws Exception {
    setLicenseKey();
  }

  /**
   * Testing ProviderInfo
   */
  @Test
  public void testProviderInfo() {
    try {
      OmdbMovieMetadataProvider mp = new OmdbMovieMetadataProvider();
      MediaProviderInfo providerInfo = mp.getProviderInfo();

      assertNotNull(providerInfo.getDescription());
      assertNotNull(providerInfo.getId());
      assertNotNull(providerInfo.getName());
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  // Search
  @Test
  public void testSearch() {
    try {
      IMovieMetadataProvider mp = new OmdbMovieMetadataProvider();

      // Matrix
      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      options.setSearchQuery("The Matrix");
      List<MediaSearchResult> resultList = new ArrayList<>(mp.search(options));
      assertNotNull(resultList);
      assertThat(resultList.size()).isGreaterThan(0);
      assertThat(resultList.get(0).getTitle()).isEqualTo("The Matrix");
      assertThat(resultList.get(0).getYear()).isEqualTo(1999);
      assertThat(resultList.get(0).getIMDBId()).isEqualTo("tt0133093");
      assertThat(resultList.get(0).getMediaType()).isEqualTo(MediaType.MOVIE);
      assertThat(resultList.get(0).getPosterUrl()).isNotEmpty();

      // Men in Black
      options.setSearchQuery("Men in Black");
      resultList = new ArrayList<>(mp.search(options));
      assertNotNull(resultList);
      assertThat(resultList.size()).isGreaterThan(0);
      assertThat(resultList.get(0).getTitle()).isEqualTo("Men in Black");
      assertThat(resultList.get(0).getYear()).isEqualTo(1997);
      assertThat(resultList.get(0).getIMDBId()).isEqualTo("tt0119654");
      assertThat(resultList.get(0).getMediaType()).isEqualTo(MediaType.MOVIE);
      assertThat(resultList.get(0).getPosterUrl()).isNotEmpty();

    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  // Scrape by ID
  @Test
  public void testScrapeById() {
    try {
      IMovieMetadataProvider mp = new OmdbMovieMetadataProvider();

      MovieSearchAndScrapeOptions scrapeOptions = new MovieSearchAndScrapeOptions();
      scrapeOptions.setLanguage(MediaLanguages.en);
      scrapeOptions.setCertificationCountry(CountryCode.US);

      MediaMetadata md;

      // Matrix
      scrapeOptions.setImdbId("tt0133093");
      md = mp.getMetadata(scrapeOptions);
      assertThat(md.getCertifications()).isNotEmpty();
      assertThat(md.getTitle()).isEqualTo("The Matrix");
      assertThat(md.getYear()).isEqualTo(Integer.valueOf(1999));
      assertThat(md.getReleaseDate()).isNotNull();
      assertThat(md.getRuntime()).isEqualTo(Integer.valueOf(136));
      assertThat(md.getCastMembers(DIRECTOR)).isNotNull();
      assertThat(md.getCastMembers(DIRECTOR).size()).isEqualTo(2);
      assertThat(md.getCastMembers(WRITER)).isNotNull();
      assertThat(md.getCastMembers(WRITER).size()).isEqualTo(2);
      assertThat(md.getCastMembers(ACTOR)).isNotNull();
      assertThat(md.getCastMembers(ACTOR).size()).isEqualTo(4);
      assertThat(md.getPlot()).isNotEmpty();
      assertThat(md.getCountries()).contains("USA");
      assertThat(md.getSpokenLanguages()).contains("English");
      assertThat(md.getRatings()).hasSize(3);
      assertThat(md.getGenres()).contains(MediaGenres.ACTION, MediaGenres.SCIENCE_FICTION);
      assertThat(md.getMediaArt(MediaArtwork.MediaArtworkType.POSTER)).isNotNull();

      // Men in Black
      scrapeOptions.setImdbId(""); // empty IMDB!!
      scrapeOptions.setId(mp.getProviderInfo().getId(), "tt0119654");
      md = mp.getMetadata(scrapeOptions);
      assertThat(md.getCertifications()).isNotEmpty();
      assertThat(md.getTitle()).isEqualTo("Men in Black");
      assertThat(md.getYear()).isEqualTo(Integer.valueOf(1997));
      assertThat(md.getReleaseDate()).isNotNull();
      assertThat(md.getRuntime()).isEqualTo(Integer.valueOf(98));
      assertThat(md.getCastMembers(DIRECTOR)).isNotNull();
      assertThat(md.getCastMembers(DIRECTOR).size()).isEqualTo(1);
      assertThat(md.getCastMembers(WRITER)).isNotNull();
      assertThat(md.getCastMembers(WRITER).size()).isEqualTo(3);
      assertThat(md.getCastMembers(ACTOR)).isNotNull();
      assertThat(md.getCastMembers(ACTOR).size()).isEqualTo(4);
      assertThat(md.getPlot()).isNotEmpty();
      assertThat(md.getCountries()).contains("USA");
      assertThat(md.getSpokenLanguages()).contains("English", "Spanish");
      assertThat(md.getRatings()).hasSize(3);
      assertThat(md.getGenres()).contains(MediaGenres.ADVENTURE, MediaGenres.COMEDY);
      assertThat(md.getMediaArt(MediaArtwork.MediaArtworkType.POSTER)).isNotNull();

    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }

  }

  @Test
  public void testScrapeShow() {

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    OmdbTvShowMetadataProvider mp = new OmdbTvShowMetadataProvider();
    MediaMetadata md;

    options.setId(mp.getProviderInfo().getId(), "tt0944947");

    try {

      md = mp.getMetadata(options);

      assertThat(md.getCertifications()).isNotEmpty();
      assertThat(md.getTitle()).isEqualTo("Game of Thrones");
      assertThat(md.getYear()).isEqualTo(Integer.valueOf(2011));
      assertThat(md.getReleaseDate()).isNotNull();
      assertThat(md.getRuntime()).isEqualTo(Integer.valueOf(57));
      assertThat(md.getCastMembers(WRITER)).isNotNull();
      assertThat(md.getCastMembers(WRITER).size()).isEqualTo(2);
      assertThat(md.getCastMembers(ACTOR)).isNotNull();
      assertThat(md.getCastMembers(ACTOR).size()).isEqualTo(4);
      assertThat(md.getPlot()).isNotEmpty();
      assertThat(md.getCountries()).contains("USA", "UK");
      assertThat(md.getSpokenLanguages()).contains("English");
      assertThat(md.getRatings()).hasSize(1);
      assertThat(md.getGenres()).contains(MediaGenres.ACTION, MediaGenres.ADVENTURE, MediaGenres.DRAMA, MediaGenres.FANTASY, MediaGenres.ROMANCE);
      assertThat(md.getMediaArt(MediaArtwork.MediaArtworkType.POSTER)).isNotNull();

    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testEpisodeList() {

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    OmdbTvShowMetadataProvider mp = new OmdbTvShowMetadataProvider();
    List<MediaMetadata> mediaMetadataList = new ArrayList<>();

    options.setId(mp.getProviderInfo().getId(), "tt0944947");

    try {
      mediaMetadataList = mp.getEpisodeList(options);
    } catch (MissingIdException e) {
      e.printStackTrace();
    } catch (ScrapeException e) {
      e.printStackTrace();
    }

    assertThat(mediaMetadataList).isNotNull();

  }
}
