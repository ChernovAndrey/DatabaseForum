package master.Controllers;

import master.objects.ObjForum;
import master.objects.ObjPost;
import master.objects.ObjThread;
import master.objects.ObjUser;
import master.rowmaps.forumMapper;
import master.rowmaps.postMapper;
import master.rowmaps.threadMapper;
import master.rowmaps.userMapper;
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
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrey on 18.05.17.
 */
@Service
public class PostController {
    private JdbcTemplate jdbcTemplate;
    PostController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public ResponseEntity<String> createPosts( ArrayList<ObjPost> body, String slugOrId) {
        final String SQLThread = "select * from thread where lower(slug) = ?";
        String slug = null;
        Integer idThread = 0;
        try {
            final Integer id = Integer.parseInt(slugOrId);
            final List<ObjThread> thrList = jdbcTemplate.query("select * from thread where id=?", new Object[]{id}, new threadMapper());
            if (thrList.isEmpty()) return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
            final ObjThread thr1 = thrList.get(0);
            slug = thr1.getSlug();
            idThread = id;
        } catch (DataAccessException e) {
            return new ResponseEntity<String>("", HttpStatus.CONFLICT);
        } catch (Exception e) {
            slug = slugOrId;
            final List<ObjThread> thrList;
            thrList = jdbcTemplate.query("select * from thread where lower(slug)=?", new Object[]{slug.toLowerCase()}, new threadMapper());
            if (thrList.isEmpty()) return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
            ObjThread thr1 = thrList.get(0);
            idThread = thr1.getId();
        }
        try {
            ObjThread thr = jdbcTemplate.queryForObject("select * from thread where lower(slug)=?", new Object[]{slug.toLowerCase()}, new threadMapper());
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
        //if(thr==null) return new ResponseEntity<String>("",HttpStatus.NOT_FOUND);

        ObjThread objthread = null;
        Integer countPosts = 0;
        final StringBuilder DateCreated = new StringBuilder();
        for (int i = 0; i < body.size(); i++) {
            final ObjPost aBody1 = body.get(i);
            try {
                objthread = jdbcTemplate.queryForObject(SQLThread,
                        new Object[]{slug.toLowerCase()}, new threadMapper());
            } catch (Exception e) {
                return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
            }
            try {
                ObjUser Author = jdbcTemplate.queryForObject("select * from users where lower(nickname)=?", new Object[]{aBody1.getAuthor().toLowerCase()}, new userMapper());
            } catch (Exception e) {
                return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
            }
            if (body.get(i).getParent() != 0) {
                try {
                    Integer parentPost = (Integer) jdbcTemplate.queryForObject("select id from post where id=? and thread=?", new Object[]{body.get(i).getParent(), idThread}, Integer.class);
                } catch (Exception e) {
                    return new ResponseEntity<String>("", HttpStatus.CONFLICT);
                }
            }
            aBody1.setForum(objthread.getForum());
            aBody1.setThread(objthread.getId());
            final KeyHolder holder = new GeneratedKeyHolder();

            try {
                aBody1.setThread(Integer.parseInt(slugOrId));//id
            } catch (Exception e) {
                boolean flag = true;//slug
            }
            if (i == 0) {
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
            } else {
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
                        ps.setString(7, DateCreated.toString());
                        return ps;
                    }
                }, holder);
                countPosts++;
                aBody1.setId((int) holder.getKey());
            }
            aBody1.setCreated(DateCreated.toString());

        }

        final JSONArray result = new JSONArray();
        for (ObjPost aBody2 : body) {
            final StringBuilder created = new StringBuilder(aBody2.getCreated());
            created.replace(10, 11, "T");
            created.append("+03:00");
            aBody2.setCreated(created.toString());
            result.put(aBody2.getJson());
        }
        jdbcTemplate.update("update forum set posts=posts+? where slug=?", countPosts, objthread.getForum());
        return new ResponseEntity<String>(result.toString(), HttpStatus.CREATED);
    }

    public ResponseEntity<String> getPosts(String slugOrId, Integer limit,
                                           String sort,  boolean desc,
                                           Integer marker) {

        final StringBuilder SQL = new StringBuilder("select * from post where ");
        Integer idThread = 0;
        try {
            idThread = Integer.parseInt(slugOrId);
            final List<ObjThread> thrList = jdbcTemplate.query("select * from thread where id=?", new Object[]{idThread}, new threadMapper());
            if (thrList.isEmpty()) return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
            SQL.append(" thread=");
            SQL.append(slugOrId).append(" ");
        } catch (Exception e) {
            final String slugThread = slugOrId;
            final boolean flag = true;//id
            SQL.append(" thread=");
            final ObjThread thread;
            try {
                thread = jdbcTemplate.queryForObject("select * from thread where lower(slug)=?", new Object[]{slugOrId.toLowerCase()}, new threadMapper());
            } catch (Exception e1) {
                return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
            }
            idThread = thread.getId();
            SQL.append("\'").append(idThread).append("\' ");
        }
        List<ObjPost> posts = null;
        final Integer SumLimAndMarker = limit + marker;
        if (sort.equals("tree")) {
            final StringBuilder SQLTree = new StringBuilder("with recursive r (id,parent,author,message,isEdited,forum,thread,created,posts) AS(" +
                    "select id,parent,author,message,isEdited,forum,thread,created,array[id] from post where parent=0 " +
                    " Union all " +
                    " select p.id,p.parent,p.author,p.message,p.isEdited,p.forum,p.thread,p.created, array_append(posts, p.id) from post p " +
                    " Join r on r.id=p.parent) select r.id, r.parent,r.author,r.message,r.isEdited,r.forum,r.thread,r.created from r where thread=? order by posts ");
            if (desc) SQLTree.append(" desc ");
            SQLTree.append("limit " + SumLimAndMarker.toString());
            posts = jdbcTemplate.query(SQLTree.toString(), new Object[]{idThread}, new postMapper());

        }
        Integer parentMarker = 0;
        if (sort.equals("parent_tree")) {
            final StringBuilder SQLParent = new StringBuilder("select * from post p Join thread t on (t.id=p.thread) where t.id=? and p.parent=0 order by p.id ");
            if (desc) SQLParent.append(" desc ");
            SQLParent.append("limit ").append(SumLimAndMarker.toString());
            final List<ObjPost> parents = jdbcTemplate.query(SQLParent.toString(),
                    new Object[]{idThread}, new postMapper());

            final StringBuilder SQLParentTree = new StringBuilder("with recursive r (id,parent,author,message,isEdited,forum,thread,created,posts) AS(\n" +
                    "select id,parent,author,message,isEdited,forum,thread,created,array[id] from post where id=? " +
                    " Union all " +
                    " select p.id,p.parent,p.author,p.message,p.isEdited,p.forum,p.thread,p.created, array_append(posts, p.id) from post p " +
                    " Join r on r.id=p.parent) select r.id, r.parent,r.author,r.message,r.isEdited,r.forum,r.thread,r.created from r where thread=? order by posts ");
            if (desc) SQLParentTree.append(" desc ");
            //SQLParentTree.append("limit " + SumLimAndMarker.toString());
            boolean flagPostsAdd = false;
            for (int i = parentMarker; i < parents.size(); i++) {
                final int IdParent = parents.get(i).getId();
                if (!flagPostsAdd) {
                    posts = jdbcTemplate.query(SQLParentTree.toString(), new Object[]{IdParent, idThread}, new postMapper());
                    flagPostsAdd = true;
                    parentMarker += posts.size();
                } else {
                    List<ObjPost> intermediateResult = jdbcTemplate.query(SQLParentTree.toString(), new Object[]{IdParent, idThread}, new postMapper());
                    if (intermediateResult != null) {
                        parentMarker += intermediateResult.size();
                        posts.addAll(intermediateResult);
                    }
                }
            }
        }
        if (sort.equals("flat")) {
            SQL.append(" Order by created ");
            if (desc) SQL.append(" desc ");
            SQL.append(" , id ");
            if (desc) SQL.append(" desc ");
            SQL.append("limit ").append(SumLimAndMarker.toString());
            posts = jdbcTemplate.query(SQL.toString(), new postMapper());
        }
        final JSONObject result = new JSONObject();
        if (sort.equals("parent_tree")) result.put("marker", parentMarker.toString());
        else {
            if (marker > posts.size()) result.put("marker", marker.toString());
            else result.put("marker", SumLimAndMarker.toString());
        }
        final JSONArray resPost = new JSONArray();
        for (int i = marker; i < posts.size(); i++) {
            final ObjPost apost = posts.get(i);
            final StringBuilder time = new StringBuilder(apost.getCreated());
            time.replace(10, 11, "T");
            time.replace(time.length() - 3, time.length(), "+03");
            time.append(":00");
            apost.setCreated(time.toString());
            resPost.put(apost.getJson());
        }
        result.put("posts", resPost);
        return new ResponseEntity<String>(result.toString(), HttpStatus.OK);
    }

    public ResponseEntity<String> getPosts(Integer id, String Request) {
        final JSONObject result = new JSONObject();
        ObjPost post = null;
        try {
            post = jdbcTemplate.queryForObject("select * from post where id=?",
                    new Object[]{id}, new postMapper());
            //StringBuilder time = new StringBuilder(post.getCreated());
            //Timestamp created;
            final Timestamp created = jdbcTemplate.queryForObject("select created from post where id=?", new Object[]{id}, Timestamp.class);
            final SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS+03:00");
            final String time = ft.format(created);

            post.setCreated(time);
            result.put("post", post.getJson());
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }

        final String[] array = Request.split(",");
        for(String anArray : array) {
            if (anArray.equals("user")) {
                final ObjUser user = jdbcTemplate.queryForObject("select * from users where lower(nickname)=? ", new Object[]{post.getAuthor().toLowerCase()}, new userMapper());
                result.put("author", user.getJson());
            }
            if (anArray.equals("thread")) {
                final ObjThread thread = jdbcTemplate.queryForObject("select * from thread where id=? ", new Object[]{post.getThread()}, new threadMapper());
                final StringBuilder time = new StringBuilder(thread.getCreated());
                time.replace(10, 11, "T");
                time.append(":00");
                thread.setCreated(time.toString());
                result.put("thread", thread.getJson());
            }
            if (anArray.equals("forum")) {
                final ObjForum forum = jdbcTemplate.queryForObject("select * from forum where lower(slug)=? ", new Object[]{post.getForum().toLowerCase()}, new forumMapper());
                result.put("forum", forum.getJson());
            }
        }
        return new ResponseEntity<String>(result.toString(), HttpStatus.OK);

    }
    public ResponseEntity<String> updatePost(ObjPost body, Integer id) {
        if (body.getJson().toString().equals("{}")) return getPosts(id, "");
        final ResponseEntity<String> resultGet = getPosts(id, "");
        if (resultGet.getStatusCodeValue() == 404) return resultGet;
        final JSONObject oldfullInf = new JSONObject(resultGet.getBody());
        final JSONObject newPost = body.getJson();
        final JSONObject oldPost = new JSONObject(oldfullInf.get("post").toString());
        if (!newPost.has("message")) {
            body.setMessage(oldPost.get("message").toString());
            if (oldPost.get("isEdited").equals(false)) body.setEdited(false);
            else body.setEdited(true);
        } else {
            if (!oldPost.get("message").toString().equals(newPost.get("message").toString()))
                body.setEdited(true);
        }
        body.setCreated(oldPost.get("created").toString());
        body.setId(Integer.parseInt(oldPost.get("id").toString()));
        body.setForum(oldPost.get("forum").toString());
        body.setThread(Integer.parseInt(oldPost.get("thread").toString()));
        body.setAuthor(oldPost.get("author").toString());
        body.setParent(Integer.parseInt(oldPost.get("parent").toString()));
        jdbcTemplate.update("update post set (id,author,forum,thread,isEdited,message,parent)=(?,?,?,?,?,?,?) where id= ?", body.getId(),
                body.getAuthor(), body.getForum(), body.getThread(), body.getEdited(), body.getMessage(), body.getParent(), body.getId());
        final Timestamp created = jdbcTemplate.queryForObject("select created from post where id=?", new Object[]{id}, Timestamp.class);
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS+03:00");
        final String time = ft.format(created);
        body.setCreated(time);
        return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.OK);
    }


}
