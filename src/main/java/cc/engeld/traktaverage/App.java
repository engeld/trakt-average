package cc.engeld.traktaverage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.AccessToken;
import com.uwetrottmann.trakt5.entities.DeviceCode;
import com.uwetrottmann.trakt5.entities.Episode;
import com.uwetrottmann.trakt5.entities.RatedEpisode;
import com.uwetrottmann.trakt5.entities.RatedSeason;
import com.uwetrottmann.trakt5.entities.RatedShow;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.UserSlug;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.RatingsFilter;
import com.uwetrottmann.trakt5.services.Users;

import retrofit2.Response;

/**
 * Hello world!
 *
 */
public class App {
	private static final Logger LOG = LogManager.getLogger();
	private static Properties prop = new Properties();

	private static TraktV2 trakt = null;

	public static void setTrakt(TraktV2 trakt) {
		App.trakt = trakt;
	}

	protected static TraktV2 getTrakt() {
		return trakt;
	}

	private static void loadProperties() {
		LOG.trace("loadProperties(): enter");
		try (InputStream input = new FileInputStream("app.properties")) {
			prop.load(input);
		} catch (IOException ex) {
			LOG.error("loadProperties(): " + ex.toString());
		}
		LOG.trace("loadProperties(): leave");
	}

	private static void loadTrakt() {
		LOG.trace("loadTrakt(): enter");
		setTrakt(trakt = new TraktV2(
				prop.getProperty("CLIENT_ID"), 
				prop.getProperty("CLIENT_SECRET"),
				"urn:ietf:wg:oauth:2.0:oob"));
		
		LOG.trace("loadTrakt(): leave");
	}

