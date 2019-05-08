package delphix.oa.runner;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import delphix.oa.model.BartResponse;
import delphix.oa.model.EstInfo;
import delphix.oa.model.ResultRecord;
import delphix.oa.model.TrainInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BartInfoRunner implements ApplicationRunner {

	@Value("${bart.est.url}")
	private String bartEstUrl;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Override
	public void run(ApplicationArguments args) {
		
		try {
			// get trains info with REST API from bart.gov
			BartResponse br = restTemplate.getForObject(bartEstUrl, BartResponse.class);
			if (br == null || br.getRoot() == null || br.getRoot().getStation() == null || br.getRoot().getStation().size() == 0) {
				log.info("Got empty result from bart.gov");
				return; 
			}
			
			// build the result list
			List<ResultRecord> result = buildResultList(br);
			
			// print out first 10 records
			printResult(result, br.getRoot().getTime());
			
		} catch (RestClientException ex) {
			log.error("Could not get BART info from bart.gov");
			log.error(ex.getMessage());
		}
	}

	private List<ResultRecord> buildResultList(BartResponse br) {
		List<ResultRecord> result = new ArrayList<>();
		for (EstInfo ei : br.getRoot().getStation().get(0).getEtd()) {
			for (TrainInfo ti : ei.getEstimate()) {
				// 'leaving' = 0
				int minsLeft = isInt(ti.getMinutes()) ? Integer.valueOf(ti.getMinutes()) : 0;
				ResultRecord rr = new ResultRecord(minsLeft, ei.getDestination());
				result.add(rr);
			}
		}
		result.sort((a, b) -> a.getMinsLeft() - b.getMinsLeft());
		return result;
	}

	private void printResult(List<ResultRecord> result, String curtTime) {
		System.out.println("--------------------------------------------------");
		System.out.println("Montgomery St. " + curtTime);
		System.out.println("--------------------------------------------------");
		for (int i = 0; i < 10 && i < result.size(); i++) {
			System.out.println(result.get(i).getMinsLeft() + " min " + result.get(i).getDest());
		}
	}

	private static boolean isInt(String strNum) {
	    try {
	        Integer.valueOf(strNum);
	    } catch (NumberFormatException | NullPointerException nfe) {
	        return false;
	    }
	    return true;
	}
}
