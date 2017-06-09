package master.objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * Created by andrey on 06.03.17.
 */
public class ObjForum {
    private int id;
    private String title;
    private String user;
    private String slug;
    private int posts;
    private int threads;

    public ObjForum() {

    }

    @JsonCreator
    public ObjForum(
            @JsonProperty("id") int id,
            @JsonProperty("title") String title,
            @JsonProperty("user") String user,
            @JsonProperty("slug") String slug,
            @JsonProperty("threads") int threads,
            @JsonProperty("posts") int posts) {
        this.id = id;
        this.title = title;
        this.user = user;
        this.slug = slug;
        this.threads = threads;
        this.posts = posts;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUser() {
        return user;
    }

    public String getSlug() {
        return slug;
    }

    public int getThreads() {
        return threads;
    }

    public int getPosts() {
        return posts;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setPosts(int posts) {
        this.posts = posts;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }


    public JSONObject getJson() {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("title", title);
        jsonObject.put("user", user);
        jsonObject.put("slug", slug);
        jsonObject.put("posts", posts);
        jsonObject.put("threads", threads);
        return jsonObject;
    }
}
