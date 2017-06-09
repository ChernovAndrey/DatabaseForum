package master.Controllers;

/**
 * Created by andrey on 05.03.17.
 */

import master.objects.ObjUser;
import master.rowmaps.userMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.List;

@Service
public class UserController {

    private JdbcTemplate jdbcTemplate;
    UserController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Autowired
    ForumController forumController;

    public ResponseEntity<String> createUser(ObjUser body, String nickname) {
        body.setNickname(nickname);
        final List<ObjUser> users = jdbcTemplate.query("select * from users where LOWER (email)=? or LOWER (nickname)=?", new Object[]{body.getEmail().toLowerCase(), body.getNickname().toLowerCase()}, new userMapper());
        if (users.isEmpty()) {
            jdbcTemplate.update("insert into users (nickname,fullname,about,email) values (?,?,?,?)", body.getNickname(), body.getFullname(), body.getAbout(), body.getEmail());
            return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.CREATED);
        }
        final JSONArray result = new JSONArray();
        for (ObjUser user : users) {
            result.put(user.getJson());
        }
        return new ResponseEntity<String>(result.toString(), HttpStatus.CONFLICT);
    }

    public ResponseEntity<String> getUser(String nickname) {
        final String SQLUser = "select * from users where lower(nickname) = ?";
        try {
            final ObjUser user = jdbcTemplate.queryForObject(SQLUser,
                    new Object[]{nickname.toLowerCase()}, new userMapper());
            return new ResponseEntity<String>(user.getJson().toString(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<String> updateUser(ObjUser body, String nickname) {
        if (body.getJson().toString().equals("{}")) return getUser(nickname);
        final ResponseEntity<String> resultGet = getUser(nickname);
        if (resultGet.getStatusCodeValue() == 404) return resultGet;
        final JSONObject oldUser = new JSONObject(resultGet.getBody());
        final JSONObject newUser = body.getJson();
        if (newUser.has("email")) {
            final List<ObjUser> users = jdbcTemplate.query("select * from users where LOWER (email)=? ", new Object[]{body.getEmail().toLowerCase()}, new userMapper());
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


    public ResponseEntity<String> getUsers(String slug, Integer limit,String since, boolean desc) {
        final ResponseEntity<String> forum = forumController.getForum(slug);
        if (forum.getStatusCodeValue() == 404) return forum;
        StringBuilder SQL = new StringBuilder("select *" +
                "from users where nickname in " +
                "(select nickname from users u full join thread t on lower(u.nickname)=lower(t.author)" +
                "full join post p on lower(u.nickname)=lower(p.author) where lower(t.forum)=lower(?) or " +
                "lower(p.forum)=lower(?)  group by u.nickname order by nickname ) ");
        if (!since.equals("-1")) {
            if (desc == false) SQL.append(" and nickname > \'").append(since).append("\' ");
            else SQL.append(" and nickname < \'").append(since).append("\' ");
        }
        SQL.append(" order by nickname ");
        if (desc == true) SQL.append(" desc ");
        if (limit != -1) SQL.append("Limit " + limit.toString());
        final List<ObjUser> users = jdbcTemplate.query(SQL.toString(), new Object[]{slug.toLowerCase(), slug.toLowerCase()}, new userMapper());
        final JSONArray result = new JSONArray();
        for (ObjUser user : users) {
            result.put(user.getJson());
        }
        return new ResponseEntity<String>(result.toString(), HttpStatus.OK);
    }
}