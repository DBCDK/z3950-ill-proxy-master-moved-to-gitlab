package dk.dbc.z3950IllProxy;

public class SendIllRequest {
    private String server;
    private String port;
    private String user;
    private String group;
    private String password;
    private String data;
    private String timeout;
    private String encoding;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public String toString() {
        return "SendIllRequest{" +
                "server='" + server + '\'' +
                ", port='" + port + '\'' +
                ", user='" + user + '\'' +
                ", group='" + group + '\'' +
                ", password='" + password + '\'' +
                ", data='" + data + '\'' +
                ", timeout='" + timeout + '\'' +
                ", encoding='" + encoding + '\'' +
                '}';
    }
}
