package dk.dbc.z3950IllProxy;

public class Z3950HoldingsRequest {
    private String url;
    private String server;
    private Integer port;
    private String base;
    private String id;
    private String user;
    private String group;
    private String password;
    private String format;
    private String esn;
    private String schema;
    private String responder;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getEsn() {
        return esn;
    }

    public void setEsn(String esn) {
        this.esn = esn;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getResponder() {
        return responder;
    }

    public void setResponder(String responder) {
        this.responder = responder;
    }

    @Override
    public String toString() {
        return "Z3950HoldingsRequest{" +
                "url='" + url + '\'' +
                ", server='" + server + '\'' +
                ", port='" + port + '\'' +
                ", base='" + base + '\'' +
                ", id='" + id + '\'' +
                ", user='" + user + '\'' +
                ", group='" + group + '\'' +
                ", password='" + password + '\'' +
                ", format='" + format + '\'' +
                ", esn='" + esn + '\'' +
                ", schema='" + schema + '\'' +
                ", responder='" + responder + '\'' +
                '}';
    }
}
