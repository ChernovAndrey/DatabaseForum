package master.Controllers;

import master.objects.ObjForum;
import master.objects.ObjThread;
import master.objects.ObjUser;
import master.rowmaps.forumMapper;
import master.rowmaps.threadMapper;
import master.rowmaps.userMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by andrey on 18.05.17.
 */
@Service
public class ThreadController {
    private JdbcTemplate jdbcTemplate;
    ThreadController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    @Autowired
    private ForumController forumController;
    public ResponseEntity<String> createThread(ObjThread body, String slug) {
        try {
            final String SQLThread = "select * from thread where  lower(slug)= ?";
            final ObjThread thread = jdbcTemplate.queryForObject(SQLThread,
                    new Object[]{body.getSlug().toLowerCase()}, new threadMapper());
            final StringBuilder time = new StringBuilder(thread.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            thread.setCreated(time.toString());
            return new ResponseEntity<String>(thread.getJson().toString(), HttpStatus.CONFLICT);
        } catch (Exception ignored) {}
        try {
            final String SQLUser = "select * from users where lower(nickname)=?";
            final ObjUser user = jdbcTemplate.queryForObject(SQLUser,
                    new Object[]{body.getAuthor().toLowerCase()}, new userMapper());
            final String SQLForum = "select * from forum where lower(slug)=?";
            final ObjForum forum = jdbcTemplate.queryForObject(SQLForum,
                    new Object[]{body.getForum().toLowerCase()}, new forumMapper());
            // forum.setSlug(body.getForum());
            final KeyHolder holder = new GeneratedKeyHolder();
            body.setForum(forum.getSlug());
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    final PreparedStatement ps = connection.prepareStatement("insert into thread (title,author,forum,message,slug," +
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
            jdbcTemplate.update("update forum set threads=threads+1 where lower(slug)=?", slug.toLowerCase());
            return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.CREATED);
        }
        catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
    }
    public ResponseEntity<String> getThreads(String slug, Integer limit,
                                             String since, boolean desc) {
        final ResponseEntity<String> forum = forumController.getForum(slug);
        if (forum.getStatusCodeValue() == 404) return forum;
        final StringBuilder SQLThreads = new StringBuilder("select * from thread where lower(forum)=?");
        if (since != null) {
            final StringBuilder time = new StringBuilder(since);
            time.replace(10, 11, " ");
            since = time.toString();
            if (desc) SQLThreads.append(" and created <=?::timestamptz ");
            else SQLThreads.append(" and created >=?::timestamptz ");
        }
        SQLThreads.append(" order by created ");
        if (desc) SQLThreads.append(" desc ");
        final List<ObjThread> threads;
        if (limit != null) {
            SQLThreads.append(" limit ?");
            if (since != null) {

                threads = jdbcTemplate.query(SQLThreads.toString(), new Object[]{slug.toLowerCase(), since, limit}, new threadMapper());
            } else threads = jdbcTemplate.query(SQLThreads.toString(), new Object[]{slug.toLowerCase(), limit}, new threadMapper());
        } else {

            if (since != null) {
                threads = jdbcTemplate.query(SQLThreads.toString(), new Object[]{slug.toLowerCase(), since}, new threadMapper());
            } else threads = jdbcTemplate.query(SQLThreads.toString(), new Object[]{slug.toLowerCase()}, new threadMapper());
        }
        final JSONArray result = new JSONArray();
        for (ObjThread thread : threads) {
            final StringBuilder time = new StringBuilder(thread.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            thread.setCreated(time.toString());
            result.put(thread.getJson());
        }
        return new ResponseEntity<String>(result.toString(), HttpStatus.OK);
    }

    public ResponseEntity<String> getThread( String slugOrId) {
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
            if (flag) {
                final ObjThread result = jdbcTemplate.queryForObject("select * from thread where lower(slug)=?",
                        new Object[]{slug.toLowerCase()}, new threadMapper());
                final StringBuilder time = new StringBuilder(result.getCreated());
                time.replace(10, 11, "T");
                time.append(":00");
                result.setCreated(time.toString());
                return new ResponseEntity<String>(result.getJson().toString(), HttpStatus.OK);
            }
            final ObjThread result = jdbcTemplate.queryForObject("select * from thread where id=?",
                    new Object[]{id}, new threadMapper());
            final StringBuilder time = new StringBuilder(result.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            result.setCreated(time.toString());
            return new ResponseEntity<String>(result.getJson().toString(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }

    }
    public ResponseEntity<String> updatethread(ObjThread body, String SlugOrId) {
        if (body.getJson().toString().equals("{}")) return getThread(SlugOrId);
        final ResponseEntity<String> resultGet = getThread(SlugOrId);
        if (resultGet.getStatusCodeValue() == 404) return resultGet;
        final JSONObject oldThread = new JSONObject(resultGet.getBody());
        final JSONObject newThread = body.getJson();
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
        final StringBuilder time = new StringBuilder(body.getCreated());
        time.replace(10, 11, "T");
        body.setCreated(time.toString());
        return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.OK);
    }



}