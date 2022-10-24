import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**.
 * Movie analyzer
 *
 * @author gongyantong
 */
public class MovieAnalyzer {

  private static final String CSV_SPLIT_BY = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";

  /* Movie list */
  private List<Movies> movies;

  /**.
   * constructor
   *
   * @param datasetPath the path of dataset
   * @author gongyantong
   */
  public MovieAnalyzer(String datasetPath) {

    this.movies = new ArrayList<>();

    // Read the csv
    BufferedReader reader;
    String line;
    try {
      reader = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
      reader.readLine();
      String[] filmInfo;
      while ((line = reader.readLine()) != null) {
        Movies movie = new Movies();
        filmInfo = line.split(CSV_SPLIT_BY, -1);
        for (int i = 0; i < filmInfo.length; i++) {
          if (filmInfo[i].startsWith("\"")) {
            filmInfo[i] = filmInfo[i].substring(1);
          }
          if (filmInfo[i].endsWith("\"")) {
            filmInfo[i] = filmInfo[i].substring(0, filmInfo[i].length() - 1);
          }
        }
        movie.setSeriesTitle(filmInfo[1]);
        movie.setReleasedYear(Integer.valueOf(filmInfo[2]));
        movie.setCertificate(filmInfo[3]);
        filmInfo[4] = filmInfo[4].replace(" min", "");
        movie.setRuntime(Integer.valueOf(filmInfo[4]));
        movie.setGenre(Arrays.stream(filmInfo[5].split(", ")).toList());
        movie.setImdbRating(Float.valueOf(filmInfo[6]));
        movie.setOverview(filmInfo[7]);
        filmInfo[8] = filmInfo[8].replace(",", "").replace("\"", "");
        if (filmInfo[8].equals("")) {
          movie.setMetaScore(null);
        } else {
          movie.setMetaScore(Integer.valueOf(filmInfo[8]));
        }
        movie.setDirector(filmInfo[9]);
        List<String> star = new ArrayList<>();
        star.add(filmInfo[10]);
        star.add(filmInfo[11]);
        star.add(filmInfo[12]);
        star.add(filmInfo[13]);
        movie.setStars(star);
        movie.setNoOfVotes(Integer.valueOf(filmInfo[14]));
        filmInfo[15] = filmInfo[15].replace(",", "").replace("\"", "");
        if (filmInfo[15].equals("")) {
          movie.setGross(null);
        } else {
          movie.setGross(Integer.valueOf(filmInfo[15]));
        }
        this.movies.add(movie);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**.
   * getMovieCountByYear
   *
   * @return Map
   * @author gongyantong
   */
  public Map<Integer, Integer> getMovieCountByYear() {
    Map<Integer, Long> count = movies.stream()
            .collect(Collectors.groupingBy(Movies::getReleasedYear, Collectors.counting()));
    Map<Integer, Integer> result = new LinkedHashMap<>();
    for (Integer key : count.keySet().stream().sorted(Comparator.reverseOrder()).toList()) {
      result.put(key, count.get(key).intValue());
    }
    return result;
  }

  /**.
   * getMovieCountByGenre
   *
   * @return Map
   * @author gongyantong
   */
  public Map<String, Integer> getMovieCountByGenre() {
    List<String> strings = new ArrayList<>();
    for (Movies movie : this.movies) {
      if (movie.getGenre() == null) {
        continue;
      }
      strings.addAll(movie.getGenre());
    }
    Map<String, Long> count = strings.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    Map<String, Integer> intCount = new HashMap<>();
    for (String key : count.keySet()) {
      intCount.put(key, count.get(key).intValue());
    }
    Map<String, Integer> result = new LinkedHashMap<>();
    List<Map.Entry<String, Integer>> list = new ArrayList<>(intCount.entrySet());
    list.sort((o1, o2) -> {
      if (o1.getValue().compareTo(o2.getValue()) == 0) {
        return o1.getKey().compareTo(o2.getKey());
      } else {
        return o2.getValue().compareTo(o1.getValue());
      }
    });
    for (Map.Entry<String, Integer> map : list) {
      result.put(map.getKey(), map.getValue());
    }
    return result;
  }

  /**.
   * getCoStarCount
   *
   * @return Map
   * @author gongyantong
   */
  public Map<List<String>, Integer> getCoStarCount() {
    List<List<String>> coStarTable = new ArrayList<>();
    for (Movies movie : this.movies) {
      List<String> stars = movie.getStars();
      for (int i = 0; i < stars.size() - 1; i++) {
        for (int j = i + 1; j < stars.size(); j++) {
          List<String> costar = new ArrayList<>();
          costar.add(stars.get(i));
          costar.add(stars.get(j));
          costar.sort(String::compareTo);
          coStarTable.add(costar);
        }
      }
    }
    Map<List<String>, Long> count = coStarTable.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    Map<List<String>, Integer> intCount = new HashMap<>();
    for (List<String> key : count.keySet()) {
      intCount.put(key, count.get(key).intValue());
    }
    Map<List<String>, Integer> result = new LinkedHashMap<>();
    List<Map.Entry<List<String>, Integer>> list = new ArrayList<>(intCount.entrySet());
    list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
    for (Map.Entry<List<String>, Integer> map : list) {
      result.put(map.getKey(), map.getValue());
    }
    return result;
  }

  /**.
   * getTopMovies
   *
   * @param top_k the # of movies needed
   * @param by sorted by what
   * @return List
   * @author gongyantong
   */
  public List<String> getTopMovies(int top_k, String by) {
    List<Movies> sortedList = new ArrayList<>();
    if ("runtime".equals(by)) {
      sortedList = this.movies.stream()
              .sorted(Comparator.comparing(Movies::getRuntime).reversed()
                      .thenComparing(Movies::getSeriesTitle)).toList();
    } else if ("overview".equals(by)) {
      Comparator<Movies> comparator =
              (o1, o2) -> o2.getOverview().length() - o1.getOverview().length();
      sortedList = this.movies.stream()
              .sorted(comparator.thenComparing(Movies::getSeriesTitle)).toList();
    }
    List<String> result = new ArrayList<>();
    for (int i = 0; i < top_k; i++) {
      result.add(sortedList.get(i).getSeriesTitle());
    }
    return result;
  }

  /**.
   * getTopStars
   *
   * @param top_k the # of stars needed
   * @param by sorted by what
   * @return List
   * @author gongyantong
   */
  public List<String> getTopStars(int top_k, String by) {
    List<String> result = new ArrayList<>();
    if ("rating".equals(by)) {
      Map<String, Double> ratingMap = new HashMap<>();
      Map<String, Integer> countMap = new HashMap<>();
      Map<String, Double> averageRatingMap = new HashMap<>();
      for (Movies movie : this.movies) {
        List<String> stars = movie.getStars();
        for (String key : stars) {
          ratingMap.put(key, ratingMap.getOrDefault(key, 0.0d) + movie.getImdbRating());
          countMap.put(key, countMap.getOrDefault(key, 0) + 1);
        }
      }
      for (String key : ratingMap.keySet()) {
        averageRatingMap.put(key, (ratingMap.get(key) / countMap.get(key)));
      }
      List<Map.Entry<String, Double>> ratingList = new ArrayList<>(averageRatingMap.entrySet());
      ratingList.sort((o1, o2) -> {
        if (o1.getValue().compareTo(o2.getValue()) == 0) {
          return o1.getKey().compareTo(o2.getKey());
        } else {
          return o2.getValue().compareTo(o1.getValue());
        }
      });
      for (int i = 0; i < top_k; i++) {
        result.add(ratingList.get(i).getKey());
      }
    } else if ("gross".equals(by)) {
      Map<String, Long> grossMap = new HashMap<>();
      Map<String, Integer> countMap = new HashMap<>();
      Map<String, Double> averageGrossMap = new HashMap<>();
      for (Movies movie : this.movies) {
        if (movie.getGross() == null) {
          continue;
        }
        List<String> stars = movie.getStars();
        for (String key : stars) {
          grossMap.put(key, grossMap.getOrDefault(key, 0L) + movie.getGross());
          countMap.put(key, countMap.getOrDefault(key, 0) + 1);
        }

      }
      for (String key : grossMap.keySet()) {
        averageGrossMap.put(key, (double) (grossMap.get(key) / countMap.get(key)));
      }
      List<Map.Entry<String, Double>> grossList = new ArrayList<>(averageGrossMap.entrySet());
      grossList.sort((o1, o2) -> {
        if (o1.getValue().compareTo(o2.getValue()) == 0) {
          return o1.getKey().compareTo(o2.getKey());
        } else {
          return o2.getValue().compareTo(o1.getValue());
        }
      });
      for (int j = 0; j < top_k; j++) {
        result.add(grossList.get(j).getKey());
      }
    }
    return result;
  }

  /**.
   * searchMovies
   *
   * @param genre genre
   * @param min_rating the min IMDB rating
   * @param max_runtime the max runtime
   * @return list
   * @author gongyantong
   */
  public List<String> searchMovies(String genre, float min_rating, int max_runtime) {
    List<String> result = new ArrayList<>();
    for (Movies movie : this.movies) {
      if (movie.getGenre().contains(genre)
              && (movie.getImdbRating() >= min_rating)
              && movie.getRuntime() <= max_runtime) {
        result.add(movie.getSeriesTitle());
      }
    }
    result.sort(String::compareTo);
    return result;
  }
}

/**.
 * Movies
 *
 * @author gongyantong
 */
class Movies {

  /* Name of the movie */
  private String seriesTitle;

  /* Year at which that movie released */
  private Integer releasedYear;

  /* Certificate earned by that movie*/
  private String certificate;

  /* Total runtime of the movie */
  private Integer runtime;

  /* Genre of the movie */
  private List<String> genre;

  /* Rating of the movie at IMDB site */
  private Float imdbRating;

  /* mini story / summary */
  private String overview;

  /* Score eared by the movie */
  private Integer metaScore;

  /* Name of the director */
  private String director;

  /* Name of the Stars */
  private List<String> stars;

  /* Total number of votes */
  private Integer noOfVotes;

  /* Money earned by that movie */
  private Integer gross;

  public String getSeriesTitle() {
    return seriesTitle;
  }

  public void setSeriesTitle(String seriesTitle) {
    this.seriesTitle = seriesTitle;
  }

  public Integer getReleasedYear() {
    return releasedYear;
  }

  public void setReleasedYear(Integer releasedYear) {
    this.releasedYear = releasedYear;
  }

  public String getCertificate() {
    return certificate;
  }

  public void setCertificate(String certificate) {
    this.certificate = certificate;
  }

  public Integer getRuntime() {
    return runtime;
  }

  public void setRuntime(Integer runtime) {
    this.runtime = runtime;
  }

  public List<String> getGenre() {
    return genre;
  }

  public void setGenre(List<String> genre) {
    this.genre = genre;
  }

  public Float getImdbRating() {
    return imdbRating;
  }

  public void setImdbRating(Float imdbRating) {
    this.imdbRating = imdbRating;
  }

  public String getOverview() {
    return overview;
  }

  public void setOverview(String overview) {
    this.overview = overview;
  }

  public Integer getMetaScore() {
    return metaScore;
  }

  public void setMetaScore(Integer metaScore) {
    this.metaScore = metaScore;
  }

  public String getDirector() {
    return director;
  }

  public void setDirector(String director) {
    this.director = director;
  }

  public List<String> getStars() {
    return stars;
  }

  public void setStars(List<String> stars) {
    this.stars = stars;
  }

  public Integer getNoOfVotes() {
    return noOfVotes;
  }

  public void setNoOfVotes(Integer noOfVotes) {
    this.noOfVotes = noOfVotes;
  }

  public Integer getGross() {
    return gross;
  }

  public void setGross(Integer gross) {
    this.gross = gross;
  }
}