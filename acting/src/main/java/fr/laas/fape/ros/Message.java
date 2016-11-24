package fr.laas.fape.ros;


public class Message {

    private String remote;
    private String body;

    public Message(String remote, String body) {
        this.remote = remote;
        this.body = body;
    }

    public String getRemote() {
        return remote;
    }

    public String getBody() {
        return body;
    }

    public String getEncoded() {
        int totalLength = 5*2 + remote.length() + body.length();
        return String.format("%5d%5d%s%5d%s", totalLength, remote.length(), remote, body.length(), body);
    }

    @Override
    public String toString() {
        return "Msg: "+ remote +" -- "+body;
    }
}
