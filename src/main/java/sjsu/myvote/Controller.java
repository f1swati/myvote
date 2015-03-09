package sjsu.myvote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@RestController
@RequestMapping("/api/v1")
public class Controller {

	private Map<Integer, Moderator> moderatorMap;
	private Map<String, Poll> pollMap;
	private final ObjectMapper objectMap = new ObjectMapper();
	private AtomicLong alongint;
	
	public Controller() {
		alongint = new AtomicLong(123456);
		moderatorMap = new HashMap<Integer, Moderator>();
		pollMap=new HashMap<String, Poll>();
		
	}

	@RequestMapping(value = "/moderators", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Moderator> createModerator( @Valid@RequestBody Moderator moderator) {

		moderator.setId((int) alongint.incrementAndGet());
		moderator.setCreated_at(Calendar.getInstance());
		moderatorMap.put(moderator.getId(), moderator);
		return new ResponseEntity<Moderator>(moderator,HttpStatus.CREATED);
	}

	@RequestMapping(value = "/moderators/{id}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Moderator> getModerator(@PathVariable int id) {
		if(moderatorMap.containsKey(id)){
			return new ResponseEntity<Moderator>(moderatorMap.get(id),HttpStatus.OK);
		}
		else return new ResponseEntity<Moderator>(HttpStatus.BAD_REQUEST);
	}
	
	@RequestMapping(value = "/moderators/{id}", method = RequestMethod.PUT, produces=MediaType.APPLICATION_JSON)
	@ResponseBody
	public ResponseEntity<Object> updateModerator( @PathVariable int id,@Validated(VoteViews.Moderator.class) @RequestBody Moderator moderator,Errors errors) {
		if(errors.hasErrors()) {return new ResponseEntity<Object>(errors.getAllErrors(), HttpStatus.BAD_REQUEST); }
		if(moderatorMap.containsKey(id)){
		Moderator moderatorActual=moderatorMap.get(id);
		moderatorActual.setEmail(moderator.getEmail());
		moderatorActual.setPassword(moderator.getPassword());
		return new ResponseEntity<Object>(moderatorMap.get(id),HttpStatus.CREATED) ;}
		else{
			 return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(value = "/moderators/{id}/polls", method = RequestMethod.POST, produces=MediaType.APPLICATION_JSON)
	@ResponseBody
	public ResponseEntity<Object> createPoll(@RequestBody Poll poll,@PathVariable int id) {
		
		if(moderatorMap.containsKey(id)){
			poll.setId(null);
			pollMap.put(poll.getId(), poll);
			List<String> pollIds= moderatorMap.get(id).getPollIds();
			pollIds.add(poll.getId());
			return new ResponseEntity<Object>(poll,HttpStatus.OK);
		}
		else{
			return new ResponseEntity<Object>("Moderator Not found",HttpStatus.BAD_REQUEST);
		}
		
	}
	
	@RequestMapping(value = "/polls/{id}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<String> getPoll(@NotNull @PathVariable String id,HttpServletResponse response) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectWriter objectWriter = objectMap.writerWithView(VoteViews.User.class);
		if(pollMap.containsKey(id)){
			return new ResponseEntity<String>(objectWriter.writeValueAsString(pollMap.get(id)),HttpStatus.OK);
		}
		else
			return new ResponseEntity<String>("Poll with given Poll id not found",HttpStatus.BAD_REQUEST);
	}
	
	@RequestMapping(value = "/moderators/{moderator_id}/polls/{pollId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Object> getModeratorPoll(@NotNull @PathVariable int moderator_id,@NotNull @PathVariable String pollId){
		if(moderatorMap.containsKey(moderator_id)){
			List<String> pollIdList=moderatorMap.get(moderator_id).getPollIds();
			if(pollIdList.contains(pollId)){
			return new ResponseEntity<Object>(pollMap.get(pollId),HttpStatus.OK);
			}
			return new ResponseEntity<Object>("Poll Id is not associated with moderator",HttpStatus.BAD_REQUEST);
		}
		else
			 return new ResponseEntity<Object>("Moderator Not Found",HttpStatus.BAD_REQUEST);
	}
	
	@RequestMapping(value = "/moderators/{moderator_id}/polls", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Object> getModeratorPolls(@NotNull @PathVariable int moderator_id){
		if(moderatorMap.containsKey(moderator_id)){
			List<String> pollIdList=moderatorMap.get(moderator_id).getPollIds();
			List<Poll> polls=new ArrayList<Poll>();
			for(String poolId:pollIdList){
				polls.add(pollMap.get(poolId));
			}
			return new ResponseEntity<Object>(polls,HttpStatus.OK);
		}
		else
			 return new ResponseEntity<Object>("Moderator Not Found",HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/moderators/{moderator_id}/polls/{pollId}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<Object> deletePoll(@NotNull @PathVariable int moderator_id,@NotNull @PathVariable String pollId){
		if(moderatorMap.containsKey(moderator_id)){
			moderatorMap.get(moderator_id).getPollIds().remove(pollId);
			pollMap.remove(pollId);
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		}
		else
			 return new ResponseEntity<Object>("Moderator Not Found",HttpStatus.BAD_REQUEST);
	}
	
	@RequestMapping(value = "/polls/{pollId}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<Object> castVote(@NotNull @PathVariable String pollId,@RequestParam int choice ){
		if(pollMap.containsKey(pollId)){
			Poll poll = pollMap.get(pollId);
			List<Integer> results = poll.getResults();
			if(choice<=results.size()){
			Integer count=results.get(choice);
			results.remove(choice);
			results.add(choice, ++count);
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			}
			return new ResponseEntity<Object>("Invalid Answer",HttpStatus.BAD_REQUEST);
		}
		else
			 return new ResponseEntity<Object>("Invalid Poll Id",HttpStatus.BAD_REQUEST);
	}

}
