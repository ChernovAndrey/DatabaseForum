package master.rowmaps;

import master.objects.ObjForum;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by andrey on 07.03.17.
 */
public class forumMapper implements RowMapper<ObjForum> {
    public ObjForum mapRow(ResultSet rs, int rowNum) throws SQLException {
        ObjForum forum = new ObjForum();
        forum.setId(rs.getInt("id"));
        forum.setTitle(rs.getString("title"));
        forum.setUser(rs.getString("user"));
        forum.setSlug(rs.getString("slug"));
        forum.setPosts(rs.getInt("posts"));
        forum.setThreads(rs.getInt("threads"));
        return forum;
    }
}
