package master.rowmaps;

import master.objects.ObjThread;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by andrey on 11.03.17.
 */
public class threadMapper implements RowMapper<ObjThread>{
    public ObjThread mapRow(ResultSet rs, int rowNum) throws SQLException {
        ObjThread thread = new ObjThread();
        thread.setId(rs.getInt("id"));
        thread.setTitle(rs.getString("title"));
        thread.setAuthor(rs.getString("author"));
        thread.setForum(rs.getString("forum"));
        thread.setMessage(rs.getString("message"));
        if(rs.getInt("votes")!=0)thread.setVotes(rs.getInt("votes"));
        thread.setSlug(rs.getString("slug"));
        thread.setCreated(rs.getString("created"));
        return thread;
    }
}
