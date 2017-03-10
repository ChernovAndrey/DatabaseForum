package master.objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * Created by andrey on 05.03.17.
 */
public class ObjUser {
   // private int id;
    private String nickname;
    private String fullname;
    private String about;
    private String email;

    public ObjUser() {

    }

    @JsonCreator
    public ObjUser(
          //  @JsonProperty("id") int id,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("fullname") String fullname,
            @JsonProperty("about") String about,
            @JsonProperty("email") String email) {
        //this.id = id;
        this.nickname=nickname;
        this.fullname = fullname;
        this.about = about;
        this.email = email;
    }

    /*public int getId() {
        return id;
    }*/

    public String getNickname() {
        return nickname;
    }

    public String getFullname() {
        return fullname;
    }

    public String getAbout() {
        return about;
    }

    public String getEmail() {
        return email;
    }

    /*public void setId(int id) {
        this.id = id;
    }*/

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public JSONObject getJson() {
        final JSONObject jsonObject = new JSONObject();
       // jsonObject.put("id", id);
        jsonObject.put("nickname",nickname);
        jsonObject.put("fullname", fullname);
        jsonObject.put("about", about);
        jsonObject.put("email", email);
        return jsonObject;
    }

}