	public static void auth() {
		if (prop.getProperty("DEVICE_CODE").isEmpty()) {
			Response<DeviceCode> codeResponse = null;
			try {
				codeResponse = getTrakt().generateDeviceCode();
				DeviceCode deviceCode = codeResponse.body();
				System.out.println("Device Code: " + deviceCode.device_code);
				System.out.println("User Code: " + deviceCode.user_code);
				System.out.println("Enter the user code at the following URI: " + deviceCode.verification_url);

				System.out.println("Press \"ENTER\" when finished...");
				Scanner scanner = new Scanner(System.in);
				scanner.nextLine();
				scanner.close();

				prop.setProperty("DEVICE_CODE", deviceCode.device_code);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (prop.getProperty("ACCESS_TOKEN").isEmpty()) {
			try {
				Response<AccessToken> response = getTrakt()
						.exchangeDeviceCodeForAccessToken(prop.getProperty("DEVICE_CODE"));

				if (response.isSuccessful()) {
					AccessToken accessToken = response.body();
					System.out.println("Token: " + accessToken.access_token + " created at " + accessToken.created_at);
					prop.setProperty("ACCESS_TOKEN", accessToken.access_token);
				} else {
					if (response.code() == 401) {
						System.out.println("authorization required, supply a valid OAuth access token");
					} else {
						System.out.println("the request failed for some other reason");
						System.out.println(response.code());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static HashMap<String, HashMap<Integer, List<RatedEpisode>>> getAllRatedEpisodesGroupedByShows(Users users) {
		LOG.trace("getAllRatedEpisodesByShows(): enter");
		try {
			Response<List<RatedEpisode>> response = users.ratingsEpisodes(UserSlug.ME, RatingsFilter.ALL, Extended.FULL).execute();

			if (response.isSuccessful()) {
				List<RatedEpisode> episodes = response.body();
				HashMap<String, HashMap<Integer, List<RatedEpisode>>> ratedEpisodesGrouped = new HashMap<String, HashMap<Integer, List<RatedEpisode>>>();

				for (RatedEpisode episode : episodes) {
					if (!ratedEpisodesGrouped.containsKey(episode.show.title)) { // show not yet existent
						HashMap<Integer, List<RatedEpisode>> tmpHashMap = new HashMap<Integer, List<RatedEpisode>>();

						List<RatedEpisode> list = new ArrayList<RatedEpisode>();
						list.add(episode);

						tmpHashMap.put(episode.episode.season, list);
						ratedEpisodesGrouped.put(episode.show.title, tmpHashMap);
					} else { // shows already in map
						HashMap<Integer, List<RatedEpisode>> tmpHashMap = ratedEpisodesGrouped.get(episode.show.title);

						if (!tmpHashMap.containsKey(episode.episode.season)) { // show exists, but season doesnt
							List<RatedEpisode> list = new ArrayList<RatedEpisode>();
							list.add(episode);
							tmpHashMap.put(episode.episode.season, list);
							ratedEpisodesGrouped.replace(episode.show.title, tmpHashMap);
						} else { // show exists, season exists
							List<RatedEpisode> list = (List<RatedEpisode>) tmpHashMap.get(episode.episode.season);
							list.add(episode);

							tmpHashMap.replace(episode.episode.season, list);
							ratedEpisodesGrouped.replace(episode.show.title, tmpHashMap);
						}

					}
				}
				System.out.println("Rated " + episodes.size() + " Episodes from " + ratedEpisodesGrouped.size() + " Shows so far!");
				LOG.trace("getAllRatedEpisodesByShows(): leave");
				return ratedEpisodesGrouped;

			} else {
				if (response.code() == 401) {
					LOG.error("authorization required, supply a valid OAuth access token");
					return null;
				} else {
					LOG.error("the request failed for some other reason with response-code" + response.code());
					return null;
				}
			}
		} catch (Exception e) {
			LOG.error("getAllRatedEpisodesByShows(): houston, we have an exception: " + e.getMessage());
			return null;
		}
	}

	private static void printRatings(HashMap<String, HashMap<Integer, List<RatedEpisode>>> groupedEpisodes) {
		for (Entry<String, HashMap<Integer, List<RatedEpisode>>> entry : groupedEpisodes.entrySet()) {
			System.out.println("===========================================================================");
			System.out.println("> Show: " + entry.getKey());
			System.out.println("---------------------------------------------------------------------------");

			int showAverageRating = 0;
			HashMap<Integer, List<RatedEpisode>> value = (HashMap<Integer, List<RatedEpisode>>) entry.getValue(); // map<season, ratedEpi>
			for (Entry<Integer, List<RatedEpisode>> entry2 : value.entrySet()) {
				Integer season = entry2.getKey();
				List<RatedEpisode> seasonEpisodes = entry2.getValue();
				System.out.println();
				System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				System.out.println(">> Season " + season);
				System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

				int seasonAverageRating = 0;
				for (RatedEpisode episode : seasonEpisodes) {
					seasonAverageRating += episode.rating.value;
					System.out.println("+ Episode '" + episode.episode.title + "' (S0" + episode.episode.season + "E"
							+ episode.episode.number + ") was rated " + episode.rating + "/10");
				}

				System.out.println("");
				int seasonAverage = (seasonAverageRating / (seasonEpisodes.size()));
				System.out.println(">> Average season rating is: " + seasonAverage);
				showAverageRating += seasonAverage;
			}
			System.out.println("");
			int showAverage = (showAverageRating / (value.size()));
			System.out.println("> Average show rating is: " + showAverage);
			System.out.println("");
		}
	}
	
	private static HashMap<String, RatedSeason> getAllRatedSeasonsByShows(Users users) {
		LOG.trace("getAllRatedSeasonsByShows(): enter");
		try {
			Response<List<RatedSeason>> response = users.ratingsSeasons(UserSlug.ME, RatingsFilter.ALL, Extended.FULL).execute();

			if (response.isSuccessful()) {
				List<RatedSeason> seasons = response.body();
				HashMap<String, RatedSeason> ratedSeasonGrouped = new HashMap<String, RatedSeason>();
				
				for (RatedSeason ratedSeason : seasons) {
					System.out.println("Show " + ratedSeason.show.title + " - "+ ratedSeason.season.title + " was rated " + ratedSeason.rating + "/10");
				}
				

				LOG.trace("getAllRatedSeasonsByShows(): leave");
				return ratedSeasonGrouped;
			}
		} catch (Exception e) {
			LOG.error("getAllRatedSeasonsByShows(): houston, we have an exception: " + e.getMessage());
			return null;
		}
		return null;
	}
	
	private static List<RatedShow> getAllRatedShows(Users users){
		LOG.trace("getAllRatedShows(): enter");

		try {
			Response<List<RatedShow>> response = users.ratingsShows(UserSlug.ME, RatingsFilter.ALL, Extended.FULL).execute();

			if (response.isSuccessful()) {
				List<RatedShow> ratedShows = response.body();
				for (RatedShow ratedShow : ratedShows) {
					System.out.println("Show " + ratedShow.show.title + " was rated " + ratedShow.rating + "/10");
				}

				LOG.trace("getAllRatedShows(): leave");
				return ratedShows;
			}
		} catch (Exception e) {
			LOG.error("getAllRatedShows(): houston, we have an exception: " + e.getMessage());
			return null;
		}
		return null;
	}

	private static void cleanupProperties() {
		LOG.trace("cleanupProperties(): enter");

		// save external properties
		try (OutputStream output = new FileOutputStream("app.properties")) {
			prop.store(output, null);
		} catch (FileNotFoundException e) {
			LOG.error("cleanupProperties(): " + e.toString());
		} catch (IOException e) {
			LOG.error("cleanupProperties(): " + e.toString());
		}
		LOG.trace("cleanupProperties(): leave");
	}

	public static void main(String[] args) {
		Configurator.setRootLevel(Level.WARN);
		LOG.trace("main(): enter");

		loadProperties();
		loadTrakt();
		auth();

		LOG.debug("main(): accessToken = " + prop.getProperty("ACCESS_TOKEN"));
		trakt.accessToken(prop.getProperty("ACCESS_TOKEN"));
		
		// map<Show, map<Season, ratedEpi>>
		HashMap<String, HashMap<Integer, List<RatedEpisode>>> ratedEpisodesGrouped = getAllRatedEpisodesGroupedByShows(trakt.users());
		HashMap<String, RatedSeason> ratedSeasonGrouped = getAllRatedSeasonsByShows(trakt.users());
		List<RatedShow> ratedShow = getAllRatedShows(trakt.users());
		
		// compare the rated seasons/shows to calulated averages rating
//		if (ratedEpisodesGrouped != null) {
//			printRatings(ratedEpisodesGrouped);
//			compareRatings(episodeGrouped);
//		}

		cleanupProperties();
		LOG.trace("main(): leave");
	}

}
