package master.Controllers;

import master.objects.ObjForum;
import master.objects.ObjUser;
import master.rowmaps.forumMapper;
import master.rowmaps.userMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.List;

/**
 * Created by andrey on 18.05.17.
 */
@Service
public class ForumController {
    private JdbcTemplate jdbcTemplate;

    ForumController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public ResponseEntity<String> createForum(ObjForum body) {
        final String SQLUsers = "select * from users where lower(nickname) = ?";
        try {
            final ObjUser user = jdbcTemplate.queryForObject(SQLUsers,
                    new Object[]{body.getUser().toLowerCase()}, new userMapper());
            body.setUser(user.getNickname());
        } catch (Exception e2) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
        final String SQLForum = "select * from forum where slug= ? or \"user\" =? ";
        final List<ObjForum> forum = jdbcTemplate.query(SQLForum,
                new Object[]{body.getSlug(), body.getUser()}, new forumMapper());
        if (!forum.isEmpty()) return new ResponseEntity<String>(forum.get(0).getJson().toString(), HttpStatus.CONFLICT);
        jdbcTemplate.update(
                "INSERT INTO forum (title,\"user\",slug,posts,threads) values(?,?,?,?,?)", body.getTitle(), body.getUser(), body.getSlug(), body.getPosts(), body.getThreads());
        return new ResponseEntity<String>(body.getJson().toString(), HttpStatus.CREATED);
    }

    public ResponseEntity<String> getForum(String slug) {
        final String SQLForum = "select * from forum where lower(slug) = ?";
        try {
            final ObjForum forum = jdbcTemplate.queryForObject(SQLForum,
                    new Object[]{slug.toLowerCase()}, new forumMapper());
            return new ResponseEntity<String>(forum.getJson().toString(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
    }
}
