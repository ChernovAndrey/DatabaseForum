package master.objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * Created by andrey on 15.03.17.
 */
public class ObjVote {
    private int id;
    private String slug;
    private int voice;
    private String nickname;

    public ObjVote() {
    }

    @JsonCreator
    public ObjVote(
            @JsonProperty("nickname") String nickname,
            @JsonProperty("slug") String slug,
            @JsonProperty("id") int id,
            @JsonProperty("voice") int voice) {
        this.nickname = nickname;
        this.id = id;
        this.slug = slug;
        this.voice = voice;
    }

    public String getSlug() {
        return slug;
    }

    public String getNickname() {
        return nickname;
    }

    public int getId() {
        return id;
    }

    public int getVoice() {
        return voice;
    }


    public void setId(int id) {
        this.id = id;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setVoice(int voice) {
        this.voice = voice;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }


    public JSONObject getJson() {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("nickname", nickname);
        jsonObject.put("slug", slug);
        jsonObject.put("voice", voice);
        return jsonObject;
    }
}
