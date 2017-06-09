package master;

/**
 * Created by andrey on 05.03.17.
 */

import master.Controllers.*;
import master.objects.*;
import master.rowmaps.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class MainController {
    @Autowired
    private ForumController forumController;
    @Autowired
    private UserController userController;
    @Autowired
    private ThreadController threadController;
    @Autowired
    private PostController postController;
    @Autowired
    private VoteController voteController;
    @Autowired
    private ServiceContoller serviceContoller;

    private String getDateTime() {

        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+03:00");

        final java.util.Date date = new java.util.Date();

        return dateFormat.format(date);

    }

    public static boolean checkSlugOrId(String str) {
        Pattern p = Pattern.compile("^[a-z]+");
        Matcher m = p.matcher(str);
        return m.matches();
    }

    @RequestMapping(path = "/forum/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createForum(@RequestBody ObjForum body) {
        return forumController.createForum(body);
    }

    @RequestMapping(path = "/forum/{slug}/details", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getForum(@PathVariable String slug) {
        return forumController.getForum(slug);
    }


    @RequestMapping(path = "/user/{nickname}/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createUser(@RequestBody ObjUser body, @PathVariable String nickname) {
        return userController.createUser(body, nickname);
    }

    @RequestMapping(path = "/user/{nickname}/profile", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getUser(@PathVariable String nickname) {
        return userController.getUser(nickname);
    }

    @RequestMapping(path = "/user/{nickname}/profile", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> updateUser(@RequestBody ObjUser body, @PathVariable String nickname) {
        return  userController.updateUser(body,nickname);
    }


    //thread
    @RequestMapping(path = "/forum/{slug}/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createThread(@RequestBody ObjThread body, @PathVariable String slug) {
        return threadController.createThread(body,slug);
    }


    @RequestMapping(path = "/forum/{slug}/threads", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getThreads(@PathVariable String slug, @RequestParam(value = "limit", required = false) Integer limit,
                                             @RequestParam(value = "since", required = false) String since, @RequestParam(value = "desc", required = false) boolean desc) {
        return threadController.getThreads(slug, limit, since, desc);
    }
    @RequestMapping(path = "/thread/{slugOrId}/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createPosts(@RequestBody ArrayList<ObjPost> body, @PathVariable String slugOrId) {
        return postController.createPosts(body,slugOrId);
    }

    @RequestMapping(path = "thread/{slugOrId}/vote", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> vote(@RequestBody ObjVote body,@PathVariable String slugOrId) {
        return voteController.vote(body,slugOrId);
    }


    @RequestMapping(path = "/thread/{slugOrId}/details", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getThread(@PathVariable String slugOrId) {
     return threadController.getThread(slugOrId);
    }


    @RequestMapping(path = "/thread/{slugOrId}/posts", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getPosts(@PathVariable String slugOrId, @RequestParam(value = "limit", required = false) Integer limit,
                                           @RequestParam(value = "sort", required = false, defaultValue = "flat") String sort, @RequestParam(value = "desc", required = false) boolean desc,
                                           @RequestParam(value = "marker", required = false, defaultValue = "0") Integer marker) {

        return postController.getPosts(slugOrId,limit,sort,desc,marker);
    }


    @RequestMapping(path = "/thread/{SlugOrId}/details", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> updatethread(@RequestBody ObjThread body, @PathVariable String SlugOrId) {
        return threadController.updatethread(body,SlugOrId);
    }


    @RequestMapping(path = "/forum/{slug}/users", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getUsers(@PathVariable String slug, @RequestParam(value = "limit", required = false, defaultValue = "-1") Integer limit,
                                           @RequestParam(value = "since", required = false, defaultValue = "-1") String since, @RequestParam(value = "desc", required = false, defaultValue = "false") boolean desc) {
        return userController.getUsers(slug, limit, since, desc);
    }

    @RequestMapping(path = "/post/{id}/details", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getPosts(@PathVariable Integer id, @RequestParam(value = "related", required = false, defaultValue = "") String Request) {
       return  postController.getPosts(id,Request);
    }


    @RequestMapping(path = "/post/{id}/details", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> updatePost(@RequestBody ObjPost body, @PathVariable Integer id) {
        return postController.updatePost(body,id);
    }

    @RequestMapping(path = "/service/status", method = RequestMethod.GET)
    public ResponseEntity<String> getStatus() {
        return serviceContoller.getStatus();
    }


    @RequestMapping(path = "/service/clear", method = RequestMethod.POST)
    public ResponseEntity<String> clear() {
        return serviceContoller.clear();
    }


}
