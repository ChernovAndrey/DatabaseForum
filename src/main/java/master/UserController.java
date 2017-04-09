package master;

/**
 * Created by andrey on 05.03.17.
 */

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jdk.nashorn.internal.parser.JSONParser;
import master.objects.*;
import master.rowmaps.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.postgresql.core.Oid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
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
import java.sql.Date;
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

        java.util.Date date = new java.util.Date();

        return dateFormat.format(date);

    }

    public static boolean checkSlugOrId(String str) {
        Pattern p = Pattern.compile("^[a-z]+");
        Matcher m = p.matcher(str);
        return m.matches();
    }

    @RequestMapping(path = "/forum/create", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> createForum(@RequestBody ObjForum body) {


        String SQLUsers = "select * from users where lower(nickname) = ?";
        try {
            ObjUser user = jdbcTemplate.queryForObject(SQLUsers,
                    new Object[]{body.getUser().toLowerCase()}, new userMapper());
            body.setUser(user.getNickname());
        } catch (Exception e2) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
        String SQLForum = "select * from forum where slug= ? or \"user\" =? ";
        List<ObjForum> forum = jdbcTemplate.query(SQLForum,
                new Object[]{ body.getSlug(), body.getUser()}, new forumMapper());
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
        String SQLThread = "select * from thread where  lower(slug)= ?";
        try {
            ObjThread thread = jdbcTemplate.queryForObject(SQLThread,
                    new Object[]{body.getSlug().toLowerCase()}, new threadMapper());
            StringBuilder time = new StringBuilder(thread.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            thread.setCreated(time.toString());
            return new ResponseEntity<String>(thread.getJson().toString(), HttpStatus.CONFLICT);
        } catch (Exception e) {
        }
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
        jdbcTemplate.update("update forum set threads=threads+1 where slug=?", slug);
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
        String SQLThread = "select * from thread where lower(slug) = ?";
        String slug = null;
        Integer idThread=0;
        try {
            Integer id = Integer.parseInt(slugOrId);
            List<ObjThread> thrList = jdbcTemplate.query("select * from thread where id=?", new Object[]{id}, new threadMapper());
            if(thrList.isEmpty()) return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);
            ObjThread thr1=thrList.get(0);
            slug = thr1.getSlug();
            idThread=id;
        } catch (DataAccessException e) {
            return new ResponseEntity<String>("",HttpStatus.CONFLICT);
        } catch(Exception e) {
            slug = slugOrId;
            List<ObjThread> thrList;
            thrList = jdbcTemplate.query("select * from thread where lower(slug)=?", new Object[]{slug.toLowerCase()}, new threadMapper());
            if(thrList.isEmpty()) return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);
            ObjThread thr1=thrList.get(0);
            idThread = thr1.getId();
        }
            try {
                ObjThread thr = jdbcTemplate.queryForObject("select * from thread where lower(slug)=?", new Object[]{slug.toLowerCase()}, new threadMapper());
            }
            catch(Exception e){
                return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
            }
            //if(thr==null) return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);

        ObjThread objthread = null;
        Integer countPosts = 0;
        final StringBuilder DateCreated = new StringBuilder();
        for (int i = 0; i < body.size(); i++) {
            ObjPost aBody1 = body.get(i);
            try {
                objthread = jdbcTemplate.queryForObject(SQLThread,
                        new Object[]{slug.toLowerCase()}, new threadMapper());
            } catch (Exception e) {
                return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
            }
            try{
                ObjUser Author=jdbcTemplate.queryForObject("select * from users where lower(nickname)=?",new Object[]{aBody1.getAuthor().toLowerCase()},new userMapper());
            }
            catch(Exception e){
                return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);
            }
            if(body.get(i).getParent()!=0) {
                Integer parentPost=0;
                try{
                    parentPost = (Integer)jdbcTemplate.queryForObject("select id from post where id=? and thread=?", new Object[]{body.get(i).getParent(),idThread}, Integer.class);
                }
                catch(Exception e){
                    return new ResponseEntity<String>("",HttpStatus.CONFLICT);
                }
                }
            aBody1.setForum(objthread.getForum());
            aBody1.setThread(objthread.getId());
            final KeyHolder holder = new GeneratedKeyHolder();
            boolean flag = false;

            try {
                aBody1.setThread(Integer.parseInt(slugOrId));//id
            } catch (Exception e) {
                flag = true;//slug
            }
            if(i==0) {
                jdbcTemplate.update(new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        PreparedStatement ps = connection.prepareStatement("insert into post (parent,author,message,isEdited,forum,thread) " +
                                "values (?,?,?,?,?,?)", new String[]{"id", "created"});
                        ps.setInt(1, aBody1.getParent());
                        ps.setString(2, aBody1.getAuthor());
                        ps.setString(3, aBody1.getMessage());
                        ps.setBoolean(4, aBody1.getEdited());
                        ps.setString(5, aBody1.getForum());
                        ps.setInt(6, aBody1.getThread());
                        return ps;
                    }
                }, holder);
                countPosts++;
                aBody1.setId((int) holder.getKeys().get("id"));
                DateCreated.append((holder.getKeys().get("created").toString()));
            }
            else{
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
                        ps.setString(7,DateCreated.toString());
                        return ps;
                    }
                }, holder);
                countPosts++;
                aBody1.setId((int) holder.getKey());
            }
            aBody1.setCreated(DateCreated.toString());

        }

        JSONArray result = new JSONArray();
        for (ObjPost aBody2 : body) {
            StringBuilder created=new StringBuilder(aBody2.getCreated());
            created.replace(10, 11, "T");
            created.append("+03:00");
            aBody2.setCreated(created.toString());
            result.put(aBody2.getJson());
        }
        jdbcTemplate.update("update forum set posts=posts+? where slug=?", countPosts, objthread.getForum());
        return new ResponseEntity<String>(result.toString(), HttpStatus.CREATED);
    }

    @RequestMapping(path = "thread/{slugOrId}/vote", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> vote(@RequestBody ObjVote body, @PathVariable String slugOrId) {
        ObjVote objvote;
        boolean flag = false;//id
        try {
            body.setId(Integer.parseInt(slugOrId));
        } catch (Exception e) {
            flag = true;//slug
        }
        try{
            jdbcTemplate.queryForObject("select * from users where nickname=?",new Object[]{body.getNickname()},new userMapper());
        }
        catch(Exception e){
            return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);
        }
        ObjThread result;
        if (flag == false) {
            String SQLVote = "Select * from vote where (id,lower(nickname))=(?,?)";
            List<ObjVote> lVote = jdbcTemplate.query(SQLVote,
                    new Object[]{body.getId(), body.getNickname().toLowerCase()}, new voteMapper());
            if (lVote.isEmpty()) {
                jdbcTemplate.update("insert into vote (id,nickname,voice) values(?,?,?)", body.getId(), body.getNickname(), body.getVoice());

                if (body.getVoice() == 1)
                    jdbcTemplate.update("update thread set votes=votes+1 where id=?", new Object[]{body.getId()});
                else jdbcTemplate.update("update thread set votes=votes-1 where id=?", new Object[]{body.getId()});
                try {
                    result = jdbcTemplate.queryForObject("select * from thread where id =?", new Object[]{body.getId()}, new threadMapper());
                }
                catch(Exception e){
                    return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);
                }

                jdbcTemplate.update("update vote set slug=? where id=?", result.getSlug(), body.getId());
            } else {
                jdbcTemplate.update("update vote set voice=? where id=?", body.getVoice(), body.getId());
                if ((body.getVoice() == -1) && (lVote.get(0).getVoice() == 1))
                    jdbcTemplate.update("update thread set votes=votes-2 where id=?", new Object[]{body.getId()});

                if ((body.getVoice() == 1) && (lVote.get(0).getVoice() == -1))
                    jdbcTemplate.update("update thread set votes=votes+2 where id=?", new Object[]{body.getId()});

                result = jdbcTemplate.queryForObject("select * from thread where id =?", new Object[]{body.getId()}, new threadMapper());
            }
            StringBuilder time = new StringBuilder(result.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            result.setCreated(time.toString());
            return new ResponseEntity<String>(result.getJson().toString(), HttpStatus.OK);

        } else {
            body.setSlug(slugOrId);
            String SQLVote = "Select * from vote where (slug,lower(nickname))=(?,?)";
            List<ObjVote> lVote = jdbcTemplate.query(SQLVote,
                    new Object[]{body.getSlug(), body.getNickname().toLowerCase()}, new voteMapper());
            if (lVote.isEmpty()) {
                jdbcTemplate.update("insert into vote (slug,nickname,voice) values(?,?,?)", body.getSlug(), body.getNickname(), body.getVoice());
                if (body.getVoice() == 1)
                    jdbcTemplate.update("update thread set votes=votes+1 where lower(slug)=?", slugOrId.toLowerCase());
                else jdbcTemplate.update("update thread set votes=votes-1 where lower(slug)=?", slugOrId.toLowerCase());

                try{
                    result = jdbcTemplate.queryForObject("select * from thread where lower(slug) =?", new Object[]{body.getSlug().toLowerCase()}, new threadMapper());
                }
                catch(Exception e){
                    return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);
                }
                jdbcTemplate.update("update vote set id=? where lower(slug)=?", result.getId(), result.getSlug().toLowerCase());
            } else {
                jdbcTemplate.update("update vote set voice=? where lower(slug)=?", body.getVoice(), body.getSlug().toLowerCase());
                if ((body.getVoice() == 1) && (lVote.get(0).getVoice() == 0))
                    jdbcTemplate.update("update thread set votes=votes+1 where lower(slug)=?", slugOrId.toLowerCase());
                if ((body.getVoice() == -1) && (lVote.get(0).getVoice() == 1))
                    jdbcTemplate.update("update thread set votes=votes-2 where lower(slug)=?", slugOrId.toLowerCase());
                if ((body.getVoice() == 1) && (lVote.get(0).getVoice() == -1))
                    jdbcTemplate.update("update thread set votes=votes+2 where lower(slug)=?", slugOrId.toLowerCase());
                if ((body.getVoice() == -1) && (lVote.get(0).getVoice() == 0))
                    jdbcTemplate.update("update thread set votes=votes-1 where lower(slug)=?", slugOrId.toLowerCase());
                result = jdbcTemplate.queryForObject("select * from thread where slug =?", new Object[]{body.getSlug()}, new threadMapper());
            }
            StringBuilder time = new StringBuilder(result.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            result.setCreated(time.toString());
            return new ResponseEntity<String>(result.getJson().toString(), HttpStatus.OK);
        }
    }


    @RequestMapping(path = "/thread/{slugOrId}/details", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getThread(@PathVariable String slugOrId) {
        boolean flag = false;//id
        int id = 0;
        String slug = "";
        try {
            id = Integer.parseInt(slugOrId);
        } catch (Exception e) {
            flag = true;
            slug = slugOrId;
        }
        try {
            if (flag == true) {
                ObjThread result = jdbcTemplate.queryForObject("select * from thread where lower(slug)=?",
                        new Object[]{slug.toLowerCase()}, new threadMapper());
                StringBuilder time = new StringBuilder(result.getCreated());
                time.replace(10, 11, "T");
                time.append(":00");
                result.setCreated(time.toString());
                return new ResponseEntity<String>(result.getJson().toString(), HttpStatus.OK);
            }
            ObjThread result = jdbcTemplate.queryForObject("select * from thread where id=?",
                    new Object[]{id}, new threadMapper());
            StringBuilder time = new StringBuilder(result.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            result.setCreated(time.toString());
            return new ResponseEntity<String>(result.getJson().toString(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }

    }


    @RequestMapping(path = "/thread/{slugOrId}/posts", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getPosts(@PathVariable String slugOrId, @RequestParam(value = "limit", required = false) Integer limit,
                                           @RequestParam(value = "sort", required = false, defaultValue ="flat") String sort, @RequestParam(value = "desc", required = false) boolean desc,
                                           @RequestParam(value = "marker", required = false, defaultValue = "0") Integer marker) {

        StringBuilder SQL = new StringBuilder("select * from post where ");
        Integer idThread = 0;
        boolean flag = false;//id
        String slugThread = "";
        try {
            idThread = Integer.parseInt(slugOrId);
            List<ObjThread> thrList=jdbcTemplate.query("select * from thread where id=?",new Object[]{idThread},new threadMapper());
            if (thrList.isEmpty()) return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
            SQL.append(" thread=");
            SQL.append(slugOrId + " ");
        } catch (Exception e) {
            slugThread = slugOrId;
            flag = true;
            SQL.append(" thread=");
            ObjThread thread;
            try {
                thread = jdbcTemplate.queryForObject("select * from thread where lower(slug)=?", new Object[]{slugOrId.toLowerCase()}, new threadMapper());
            }catch (Exception e1){
                return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);
            }
            idThread = thread.getId();
            SQL.append("\'" + idThread + "\' ");
        }
        List<ObjPost> posts = null;
        Integer SumLimAndMarker = limit + marker;
        if (sort.equals("tree")) {
            StringBuilder SQLTree = new StringBuilder("with recursive r (id,parent,author,message,isEdited,forum,thread,created,posts) AS(" +
                    "select id,parent,author,message,isEdited,forum,thread,created,array[id] from post where parent=0 " +
                    " Union all " +
                    " select p.id,p.parent,p.author,p.message,p.isEdited,p.forum,p.thread,p.created, array_append(posts, p.id) from post p " +
                    " Join r on r.id=p.parent) select r.id, r.parent,r.author,r.message,r.isEdited,r.forum,r.thread,r.created from r where thread=? order by posts ");
            if (desc == true) SQLTree.append(" desc ");
            SQLTree.append("limit " + SumLimAndMarker.toString());
            posts = jdbcTemplate.query(SQLTree.toString(), new Object[]{idThread}, new postMapper());

        }
        Integer parentMarker = 0;
        if (sort.equals("parent_tree")) {
            StringBuilder SQLParent = new StringBuilder("select * from post p Join thread t on (t.id=p.thread) where t.id=? and p.parent=0 order by p.id ");
            if (desc == true) SQLParent.append(" desc ");
            SQLParent.append("limit " + SumLimAndMarker.toString());
            List<ObjPost> parents = jdbcTemplate.query(SQLParent.toString(),
                    new Object[]{idThread}, new postMapper());

            StringBuilder SQLParentTree = new StringBuilder("with recursive r (id,parent,author,message,isEdited,forum,thread,created,posts) AS(\n" +
                    "select id,parent,author,message,isEdited,forum,thread,created,array[id] from post where id=? " +
                    " Union all " +
                    " select p.id,p.parent,p.author,p.message,p.isEdited,p.forum,p.thread,p.created, array_append(posts, p.id) from post p " +
                    " Join r on r.id=p.parent) select r.id, r.parent,r.author,r.message,r.isEdited,r.forum,r.thread,r.created from r where thread=? order by posts ");
            if (desc == true) SQLParentTree.append(" desc ");
            //SQLParentTree.append("limit " + SumLimAndMarker.toString());
            boolean flagPostsAdd = false;
            for (int i = parentMarker; i < parents.size(); i++) {
                int IdParent = parents.get(i).getId();
                if (flagPostsAdd == false) {
                    posts = jdbcTemplate.query(SQLParentTree.toString(), new Object[]{IdParent, idThread}, new postMapper());
                    flagPostsAdd = true;
                    parentMarker+=posts.size();
                } else {
                    List<ObjPost> intermediateResult = null;
                    intermediateResult = jdbcTemplate.query(SQLParentTree.toString(), new Object[]{IdParent, idThread}, new postMapper());
                    if (intermediateResult != null) {
                        parentMarker+=intermediateResult.size();
                        posts.addAll(intermediateResult);
                    }
                }
            }
        }
        if (sort.equals("flat")) {
            SQL.append(" Order by created ");
            if (desc == true) SQL.append(" desc ");
            SQL.append(" , id ");
            if (desc == true) SQL.append(" desc ");
            SQL.append("limit " + SumLimAndMarker.toString());
            posts = jdbcTemplate.query(SQL.toString(), new postMapper());
        }
        JSONObject result = new JSONObject();
        if (sort.equals("parent_tree")) result.put("marker", parentMarker.toString());
        else {
            if (marker > posts.size()) result.put("marker", marker.toString());
            else result.put("marker", SumLimAndMarker.toString());
        }
        JSONArray resPost = new JSONArray();
        for (int i = marker; i < posts.size(); i++) {
            ObjPost apost = posts.get(i);
            StringBuilder time = new StringBuilder(apost.getCreated());
            time.replace(10, 11, "T");
            time.replace(time.length()-3,time.length(),"+03");
            time.append(":00");
            apost.setCreated(time.toString());
            resPost.put(apost.getJson());
        }
        result.put("posts", resPost);
        return new ResponseEntity<String>(result.toString(), HttpStatus.OK);
    }


    @RequestMapping(path = "/thread/{SlugOrId}/details", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> updatethread(@RequestBody ObjThread body, @PathVariable String SlugOrId) {
        if (body.getJson().toString().equals("{}")) return getThread(SlugOrId);
        ResponseEntity<String> resultGet = getThread(SlugOrId);
        if (resultGet.getStatusCodeValue() == 404) return resultGet;
        JSONObject oldThread = new JSONObject(resultGet.getBody());
        JSONObject newThread = body.getJson();
       /* if (newThread.has("title")) {
            List<ObjThread> thread = jdbcTemplate.query("select * from thread where LOWER (title)=? ", new Object[]{body.getTitle().toLowerCase()}, new threadMapper());
            if (!thread.isEmpty()) {
                return new ResponseEntity<String>("", HttpStatus.CONFLICT);
            }
        }*/

        if (!newThread.has("title")) body.setTitle(oldThread.get("title").toString());
        if (!newThread.has("message")) body.setMessage(oldThread.get("message").toString());
        body.setSlug(oldThread.get("slug").toString());
        body.setId(Integer.parseInt(oldThread.get("id").toString()));
        if (oldThread.has("votes")) body.setVotes(Integer.parseInt(oldThread.get("votes").toString()));
        else body.setVotes(0);
        body.setAuthor(oldThread.get("author").toString());
        body.setCreated(oldThread.get("created").toString());
        body.setForum(oldThread.get("forum").toString());
        jdbcTemplate.update("update thread set (id,author,created,forum,slug,title,message,votes)=(?,?,?::timestamptz,?,?,?,?,?) where lower(slug)= ?", body.getId(),
                body.getAuthor(), body.getCreated(), body.getForum(), body.getSlug(), body.getTitle(), body.getMessage(), body.getVotes(), body.getSlug().toLowerCase());
        StringBuilder time = new StringBuilder(body.getCreated());
        time.replace(10, 11, "T");
        body.setCreated(time.toString());
        return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.OK);
    }


    @RequestMapping(path = "/forum/{slug}/users", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getUsers(@PathVariable String slug, @RequestParam(value = "limit", required = false, defaultValue = "-1") Integer limit,
                                           @RequestParam(value = "since", required = false, defaultValue = "-1") String since, @RequestParam(value = "desc", required = false, defaultValue = "false") boolean desc) {
        ResponseEntity<String> forum = getForum(slug);
        if (forum.getStatusCodeValue() == 404) return forum;
        StringBuilder SQL= new StringBuilder("select *" +
                "from users where nickname in " +
                "(select nickname from users u full join thread t on lower(u.nickname)=lower(t.author)" +
                "full join post p on lower(u.nickname)=lower(p.author) where lower(t.forum)=lower(?) or " +
                "lower(p.forum)=lower(?)  group by u.nickname order by nickname ) ");
        if (!since.equals("-1")) {
          if(desc==false)  SQL.append(" and nickname > \'"+since+"\' ");
          else     SQL.append(" and nickname < \'"+since+"\' ");
        }
        SQL.append(" order by nickname ");
        if (desc==true) SQL.append(" desc ");
        if (limit!=-1) SQL.append("Limit "+limit.toString());
        List<ObjUser> users = jdbcTemplate.query(SQL.toString(),new Object[]{slug.toLowerCase(), slug.toLowerCase()}, new userMapper());
        JSONArray result = new JSONArray();
        for (int i = 0; i < users.size(); i++) {
            result.put(users.get(i).getJson());
        }
        return new ResponseEntity<String>(result.toString(), HttpStatus.OK);
    }

    @RequestMapping(path = "/post/{id}/details", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getPosts(@PathVariable Integer id, @RequestParam(value = "related", required = false, defaultValue = "") String Request) {
        JSONObject result=new JSONObject();
        ObjPost post=null;
        try {
             post = jdbcTemplate.queryForObject("select * from post where id=?",
                    new Object[]{id}, new postMapper());
            //StringBuilder time = new StringBuilder(post.getCreated());
            //Timestamp created;
                Timestamp created = jdbcTemplate.queryForObject("select created from post where id=?", new Object[]{id}, Timestamp.class);
                SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS+03:00");
                String time= ft.format(created);

            post.setCreated(time);
            result.put("post",post.getJson());
        }
        catch(Exception e){
            return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);
        }

       String[] array= Request.split(",");
        for(int i=0;i<array.length;i++) {
            if (array[i].equals("user")) {
                ObjUser user = jdbcTemplate.queryForObject("select * from users where lower(nickname)=? ", new Object[]{post.getAuthor().toLowerCase()}, new userMapper());
                result.put("author", user.getJson());
            }
            if (array[i].equals("thread")) {
                ObjThread thread = jdbcTemplate.queryForObject("select * from thread where id=? ", new Object[]{post.getThread()}, new threadMapper());
                StringBuilder time = new StringBuilder(thread.getCreated());
                time.replace(10, 11, "T");
                time.append(":00");
                thread.setCreated(time.toString());
                result.put("thread", thread.getJson());
            }
            if (array[i].equals("forum")) {
                ObjForum forum = jdbcTemplate.queryForObject("select * from forum where lower(slug)=? ", new Object[]{post.getForum().toLowerCase()}, new forumMapper());
                result.put("forum", forum.getJson());
            }
        }
        return new ResponseEntity<String>(result.toString(), HttpStatus.OK);

    }



    @RequestMapping(path = "/post/{id}/details", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> updatePost(@RequestBody ObjPost body, @PathVariable Integer id) {
        if (body.getJson().toString().equals("{}")) return getPosts(id,"");
        ResponseEntity<String> resultGet = getPosts(id,"");
        if (resultGet.getStatusCodeValue() == 404) return resultGet;
        JSONObject oldfullInf = new JSONObject(resultGet.getBody());
        JSONObject newPost = body.getJson();
        JSONObject oldPost= new JSONObject(oldfullInf.get("post").toString());
        if (!newPost.has("message")) {
            body.setMessage(oldPost.get("message").toString());
            if(oldPost.get("isEdited").equals(false)) body.setEdited(false);
            else body.setEdited(true);
        }
        else {
            if(!oldPost.get("message").toString().equals(newPost.get("message").toString()))
            body.setEdited(true);
        }
        body.setCreated(oldPost.get("created").toString());
        body.setId(Integer.parseInt(oldPost.get("id").toString()));
        body.setForum(oldPost.get("forum").toString());
        body.setThread(Integer.parseInt(oldPost.get("thread").toString()));
        body.setAuthor(oldPost.get("author").toString());
        body.setParent(Integer.parseInt(oldPost.get("parent").toString()));
        jdbcTemplate.update("update post set (id,author,forum,thread,isEdited,message,parent)=(?,?,?,?,?,?,?) where id= ?", body.getId(),
                body.getAuthor(), body.getForum(), body.getThread(), body.getEdited(), body.getMessage(), body.getParent(),body.getId());
        Timestamp created = jdbcTemplate.queryForObject("select created from post where id=?", new Object[]{id}, Timestamp.class);
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS+03:00");
        String time= ft.format(created);
        body.setCreated(time);
        return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.OK);
    }

    @RequestMapping(path = "/service/status", method = RequestMethod.GET)
    public ResponseEntity<String> getStatus() {
        String SQLUser = "select * from users where lower(nickname) = ?";
            JSONObject result =new JSONObject();
            result.put("user" ,jdbcTemplate.queryForObject("select Count(*) from users",Integer.class));
            result.put("forum" ,jdbcTemplate.queryForObject("select Count(*) from forum",Integer.class));
            result.put("thread",jdbcTemplate.queryForObject("select Count(*) from thread",Integer.class));
            result.put("post",jdbcTemplate.queryForObject("select Count(*) from post",Integer.class));

            return new ResponseEntity<String>(result.toString(), HttpStatus.OK);
        }






    @RequestMapping(path = "/service/clear", method = RequestMethod.POST)
    public ResponseEntity<String> clear() {
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from forum");
        jdbcTemplate.update("delete from thread");
        jdbcTemplate.update("delete from vote");
        jdbcTemplate.update("delete from post");
        return new ResponseEntity<String>("",HttpStatus.OK);
    }

}
