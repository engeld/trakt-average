package cc.engeld.traktaverage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.AccessToken;
import com.uwetrottmann.trakt5.entities.BaseEpisode;
import com.uwetrottmann.trakt5.entities.BaseSeason;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.entities.DeviceCode;
import com.uwetrottmann.trakt5.entities.Episode;
import com.uwetrottmann.trakt5.entities.RatedEpisode;
import com.uwetrottmann.trakt5.entities.RatedShow;
import com.uwetrottmann.trakt5.entities.Season;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.UserSlug;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.Rating;
import com.uwetrottmann.trakt5.enums.RatingsFilter;
import com.uwetrottmann.trakt5.services.Shows;
import com.uwetrottmann.trakt5.services.Users;

import retrofit2.Response;

/**
 * Hello world!
 *
 */
public class App 
{
	private static final String CLIENT_ID = "";
	private static final String CLIENT_SECRET = "";
    private static final String DEVICE_CODE = "";
	private static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    
    private static final Logger LOG = LogManager.getLogger();  
    private static final TraktV2 trakt = new TraktV2(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
    
    protected static TraktV2 getTrakt() {
        return trakt;
    }
    
    private static String accessToken = "";
	
	public static void auth() {
		if(DEVICE_CODE.isEmpty()) {
			Response<DeviceCode> codeResponse = null;
			try {
				codeResponse = getTrakt().generateDeviceCode();
				DeviceCode deviceCode = codeResponse.body();
		        System.out.println("Device Code: " + deviceCode.device_code);
		        System.out.println("User Code: " + deviceCode.user_code);
		        System.out.println("Enter the user code at the following URI: " + deviceCode.verification_url);
		        System.out.println("Set the TEST_DEVICE_CODE variable to run the access token test");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        
        // get accesstoken
		if(accessToken == "") {
			try {
				Response<AccessToken> response = getTrakt().exchangeDeviceCodeForAccessToken(DEVICE_CODE);
				
				if(response.isSuccessful()) {
					AccessToken accessToken = response.body();
					System.out.println("Token: " + accessToken.access_token + " created at " + accessToken.created_at);
				}else {
	    	        if (response.code() == 401) {
	    	        	System.out.println("authorization required, supply a valid OAuth access token");
	    	            // authorization required, supply a valid OAuth access token
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
    
    
    public static void main( String[] args ) {
        Configurator.setRootLevel(Level.TRACE);

    	LOG.trace("main(): enter");
//    	auth();

    	LOG.info("main(): accessToken = " + accessToken);
    	trakt.accessToken(accessToken);
    	Users users = trakt.users();
    	
    	getAllRatedEpisodesGroupedByShows(users);
    	LOG.trace("main(): leave");
    }

	private static void getAllRatedEpisodesGroupedByShows(Users users) {
		LOG.trace("getAllRatedEpisodesByShows(): enter");
		try {
    		Response<List<RatedEpisode>> response = users.ratingsEpisodes(UserSlug.ME, RatingsFilter.ALL, Extended.FULL).execute();
   		
    	    if (response.isSuccessful()) {
    	        List<RatedEpisode> episodes = response.body();
    	        
    	        HashMap<String, HashMap<Integer, List<RatedEpisode>>> hashMap2 = new HashMap<String, HashMap<Integer, List<RatedEpisode>>>();

    	        for (RatedEpisode episode : episodes) {
    	            Show tmpShow = episode.show;
	        		Episode epi = episode.episode;

    	        	if (!hashMap2.containsKey(tmpShow.title)) { // show not yet existent
    	        		HashMap<Integer, List<RatedEpisode>> tmpHashMap = new HashMap<Integer, List<RatedEpisode>>();

    	        	    List<RatedEpisode> list = new ArrayList<RatedEpisode>();
    	        	    list.add(episode);
    	        	    
    	        	    tmpHashMap.put(epi.season, list);
    	        	    hashMap2.put(tmpShow.title, tmpHashMap);
    	        	} else { // shows already in map
    	        	    HashMap<Integer, List<RatedEpisode>> tmpHashMap = hashMap2.get(tmpShow.title);
    	        	    
    	        	    if(!tmpHashMap.containsKey(epi.season)) { // show exists, but season doesnt
    	        	    	List<RatedEpisode> list = new ArrayList<RatedEpisode>();
        	        	    list.add(episode);
        	        	    tmpHashMap.put(epi.season, list);
        	        	    hashMap2.replace(tmpShow.title, tmpHashMap);
    	        	    } else { // show exists, season exists
    	    	            List<RatedEpisode> list = (List<RatedEpisode>) tmpHashMap.get(epi.season);
    	    	            list.add(episode);
    	    	            
        	        	    tmpHashMap.replace(epi.season, list);
        	        	    hashMap2.replace(tmpShow.title, tmpHashMap);
    	        	    }
    	        	    
    	        	}
    	        }
    	        
    	        for (Entry<String, HashMap<Integer, List<RatedEpisode>>> entry : hashMap2.entrySet()) {
    	            String key = entry.getKey(); // tv show title
    	            HashMap<Integer, List<RatedEpisode>> value = (HashMap<Integer, List<RatedEpisode>>) entry.getValue(); // map<season, ratedEpi>
    	            
		        	System.out.println("===========================================================================");
		        	System.out.println("> Show: "+ entry.getKey());
		        	System.out.println("---------------------------------------------------------------------------");
		        	
		        	int showAverageRating = 0;
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
		    	            System.out.println("+ Episode '" + episode.episode.title + "' (S0"+ episode.episode.season + "E"+episode.episode.number +") was rated " + episode.rating + "/10");
						}
		        		
			        	System.out.println("");
			        	int seasonAverage = (seasonAverageRating/(seasonEpisodes.size()));
		        		System.out.println(">> Average season rating is: " + seasonAverage);
		        		showAverageRating += seasonAverage;
		        	}
		        	System.out.println("");
		        	int showAverage = (showAverageRating/(value.size()));
	        		System.out.println("> Average show rating is: " + showAverage);
		        	System.out.println("");
    	        }
    	        
    	        
    	        System.out.println("Rated " + episodes.size() + " Episodes from " + hashMap2.size() + " Shows so far!");
    	    } else {
    	        if (response.code() == 401) {
    	        	LOG.error("authorization required, supply a valid OAuth access token");
    	        } else {
    	        	LOG.error("the request failed for some other reason with response-code" + response.code());
    	        }
    	    }
    	} catch (Exception e) {
    		LOG.error("getAllRatedEpisodesByShows(): houston, we have an exception: " + e.getMessage());
    	}		
		LOG.trace("getAllRatedEpisodesByShows(): leave");
	}

	
	public static boolean containsShow(Collection<Show> c, String showTitle) {
	    for(Show o : c) {
	        if(o != null && o.title.equals(showTitle)) {
	            return true;
	        }
	    }
	    return false;
	}
	
}


