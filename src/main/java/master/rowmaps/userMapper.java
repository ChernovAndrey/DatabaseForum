package master.rowmaps;

import master.objects.ObjForum;
import master.objects.ObjUser;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by andrey on 07.03.17.
 */
public class userMapper implements RowMapper<ObjUser> {
    public ObjUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        ObjUser user = new ObjUser();
       // user.setId(rs.getInt("id"));
        user.setNickname(rs.getString("nickname"));
        user.setFullname(rs.getString("fullname"));
        user.setAbout(rs.getString("about"));
        user.setEmail(rs.getString("email"));
        return user;
    }
}

