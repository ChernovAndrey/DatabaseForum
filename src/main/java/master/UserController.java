package master;

/**
 * Created by andrey on 05.03.17.
 */

import jdk.nashorn.internal.parser.JSONParser;
import master.objects.ObjForum;
import master.objects.ObjPost;
import master.objects.ObjThread;
import master.rowmaps.forumMapper;
import master.rowmaps.threadMapper;
import master.rowmaps.userMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.postgresql.core.Oid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import master.objects.ObjUser;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import java.text.DateFormat;

import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@RestController
@RequestMapping("/api")
public class UserController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String getDateTime() {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+03:00");

        Date date = new Date();

        return dateFormat.format(date);

    }

    public static boolean checkSlugOrId(String str){
        Pattern p = Pattern.compile("^[a-z]+");
        Matcher m = p.matcher(str);
        return m.matches();
    }

    @RequestMapping(path = "/forum/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createForum(@RequestBody ObjForum body) {
        //  final JSONObject answer = new JSONObject();

        String SQLUsers = "select * from users where lower(nickname) = ?";
        try {
            ObjUser user = jdbcTemplate.queryForObject(SQLUsers,
                    new Object[]{body.getUser().toLowerCase()}, new userMapper());
            body.setUser(user.getNickname());
        } catch (Exception e2) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
        String SQLForum = "select * from forum where title = ? or slug= ? or \"user\" =? ";
        List<ObjForum> forum = jdbcTemplate.query(SQLForum,
                new Object[]{body.getTitle(), body.getSlug(), body.getUser()}, new forumMapper());
        if (!forum.isEmpty()) return new ResponseEntity<String>(forum.get(0).getJson().toString(), HttpStatus.CONFLICT);
        jdbcTemplate.update(
                "INSERT INTO forum (title,\"user\",slug,posts,threads) values(?,?,?,?,?)", body.getTitle(), body.getUser(), body.getSlug(), body.getPosts(), body.getThreads());
        return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.CREATED);
    }


    @RequestMapping(path = "/forum/{slug}/details", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getForum(@PathVariable String slug) {
        String SQLForum = "select * from forum where lower(slug) = ?";
        try {
            ObjForum forum = jdbcTemplate.queryForObject(SQLForum,
                    new Object[]{slug.toLowerCase()}, new forumMapper());
            return new ResponseEntity<String>(forum.getJson().toString(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }

    }


    @RequestMapping(path = "/user/{nickname}/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createUser(@RequestBody ObjUser body, @PathVariable String nickname) {
        //  final JSONObject answer = new JSONObject();
        body.setNickname(nickname);
        List<ObjUser> users = jdbcTemplate.query("select * from users where LOWER (email)=? or LOWER (nickname)=?", new Object[]{body.getEmail().toLowerCase(), body.getNickname().toLowerCase()}, new userMapper());
        if (users.isEmpty()) {
            jdbcTemplate.update("insert into users (nickname,fullname,about,email) values (?,?,?,?)", body.getNickname(), body.getFullname(), body.getAbout(), body.getEmail());
            return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.CREATED);
        }
        JSONArray result = new JSONArray();
        for (int i = 0; i < users.size(); i++) {
            result.put(users.get(i).getJson());
        }
        return new ResponseEntity<String>(result.toString(), HttpStatus.CONFLICT);
    }

    @RequestMapping(path = "/user/{nickname}/profile", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getUser(@PathVariable String nickname) {
        String SQLUser = "select * from users where lower(nickname) = ?";
        try {
            ObjUser user = jdbcTemplate.queryForObject(SQLUser,
                    new Object[]{nickname.toLowerCase()}, new userMapper());
            return new ResponseEntity<String>(user.getJson().toString(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }

    }

    @RequestMapping(path = "/user/{nickname}/profile", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> updateUser(@RequestBody ObjUser body, @PathVariable String nickname) {
        if (body.getJson().toString().equals("{}")) return getUser(nickname);
        ResponseEntity<String> resultGet = getUser(nickname);
        if (resultGet.getStatusCodeValue() == 404) return resultGet;
        //String oldUSer=resultGet.getBody();
        //JSONParser parser =new JSONParser();
        JSONObject oldUser = new JSONObject(resultGet.getBody());
        JSONObject newUser = body.getJson();
        if (newUser.has("email")) {
            List<ObjUser> users = jdbcTemplate.query("select * from users where LOWER (email)=? ", new Object[]{body.getEmail().toLowerCase()}, new userMapper());
            if (!users.isEmpty()) {
                return new ResponseEntity<String>("", HttpStatus.CONFLICT);
            }
        }


        if (!newUser.has("about")) body.setAbout(oldUser.get("about").toString());
        if (!newUser.has("email")) body.setEmail(oldUser.get("email").toString());
        if (!newUser.has("fullname")) body.setFullname(oldUser.get("fullname").toString());

        body.setNickname(nickname);
        jdbcTemplate.update("update users set (fullname,about,email)=(?,?,?) where lower(nickname)= ?", body.getFullname(), body.getAbout(), body.getEmail(), nickname.toLowerCase());
        return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.OK);
    }


    //thread
    @RequestMapping(path = "/forum/{slug}/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createThread(@RequestBody ObjThread body, @PathVariable String slug) {
        String SQLThread = "select * from thread where lower(title)=? or lower(slug)= ?";
        try {
            ObjThread thread = jdbcTemplate.queryForObject(SQLThread,
                    new Object[]{body.getTitle().toLowerCase(),body.getSlug().toLowerCase()}, new threadMapper());
            StringBuilder time=new StringBuilder(thread.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            thread.setCreated(time.toString());
            return new ResponseEntity<String>(thread.getJson().toString(), HttpStatus.CONFLICT);
        } catch (Exception e) {}
        String SQLForum = "select * from forum where lower(slug)=?";

        String SQLUser = "select * from users where lower(nickname)=?";

        try {
            ObjUser user = jdbcTemplate.queryForObject(SQLUser,
                    new Object[]{body.getAuthor().toLowerCase()}, new userMapper());
            ObjForum forum = jdbcTemplate.queryForObject(SQLForum,
                    new Object[]{body.getForum().toLowerCase()}, new forumMapper());
            body.setForum(forum.getSlug());
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
        final KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement("insert into thread (title,author,forum,message,slug," +
                        "votes,created) values (?,?,?,?,?,?,?::timestamptz)", new String[]{"id"});
                ps.setString(1, body.getTitle());
                ps.setString(2, body.getAuthor());
                ps.setString(3, body.getForum());
                ps.setString(4, body.getMessage());
                ps.setString(5, body.getSlug());
                ps.setInt(6, body.getVotes());
                ps.setString(7, body.getCreated());
                return ps;
            }
        }, holder);
        body.setId((int) holder.getKey());
        return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.CREATED);
    }


    @RequestMapping(path = "/forum/{slug}/threads", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getThreads(@PathVariable String slug, @RequestParam(value = "limit", required = false) Integer limit,
                                             @RequestParam(value = "since", required = false) String since, @RequestParam(value = "desc", required = false) boolean desc) {
        ResponseEntity<String> forum = getForum(slug);
        if (forum.getStatusCodeValue() == 404) return forum;
        StringBuilder SQLThreads = new StringBuilder("select * from thread where forum=?");
        if (since != null) {
            StringBuilder time = new StringBuilder(since);
            time.replace(10, 11, " ");
            since = time.toString();
            if (desc == true) SQLThreads.append(" and created <=?::timestamptz ");
            else SQLThreads.append(" and created >=?::timestamptz ");
        }
        SQLThreads.append(" order by created ");
        if (desc == true) SQLThreads.append(" desc ");
        List<ObjThread> threads;
        if (limit != null) {
            SQLThreads.append(" limit ?");
            if (since != null) {

                System.out.println(since);
                threads = jdbcTemplate.query(SQLThreads.toString(), new Object[]{slug, since, limit}, new threadMapper());
            } else threads = jdbcTemplate.query(SQLThreads.toString(), new Object[]{slug, limit}, new threadMapper());
        } else {

            if (since != null) {
                threads = jdbcTemplate.query(SQLThreads.toString(), new Object[]{slug, since}, new threadMapper());
            } else threads = jdbcTemplate.query(SQLThreads.toString(), new Object[]{slug}, new threadMapper());
        }
        JSONArray result = new JSONArray();
        for (int i = 0; i < threads.size(); i++) {
            StringBuilder time = new StringBuilder(threads.get(i).getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            threads.get(i).setCreated(time.toString());
            result.put(threads.get(i).getJson());
        }
        return new ResponseEntity<String>(result.toString(), HttpStatus.OK);
    }


    @RequestMapping(path = "/thread/{slugOrId}/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createPosts(@RequestBody ArrayList<ObjPost> body, @PathVariable String slugOrId) {
        System.out.println(body.size());
        String SQLThread = "select * from thread where lower(author) = ?";
        for (int i=0;i<body.size();i++) {
            ObjPost aBody1=body.get(i);
            ObjThread objthread;
            try {
                objthread = jdbcTemplate.queryForObject(SQLThread,
                        new Object[]{aBody1.getAuthor().toLowerCase()}, new threadMapper());
            } catch (Exception e) {
                return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
            }
            aBody1.setForum(objthread.getForum());
            aBody1.setThread(objthread.getId());
            StringBuilder time = new StringBuilder(getDateTime());
            time.replace(10, 11, "T");
            aBody1.setCreated(time.toString());
            /*if (checkSlugOrId(slugOrId)) {
                aBody1.setForum(slugOrId);
            } else aBody1.setId(Integer.parseInt(slugOrId));*/
            final KeyHolder holder = new GeneratedKeyHolder();
            try{
                aBody1.setId(Integer.parseInt(slugOrId));
                jdbcTemplate.update("insert into post (id,parent,author,message,isEdited,forum,thread,created) " +
                        "values (?,?,?,?,?,?,?,?::timestamptz)", aBody1.getId(),aBody1.getParent(), aBody1.getAuthor(),aBody1.getMessage(),aBody1.getEdited(),
                        aBody1.getForum(),aBody1.getThread(),aBody1.getCreated());
            }
            catch(Exception e) {
                jdbcTemplate.update(new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        PreparedStatement ps = connection.prepareStatement("insert into post (parent,author,message,isEdited,forum,thread,created) " +
                                "values (?,?,?,?,?,?,?::timestamptz)", new String[]{"id"});
                        ps.setInt(1, aBody1.getParent());
                        ps.setString(2, aBody1.getAuthor());
                        ps.setString(3, aBody1.getMessage());
                        ps.setBoolean(4, aBody1.getEdited());
                        ps.setString(5, aBody1.getForum());
                        ps.setInt(6, aBody1.getThread());
                        ps.setString(7, aBody1.getCreated());
                        return ps;
                    }
                }, holder);
                aBody1.setId((int) holder.getKey());
            }
        }

        JSONArray result = new JSONArray();
        for (ObjPost aBody2 : body) {
            result.put(aBody2.getJson());
        }
        return new ResponseEntity<String>(result.toString(), HttpStatus.CREATED);
    }
}