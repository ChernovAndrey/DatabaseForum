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
    public  ResponseEntity<String> vote(ObjVote body,  String slugOrId) {
        ObjVote objvote;
        try {
            body.setId(Integer.parseInt(slugOrId));
        } catch (Exception e) {
           try {
               final Integer id = jdbcTemplate.queryForObject("select id from thread where lower(slug)=?", new Object[]{slugOrId.toLowerCase()}, Integer.class);
               body.setId(id);
           }catch (Exception e1) {
               return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
           }
        }
        try {
            jdbcTemplate.queryForObject("select * from users where lower(nickname)=?", new Object[]{body.getNickname().toLowerCase()}, new userMapper());
        } catch (Exception e) {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
        final ObjThread result;
           final String SQLVote = "Select * from vote where (id,lower(nickname))=(?,?)";
            final List<ObjVote> lVote = jdbcTemplate.query(SQLVote,
                    new Object[]{body.getId(), body.getNickname().toLowerCase()}, new voteMapper());
            if (lVote.isEmpty()) {
                jdbcTemplate.update("insert into vote (id, nickname,voice) values(?,?,?)", body.getId(), body.getNickname(), body.getVoice());

                if (body.getVoice() == 1)  jdbcTemplate.update("update thread set votes=votes+1 where id=?", new Object[]{body.getId()});
                if (body.getVoice() == -1) jdbcTemplate.update("update thread set votes=votes-1 where id=?", new Object[]{body.getId()});
                try {
                    result = jdbcTemplate.queryForObject("select * from thread where id =?", new Object[]{body.getId()}, new threadMapper());
                } catch (Exception e) {
                    return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
                }
            } else {
                result = jdbcTemplate.queryForObject("select * from thread where id =?", new Object[]{body.getId()}, new threadMapper());
                jdbcTemplate.update("update vote set voice=? where (id,lower(nickname))=(?,?)", body.getVoice(), body.getId(),body.getNickname().toLowerCase());
                if ((body.getVoice() == -1) && (lVote.get(0).getVoice() == 1)) {
                    jdbcTemplate.update("update thread set votes=votes-2 where id=?", new Object[]{body.getId()});
                    result.updateVotes(-2);
                }
                if ((body.getVoice() == -1) && (lVote.get(0).getVoice() == 0)) {
                    jdbcTemplate.update("update thread set votes=votes-1 where id=?", new Object[]{body.getId()});
                    result.updateVotes(-1);
                }
                if ((body.getVoice() == 1) && (lVote.get(0).getVoice() == 0)) {
                    jdbcTemplate.update("update thread set votes=votes+1 where id=?", new Object[]{body.getId()});
                    result.updateVotes(1);
                }
                if ((body.getVoice() == 1) && (lVote.get(0).getVoice() == -1)){
                    jdbcTemplate.update("update thread set votes=votes+2 where id=?", new Object[]{body.getId()});
                    result.updateVotes(2);
                }
            }
            final StringBuilder time = new StringBuilder(result.getCreated());
            time.replace(10, 11, "T");
            time.append(":00");
            result.setCreated(time.toString());
            return new ResponseEntity<String>(result.getJson().toString(), HttpStatus.OK);
    }

}
