package master;

/**
 * Created by andrey on 05.03.17.
 */

import jdk.nashorn.internal.parser.JSONParser;
import master.objects.ObjForum;
import master.objects.ObjThread;
import master.rowmaps.forumMapper;
import master.rowmaps.threadMapper;
import master.rowmaps.userMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import master.objects.ObjUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RequestMapping(path = "/user", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public String loginUser(@RequestBody ObjUser body) {
        final JSONObject answer = new JSONObject();
        final StringBuilder sql = new StringBuilder("SELECT id, description, completed FROM tasks");
        final List<Object> args = new ArrayList<>();
        jdbcTemplate.update(
                "INSERT INTO Users (nickname,fullname) values(?,?)", "nick", "full");
        return "OK";
    }

    @RequestMapping(path = "/forum/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createForum(@RequestBody ObjForum body) {
      //  final JSONObject answer = new JSONObject();

        String SQLUsers = "select * from users where lower(nickname) = ?";
        try {
            ObjUser user = jdbcTemplate.queryForObject(SQLUsers,
                    new Object[]{body.getUser().toLowerCase()}, new userMapper());
            body.setUser(user.getNickname());
        }
        catch(Exception e2){
            return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);
        }
        String SQLForum="select * from forum where title = ? or slug= ? or \"user\" =? ";
            List<ObjForum> forum = jdbcTemplate.query(SQLForum,
                    new Object[]{body.getTitle(),body.getSlug(),body.getUser()}, new forumMapper());
            if (!forum.isEmpty()) return new ResponseEntity<String>(forum.get(0).getJson().toString(), HttpStatus.CONFLICT);
            jdbcTemplate.update(
                    "INSERT INTO forum (title,\"user\",slug,posts,threads) values(?,?,?,?,?)", body.getTitle(), body.getUser(), body.getSlug(), body.getPosts(), body.getThreads());
            return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.CREATED);
    }


    @RequestMapping(path = "/forum/{slug}/details", method = RequestMethod.GET,produces = "application/json")
    public ResponseEntity<String> getForum(@PathVariable String slug) {
        String SQLForum="select * from forum where lower(slug) = ?";
        try {
            ObjForum forum = jdbcTemplate.queryForObject(SQLForum,
                    new Object[]{slug.toLowerCase()}, new forumMapper());
            return new ResponseEntity<String>(forum.getJson().toString(), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }

    }


    @RequestMapping(path = "/user/{nickname}/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createUser(@RequestBody ObjUser body, @PathVariable String nickname) {
        //  final JSONObject answer = new JSONObject();
            body.setNickname(nickname);
            List<ObjUser> users = jdbcTemplate.query("select * from users where LOWER (email)=? or LOWER (nickname)=?", new Object[]{body.getEmail().toLowerCase(), body.getNickname().toLowerCase()}, new userMapper());
            if(users.isEmpty()) {
                jdbcTemplate.update("insert into users (nickname,fullname,about,email) values (?,?,?,?)",body.getNickname(),body.getFullname(),body.getAbout(),body.getEmail());
                return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.CREATED);
            }
        JSONArray result = new JSONArray();
        for (int i = 0; i < users.size(); i++) {
            result.put(users.get(i).getJson());
        }
            return new ResponseEntity<String>(result.toString(), HttpStatus.CONFLICT);
    }

    @RequestMapping(path = "/user/{nickname}/profile", method = RequestMethod.GET,produces = "application/json")
    public ResponseEntity<String> getUser(@PathVariable String nickname) {
        String SQLUser="select * from users where lower(nickname) = ?";
        try {
            ObjUser user = jdbcTemplate.queryForObject(SQLUser,
                    new Object[]{nickname.toLowerCase()}, new userMapper());
            return new ResponseEntity<String>(user.getJson().toString(), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }

    }

    @RequestMapping(path = "/user/{nickname}/profile", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> updateUser(@RequestBody ObjUser body, @PathVariable String nickname) {
        if(body.getJson().toString().equals("{}")) return getUser(nickname);
        ResponseEntity<String> resultGet = getUser(nickname);
        if(resultGet.getStatusCodeValue()==404) return resultGet;
        //String oldUSer=resultGet.getBody();
        //JSONParser parser =new JSONParser();
        JSONObject oldUser= new JSONObject(resultGet.getBody());
        JSONObject newUser= body.getJson();
        if(newUser.has("email")) {
            List<ObjUser> users = jdbcTemplate.query("select * from users where LOWER (email)=? ", new Object[]{body.getEmail().toLowerCase()}, new userMapper());
            if (!users.isEmpty()) {
                return new ResponseEntity<String>("", HttpStatus.CONFLICT);
            }
        }


        if(!newUser.has("about")) body.setAbout(oldUser.get("about").toString());
        if(!newUser.has("email")) body.setEmail(oldUser.get("email").toString());
        if(!newUser.has("fullname")) body.setFullname(oldUser.get("fullname").toString());

        body.setNickname(nickname);
        jdbcTemplate.update("update users set (fullname,about,email)=(?,?,?) where lower(nickname)= ?",body.getFullname(),body.getAbout(),body.getEmail(),nickname.toLowerCase());
        return new ResponseEntity<String>(body.getJson().toString(),HttpStatus.OK);
    }



    //thread
    @RequestMapping(path = "/forum/{slug}/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createThread(@RequestBody ObjThread body, @PathVariable String slug) {

        String SQLThread = "select * from thread where lower(slug)= ?";
        try {
            ObjThread thread = jdbcTemplate.queryForObject(SQLThread,
                    new Object[]{slug.toLowerCase()}, new threadMapper());
            return new ResponseEntity<String>(thread.getJson().toString(), HttpStatus.CONFLICT);
        } catch (Exception e) {
        }
        String SQLForum = "select * from forum where lower(slug)=?";

        String SQLUser = "select * from users where lower(nickname)=?";
        try {
            ObjUser user = jdbcTemplate.queryForObject(SQLUser,
                    new Object[]{body.getAuthor().toLowerCase()}, new userMapper());
            ObjForum thread = jdbcTemplate.queryForObject(SQLForum,
                    new Object[]{body.getForum().toLowerCase()}, new forumMapper());
            System.out.println(body.getId());
            return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.CREATED);
        }
        catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
    }

}
