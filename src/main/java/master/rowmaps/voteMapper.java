package master.rowmaps;

import master.objects.ObjVote;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by andrey on 15.03.17.
 */
public class voteMapper implements RowMapper<ObjVote> {
    public ObjVote mapRow(ResultSet rs, int rowNum) throws SQLException {
        ObjVote vote = new ObjVote();
        vote.setId(rs.getInt("id"));
        vote.setNickname(rs.getString("nickname"));
        vote.setSlug(rs.getString("slug"));
        vote.setVoice(rs.getInt("voice"));
        return vote;
    }
}
