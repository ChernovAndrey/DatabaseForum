package master.objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * Created by andrey on 14.03.17.
 */
public class ObjPost {
    private int id;
    private int parent;
    private String author;
    private String message;
    private boolean isEdited;
    private String forum;
    private int thread;
    private String created;

    public ObjPost() {

    }

    @JsonCreator
    public ObjPost(
            @JsonProperty("id") int id,
            @JsonProperty("parent") int parent,
            @JsonProperty("author") String author,
            @JsonProperty("message") String message,
            @JsonProperty("thread") int thread,
            @JsonProperty("isEdited") boolean isEdited,
            @JsonProperty("forum") String forum,
            @JsonProperty("created") String created) {
        this.id = id;
        this.parent = parent;
        this.author = author;
        this.message = message;
        this.thread = thread;
        this.isEdited = isEdited;
        this.forum = forum;
        this.created = created;

    }

    public int getId() {
        return id;
    }

    public int getParent() {
        return parent;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public int getThread() {
        return thread;
    }

    public String getForum() {
        return forum;
    }

    public boolean getEdited() {
        return isEdited;
    }

    public String getCreated() {
        return created;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setEdited(boolean isEdited) {
        this.isEdited = isEdited;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public JSONObject getJson() {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("author", author);
        jsonObject.put("message", message);
        jsonObject.put("parent", parent);
        jsonObject.put("isEdited", isEdited);
        jsonObject.put("thread", thread);
        jsonObject.put("forum", forum);
        jsonObject.put("created", created);
        return jsonObject;
    }
}

