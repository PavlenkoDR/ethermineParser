import java.net.HttpURLConnection;
import java.net.URL;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {

	// HTTP GET request
	
	private static String sendGet(String url) throws Exception {

		System.out.println("\nSending 'GET' request to URL : " + url);
		String USER_AGENT = "Mozilla/5.0";

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// print result
		return response.toString();

	}

	@SuppressWarnings({ "resource", "rawtypes", "unchecked" })
	public static void main(String[] args) throws IOException {
		InputStream is = new FileInputStream("settings.json");
		BufferedReader buf = new BufferedReader(new InputStreamReader(is));
		String line = buf.readLine(); 
		StringBuilder sb = new StringBuilder(); 
		while(line != null)
		{ 
			sb.append(line).append("\n"); 
			line = buf.readLine(); 
		}
		String settingsString = sb.toString();

		JSONObject settingsJSON = null;
		
		String user_id = null;
		String token = null;
		String miner = null;
		String worker_name = null;
		double fall_percentage = 95;
		
		try {
			settingsJSON = new JSONObject(settingsString);
			fall_percentage = Double.valueOf(settingsJSON.getString("fall_percentage"));
			worker_name 	= settingsJSON.getString("worker_name");
			user_id 		= settingsJSON.getString("user_id");
			token 			= settingsJSON.getString("token");
			miner 			= settingsJSON.getString("miner");
		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		String minerUrl = "https://api-etc.ethermine.org/miner/" + miner + "/workers";
		
		if (fall_percentage < 0) fall_percentage = 0;
		if (fall_percentage > 100) fall_percentage = 100;
		fall_percentage /= 100;

		
		Map<String, List> hashRates = new HashMap<String, List>();

		String method = "messages.send"; // to message
		String message;
		
		boolean launch = true, sended;
		// String method = "wall.post"; // wall post
		String request;
		JSONObject JSONrequest = null;
		double hashRate, hashRateMin;
		String worker;
		
		while (true) {
			message = "";
			if (launch)
				message = worker_name + "%20launched%0A";
			else
				message += "from%20" + worker_name + "%3A%0A";
			hashRateMin = 1000;
			sended = false;
			String res = "";
			while (!sended)
				try {
					res = sendGet(minerUrl);
					sended = true;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.err.println(minerUrl + " not sended!");
					try {
						TimeUnit.SECONDS.sleep(60);
					} catch (InterruptedException e1) {
					}
				}
			try {
				JSONrequest = new JSONObject(res);
				JSONArray workers = JSONrequest.getJSONArray("data");
				for (int i = 0; i < workers.length(); i++) {
					worker = workers.getJSONObject(i).getString("worker");
					message += worker + ": ";
					hashRate = workers.getJSONObject(i).getDouble("reportedHashrate") / 1000000;
					hashRateMin = (hashRateMin < hashRate) ? hashRateMin : hashRate;
					message += hashRate + "Mh/s";
					if (launch)
					{
						List<Double> tmp = new ArrayList<Double>();
						for (int j = 0; j < 10; j ++)
							tmp.add(hashRate);
						Collections.sort(tmp);
						Collections.reverse(tmp);
						hashRates.put(worker, tmp);
					}
					else if (fall_percentage != 0)
					{
						List<Double> tmp = hashRates.get(worker);
						if (hashRate < tmp.get(5)*fall_percentage)
						{
							hashRateMin = 0;
							message += "%20WARNING";
						}
						tmp.remove(0);
						tmp.add(hashRate);
						Collections.sort(tmp);
						Collections.reverse(tmp);
						hashRates.put(worker, tmp);
					}
					message += "%0A";
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			message = message.replace(" ", "%20");
			message = message.replace(":", "%3A");
			message = message.replace("/", "%2F");
			request = "https://api.vk.com/method/" + method + "?" + "user_id=" + user_id + "&message=" + message + "&access_token=" + token + "&v=5.73";// wall post
			sended = false;
			while (!sended)
				try {
					if ((hashRateMin == 0)||(launch)) {
						sendGet(request);
						if (launch)
						{
							launch = false;
							TimeUnit.SECONDS.sleep(60);
						}
						else
							TimeUnit.SECONDS.sleep(600);
					} else
						TimeUnit.SECONDS.sleep(60);
					sended = true;
				} catch (Exception e) {
					System.err.println(request + " not sended!");
					try {
						TimeUnit.SECONDS.sleep(60);
					} catch (InterruptedException e1) {
					}
				}
		}
	}
}
