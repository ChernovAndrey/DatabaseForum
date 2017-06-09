package master.rowmaps;

import master.objects.ObjPost;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by andrey on 14.03.17.
 */
public class postMapper implements RowMapper<ObjPost> {
    public ObjPost mapRow(ResultSet rs, int rowNum) throws SQLException {
        ObjPost post = new ObjPost();
        post.setId(rs.getInt("id"));
        post.setAuthor(rs.getString("author"));
        post.setCreated(rs.getString("created"));
        post.setForum(rs.getString("forum"));
        post.setEdited(rs.getBoolean("isEdited"));
        post.setMessage(rs.getString("message"));
        post.setParent(rs.getInt("parent"));
        post.setThread(rs.getInt("thread"));
        return post;
    }
}
