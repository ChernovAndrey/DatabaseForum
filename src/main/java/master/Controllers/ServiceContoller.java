package master.Controllers;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.sql.DataSource;

/**
 * Created by andrey on 26.05.17.
 */
@Service
public class ServiceContoller {

    private JdbcTemplate jdbcTemplate;
    ServiceContoller(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    public ResponseEntity<String> getStatus() {
      //  final String SQLUser = "select * from users where lower(nickname) = lower(?)";
        final JSONObject result = new JSONObject();
        result.put("user", jdbcTemplate.queryForObject("select Count(*) from users", Integer.class));
        result.put("forum", jdbcTemplate.queryForObject("select Count(*) from forum", Integer.class));
        result.put("thread", jdbcTemplate.queryForObject("select Count(*) from thread", Integer.class));
        result.put("post", jdbcTemplate.queryForObject("select Count(*) from post", Integer.class));

        return new ResponseEntity<String>(result.toString(), HttpStatus.OK);
    }


    public ResponseEntity<String> clear() {
        jdbcTemplate.update("TRUNCATE post; TRUNCATE forum CASCADE; TRUNCATE users CASCADE; TRUNCATE thread CASCADE; TRUNCATE vote; TRUNCATE ForumUser;");
        return new ResponseEntity<String>("", HttpStatus.OK);
    }

}
