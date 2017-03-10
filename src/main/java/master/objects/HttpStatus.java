package master.objects;

/**
 * Created by andrey on 06.03.17.
 */

public class HttpStatus {

    public String getOk(){
        return "200 OK";
    }

    public String getCreated(){
        return "201 Created";
    }

    public String getBadRequest(){
        return "400 Bad Request";
    }

    public String getUnauthorized(){
        return "401 Unauthorized";
    }

    public String getForbidden(){
        return "403 Forbidden";
    }

    public String getNotFound(){
        return "404 Not Found";
    }
}