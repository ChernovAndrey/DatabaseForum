package master.objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * Created by andrey on 11.03.17.
 */
public class ObjThread {
    private int id;
    private String title;
    private String author;
    private String forum;
    private  String message;
    private String slug;
    private int votes;
    private String  created;
    public ObjThread() {

    }

    @JsonCreator
    public ObjThread(
            @JsonProperty("id") int id,
            @JsonProperty("title") String title,
            @JsonProperty("author") String author,
            @JsonProperty("slug") String slug,
            @JsonProperty("message") String message,
            @JsonProperty("forum") String forum,
            @JsonProperty("votes") int votes,
            @JsonProperty("created") String created) {
        this.id = id;
        this.title=title;
        this.author = author;
        this.forum=forum;
        this.message=message;
        this.slug = slug;
        this.votes = votes;
        this.created=created;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getForum() {return forum;}

    public  String getMessage(){return message;}

    public String getSlug() {
        return slug;
    }

    public int getVotes() {
        return votes;
    }
    public String getCreated() {return created;}

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public  void setForum(String forum){this.forum=forum;}

    public  void setMessage(String message){this.message=message;}

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }
    public void setCreated(String created) {
        this.created=created;
    }


    public JSONObject getJson() {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("author", author);
        jsonObject.put("created", created);
        jsonObject.put("forum",forum);
        jsonObject.put("id", id);
        jsonObject.put("message",message);
        jsonObject.put("slug", slug);
        jsonObject.put("title",title);
        if(votes!=0)jsonObject.put("votes",votes);

        return jsonObject;
    }
}
