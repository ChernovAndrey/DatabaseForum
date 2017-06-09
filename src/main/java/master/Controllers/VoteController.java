package master.Controllers;

import master.objects.ObjThread;
import master.objects.ObjVote;
import master.rowmaps.threadMapper;
import master.rowmaps.userMapper;
import master.rowmaps.voteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.sql.DataSource;
import java.util.List;

/**
 * Created by andrey on 18.05.17.
 */
@Service
public class VoteController {

    private JdbcTemplate jdbcTemplate;
    VoteController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    public ResponseEntity<String> vote(ObjVote body,  String slugOrId) {
        ObjVote objvote;
        boolean flag = false;//id
        try {
            body.setId(Integer.parseInt(slugOrId));
        } catch (Exception e) {
            flag = true;//slug
        }
        try {
            jdbcTemplate.queryForObject("select * from users where lower(nickname)=?", new Object[]{body.getNickname().toLowerCase()}, new userMapper());
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
        final ObjThread result;
        if (!flag) {
            final String SQLVote = "Select * from vote where (id,lower(nickname))=(?,?)";
            final List<ObjVote> lVote = jdbcTemplate.query(SQLVote,
                    new Object[]{body.getId(), body.getNickname().toLowerCase()}, new voteMapper());
            if (lVote.isEmpty()) {
                jdbcTemplate.update("insert into vote (id, nickname,voice) values(?,?,?)", body.getId(), body.getNickname(), body.getVoice());

                if (body.getVoice() == 1)
                    jdbcTemplate.update("update thread set votes=votes+1 where id=?", new Object[]{body.getId()});
                else jdbcTemplate.update("update thread set votes=votes-1 where id=?", new Object[]{body.getId()});
                try {
                    result = jdbcTemplate.queryForObject("select * from thread where id =?", new Object[]{body.getId()}, new threadMapper());
                } catch (Exception e) {
                    return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
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
            final StringBuilder time = new StringBuilder(result.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            result.setCreated(time.toString());
            return new ResponseEntity<String>(result.getJson().toString(), HttpStatus.OK);

        } else {
            body.setSlug(slugOrId);
            final String SQLVote = "Select * from vote where (slug,lower(nickname))=(?,?)";
            final List<ObjVote> lVote = jdbcTemplate.query(SQLVote,
                    new Object[]{body.getSlug(), body.getNickname().toLowerCase()}, new voteMapper());
            if (lVote.isEmpty()) {
                jdbcTemplate.update("insert into vote (slug,nickname,voice) values(?,?,?)", body.getSlug(), body.getNickname(), body.getVoice());
                if (body.getVoice() == 1)
                    jdbcTemplate.update("update thread set votes=votes+1 where lower(slug)=?", slugOrId.toLowerCase());
                else jdbcTemplate.update("update thread set votes=votes-1 where lower(slug)=?", slugOrId.toLowerCase());

                try {
                    result = jdbcTemplate.queryForObject("select * from thread where lower(slug) =?", new Object[]{body.getSlug().toLowerCase()}, new threadMapper());
                } catch (Exception e) {
                    return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
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
                result = jdbcTemplate.queryForObject("select * from thread where lower(slug) =?", new Object[]{body.getSlug().toLowerCase()}, new threadMapper());
            }
            final StringBuilder time = new StringBuilder(result.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            result.setCreated(time.toString());
            return new ResponseEntity<String>(result.getJson().toString(), HttpStatus.OK);
        }
    }

}
